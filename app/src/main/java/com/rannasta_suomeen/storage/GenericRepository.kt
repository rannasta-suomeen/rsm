package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.NetworkController.CabinetOperation
import com.rannasta_suomeen.NetworkController.CabinetOperation.*
import com.rannasta_suomeen.NetworkController.tryNTimes
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.ingredientRepository
import com.rannasta_suomeen.productRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

private const val DRINKFILENAME = "drinks"
private const val PRODUCTFILENAME = "products"
private const val INGREDIENTFILENAME = "ingredients"
private const val INGREDIENTFORDRINKFILENAME = "drink_ingredients"
private const val INGREDIENTPRODUCTFILTERFILENAME = "ingredient_product_filter"
private const val CABINETSFILENAME = "cabinets"
private const val NETACTIONFILENAME = "netactions"

abstract class GenericRepository<R,T>(context: Context, fn: String) {
    private var memoryCopy: Optional<List<R>> = Optional.empty()
    private var syncedFromInternet = false
    abstract val input: T
    private val file: File =  File(context.filesDir, fn)
    abstract val getFn: suspend (T) -> Result<List<R>>
    // BECAUSE FUCK YOU KOTLIN
    abstract val type: Class<Array<R>>
    val jackson = jacksonObjectMapper()

    init {
        jackson.findAndRegisterModules()
    }

    val dataFlow: Flow<List<R>> = flow{
        when (memoryCopy.isPresent){
            true -> emit(memoryCopy.get())
            false -> {
                val t = loadFromFile()
                t?.let { emit(it) }
                memoryCopy = Optional.ofNullable(t)
                // TODO There is a (small) performance improvement in starting the network request BEFORE making the file fetch
                if (!syncedFromInternet){
                    val res = tryNTimes(5, input, getFn)
                    if (res.isSuccess){
                        val x = res.getOrThrow()
                        emit(x)
                        writeToFile(x)
                        memoryCopy = Optional.of(x)
                        syncedFromInternet = true
                    }
                }
            }
        }
        return@flow
    }

    private fun loadFromFile(): List<R>?{
        return try{
            val t: Array<R> = jackson.readValue(file.readText(),type)
            Log.d("Storage", "Loaded ${t.size}")
            t.toList()
        } catch (e: FileNotFoundException){
            null
        }
        catch (e: MissingKotlinParameterException){
            Log.d("Storage","Failed json parse")
            null
        }
    }

    private fun writeToFile(list: List<R>){
        file.writeText(jackson.writeValueAsString(list))
    }

    fun deleteFile(){
        file.delete()
        memoryCopy = Optional.empty()
        syncedFromInternet = false
    }
}

class DrinkRepository(context: Context):
    GenericRepository<DrinkInfo, Unit>(context, DRINKFILENAME){
    override val getFn = NetworkController::getDrinks
    override val type = Array<DrinkInfo>::class.java
    override val input: Unit = Unit
}

class ProductRepository(context: Context):
    GenericRepository<Product, Unit>(context, PRODUCTFILENAME){
    override val input: Unit = Unit
    override val getFn = NetworkController::getProducts
    override val type = Array<Product>::class.java
}

class IngredientRepository(context: Context): GenericRepository<GeneralIngredient, Unit>(context, INGREDIENTFILENAME){
    override val input: Unit = Unit
    override val getFn = NetworkController::getIngredients
    override val type = Array<GeneralIngredient>::class.java
}

class IngredientForDrinkRepository(context: Context): GenericRepository<IngredientsForDrink, Unit>(context, INGREDIENTFORDRINKFILENAME){
    override val getFn = NetworkController::getDrinkRecipes
    override val input = Unit
    override val type = Array<IngredientsForDrink>::class.java
}

class ProductToIngredientRepository(context: Context): GenericRepository<IngredientProductFilter, Unit>(context, INGREDIENTPRODUCTFILTERFILENAME){
    override val getFn = NetworkController::getProductIngredientFilter
    override val input = Unit
    override val type = Array<IngredientProductFilter>::class.java
}

internal class CabinetRepository(context: Context){
    private var state: MutableList<CabinetCompact> = mutableListOf()
    val stateFlow: MutableSharedFlow<List<CabinetCompact>> = MutableSharedFlow(1)
    private var serverState: List<CabinetCompact> = listOf()
    private val serverFlow = MutableSharedFlow<List<CabinetCompact>>()

    private var netActionQueue: MutableList<CabinetOperation> = mutableListOf()
    private val file = File(context.filesDir, CABINETSFILENAME)
    private val netQueueFile = File(context.filesDir, NETACTIONFILENAME)

    private val stateMutex = Mutex()

    private val jackson = jacksonObjectMapper()

    init {
        jackson.findAndRegisterModules()
        CoroutineScope(Dispatchers.IO).launch {
            launch { serverFlow.collect{
                // TODO: Add timestamps and use them
                serverState = it
                stateMutex.withLock {
                    state = serverState.toMutableList()
                    stateFlow.emit(state)
                }
            }}
            stateMutex.withLock {
                if (state.size == 0){
                    try {
                        state = jackson.readerForArrayOf(CabinetCompact::class.java).readValue(file.readText(), Array<CabinetCompact>::class.java).toMutableList()
                        stateFlow.emit(state)
                    } catch (_: FileNotFoundException){
                    } catch (e: JsonMappingException){
                        Log.d("Json", "Failed to parse state cause:$e")
                    }

                }
            }
            try {
                netActionQueue =
                    jackson.readerForArrayOf(CabinetOperation::class.java).readValue(netQueueFile.readText(), Array<CabinetOperation>::class.java)
                        .toMutableList()
            } catch (e: Exception) {
                fun failParse() {
                    Log.d("Storage", "Failed to parse $netQueueFile")
                    netQueueFile.delete()
                }
                when (e) {
                    is FileNotFoundException -> Unit
                    is IOException -> failParse()
                    else -> throw e
                }
            }

            forceUpdate()
            while (true){
                when(netActionQueue.isNotEmpty()){
                    true -> processNetOperation(netActionQueue.first())
                    false -> {
                        forceUpdate()
                        delay(1000)
                    }
                }
            }
        }
    }

    suspend fun delete(){
        file.delete()
        netQueueFile.delete()
        netActionQueue.clear()
        state = mutableListOf()
        stateFlow.emit(state)
    }

    private suspend fun forceUpdate(){
        tryNTimes(5, Unit, NetworkController::getCabinets).onSuccess {
            file.writeText(jackson.writeValueAsString(it))
            serverFlow.emit(it)
        }
    }

    private suspend fun processNetOperation(oper: CabinetOperation){
        // TODO: Add timestamps and use them
       val res = when (oper){
           is AddItemToCabinet -> tryNTimes(5,oper,NetworkController::insertCabinetProduct)
           is DeleteCabinet  -> tryNTimes(5,oper,NetworkController::deleteCabinet)
           is RemoveItemFromCabinet -> tryNTimes(5,oper,NetworkController::deleteCabinetProduct)
           is MakeItemUnusable -> tryNTimes(5,oper,NetworkController::unusableCabinetProduct)
           is MakeItemUsable -> tryNTimes(5,oper,NetworkController::usableCabinetProduct)
           is NewCabinet -> tryNTimes(5,oper,NetworkController::createCabinet)
           is ModifyCabinetProductAmount -> tryNTimes(5,oper,NetworkController::modifyCabinetProduct)
           is JoinCabinet -> tryNTimes(5,oper,NetworkController::joinCabinet)
           is ExitCabinet -> tryNTimes(5,oper,NetworkController::quitCabinet)
           is BulkMoveItems -> tryNTimes(5,oper,NetworkController::moveItemsIntoCabinet)
           is AddMixer -> tryNTimes(5, oper, NetworkController::insertCabinetMixer)
           is BulkMoveMixers -> tryNTimes(5, oper, NetworkController::moveMixers)
           is ModifyMixer -> tryNTimes(5, oper, NetworkController::modifyCabinetMixer)
           is RemoveMixer -> tryNTimes(5, oper, NetworkController::deleteCabinetMixer)
           is SetMixerUnusable -> tryNTimes(5, oper, NetworkController::unusableCabinetMixer)
           is SetMixerUsable -> tryNTimes(5, oper, NetworkController::usableCabinetMixer)
       }
        if (res.isSuccess){
            netActionQueue.removeIf { it == oper }
            netQueueFile.writeText(jackson.writerFor(Array<CabinetOperation>::class.java).writeValueAsString(netActionQueue.toTypedArray()))
            runNetQueueAction(oper)
        } else {
            val exp = res.exceptionOrNull()
            if (exp is NetworkController.Error.MiscError){
                netActionQueue.removeIf { it == oper }
                netQueueFile.writeText(jackson.writerFor(Array<CabinetOperation>::class.java).writeValueAsString(netActionQueue.toTypedArray()))
            }
            delay(100)
        }
    }

    private fun addActionToQueue(oper: CabinetOperation){
        netActionQueue.add(oper)
        CoroutineScope(Dispatchers.IO).launch {
            netQueueFile.writeText(jackson.writerFor(Array<CabinetOperation>::class.java).writeValueAsString(netActionQueue.toTypedArray()))
        }
    }

    private fun updateState(fn: (MutableList<CabinetCompact>) -> Unit){
        fn(state)
        CoroutineScope(Dispatchers.IO).launch {
            stateMutex.withLock {
                stateFlow.emit(state)
                file.writeText(jackson.writeValueAsString(state))
            }
        }
    }

    fun newCabinet(c: NewCabinet) {
        // TODO: Make this work without internet
        CoroutineScope(Dispatchers.IO).launch {
            tryNTimes(5, c, NetworkController::createCabinet).onSuccess {
                tryNTimes(5, Unit, NetworkController::getCabinets).onSuccess { cabState ->
                    stateMutex.withLock {
                        serverFlow.emit(cabState)
                    }
                }
            }
        }
    }

    fun deleteCabinet(c: DeleteCabinet){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun addItemToCabinet(c: AddItemToCabinet){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun removeItemFromCabinet(c: RemoveItemFromCabinet){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun modifyCabinetProductAmount(c: ModifyCabinetProductAmount) {
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun setProductUsable(c: MakeItemUsable){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun setProductUnUsable(c: MakeItemUnusable){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun joinCabinet(c: JoinCabinet){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun exitCabinet(c: ExitCabinet){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun bulkMoveItems(c: BulkMoveItems){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    fun runCabinetOperation(c: CabinetOperation){
        addActionToQueue(c)
        runNetQueueAction(c)
    }

    private fun runNetQueueAction(cabinetOperation: CabinetOperation){
        when (cabinetOperation){
            is NewCabinet -> CoroutineScope(Dispatchers.IO).launch {
                tryNTimes(5, Unit, NetworkController::getCabinets).onSuccess { cabState ->
                    stateMutex.withLock {
                        serverFlow.emit(cabState)
                    }
                }
            }
            is JoinCabinet -> CoroutineScope(Dispatchers.IO).launch {
                tryNTimes(5, Unit, NetworkController::getCabinets).onSuccess { cabState ->
                    stateMutex.withLock {
                        serverFlow.emit(cabState)
                    }
                }
            }
            is AddItemToCabinet -> updateState {
                val t = it.find { it.id == cabinetOperation.id }
                    t?.products?.add(CabinetProductCompact(Random().nextInt(), cabinetOperation.pid, t.getOwnUserId(),cabinetOperation.amount, true))
            }
            is DeleteCabinet -> updateState { it.removeIf { it.id == cabinetOperation.id } }
            is MakeItemUnusable -> updateState { it.find { it.id == cabinetOperation.id }?.let { it.products.find { it.id == cabinetOperation.pid }?.let { it.usable = false} } }
            is MakeItemUsable -> updateState { it.find { it.id == cabinetOperation.id }?.let { it.products.find { it.id == cabinetOperation.pid }?.let { it.usable = true} } }
            is ModifyCabinetProductAmount -> updateState { it.find { it.id == cabinetOperation.id }?.let { it.products.find { it.id == cabinetOperation.pid }?.let { it.amountMl = cabinetOperation.amount} } }
            is RemoveItemFromCabinet -> updateState { it.find { it.id == cabinetOperation.id }?.let { it.products.removeIf { it.id == cabinetOperation.pid } } }
            is ExitCabinet -> updateState { it.removeIf { it.id == cabinetOperation.id } }
            is BulkMoveItems -> updateState {
                val originCabint = it.find { it.id == cabinetOperation.originId }
                val targetCabinet = it.find { it.id == cabinetOperation.targetId }
                originCabint?.let { origin ->
                    targetCabinet?.let {target ->
                        val targetList = originCabint.products.filter { cabinetOperation.items.contains(it.id) }
                        targetList.forEach {
                            origin.products.removeIf { targetList.contains(it) }
                            target.products.addAll(targetList)
                        }
                    }
                }
            }
            is AddMixer -> updateState {
                it.find { it.id == cabinetOperation.cabinetId }?.let{it.mixers.add(CabinetMixerCompact(Random().nextInt(), cabinetOperation.ingredientId,it.getOwnUserId(), cabinetOperation.amount,true))}}
            is ModifyMixer -> updateState { it.find {it.id == cabinetOperation.cabinetId}?.let {it.mixers.find { it.id == cabinetOperation.mixerId }?.let { it.amount = cabinetOperation.amount }} }
            is RemoveMixer -> updateState { it.find { it.id == cabinetOperation.cabinetId }?.mixers?.removeIf { it.id == cabinetOperation.mixerId } }
            is SetMixerUnusable -> updateState { it.find { it.id == cabinetOperation.cabinetId }?.mixers?.find { it.id == cabinetOperation.mixerId }?.usable = false }
            is SetMixerUsable -> updateState { it.find { it.id == cabinetOperation.cabinetId }?.mixers?.find { it.id == cabinetOperation.mixerId }?.usable = true }
            is BulkMoveMixers -> updateState {
                val originCabint = it.find { it.id == cabinetOperation.originId }
                val targetCabinet = it.find { it.id == cabinetOperation.targetId }
                originCabint?.let { origin ->
                    targetCabinet?.let {target ->
                        val targetList = originCabint.mixers.filter { cabinetOperation.items.contains(it.id) }
                        targetList.forEach {
                            origin.mixers.removeIf { targetList.contains(it) }
                            target.mixers.addAll(targetList)
                        }
                    }
                }
            }
        }
    }
}

class TotalCabinetRepository(context: Context, private val settings: Settings){
    private val cabinetRepository = CabinetRepository(context)
    private val productToIngredientRepository = ProductToIngredientRepository(context)

    private var generalIngredientList: HashMap<Int,GeneralIngredient> = HashMap()
    private var productMap:HashMap<Int, Product> = HashMap()
    private var productIngredientList = listOf<IngredientProductFilter>()
    private var productIngredientListPointer = listOf<IngredientProductFilterPointer>()
    var cabinetList: List<CabinetCompact> = listOf()
    private var lock = Mutex()

    var selectedCabinetFlow: MutableSharedFlow<Cabinet?> = MutableSharedFlow(1)
    var selectedCabinet: Cabinet? = null

    val cabinetFlow: MutableSharedFlow<List<Cabinet>> = MutableSharedFlow(1)

    val ownedIngredientFlow: MutableSharedFlow<List<GeneralIngredient>> = MutableSharedFlow(1)

    suspend fun clear(){
        CoroutineScope(Dispatchers.IO).launch {
            lock.withLock {
                cabinetList = listOf()
                cabinetRepository.delete()
                selectedCabinetFlow.emit(null)
                selectedCabinet = null
                cabinetFlow.emit(listOf())
                ownedIngredientFlow.emit(listOf())
            }
        }
    }

    private suspend fun emitCurrent() {
        productIngredientListPointer = productIngredientList.mapNotNull { it.toPointer(generalIngredientList, productMap) }

        cabinetFlow.emit(cabinetList.mapNotNull { cab ->
            val t = cab.toCabinet(productMap, generalIngredientList)
            if ((selectedCabinet == null && settings.cabinet == t?.id) || selectedCabinet?.id == t?.id){
                selectedCabinet = t
                selectedCabinetFlow.emit(t)
            }
            t
        })
        selectedCabinet?.let { ownedIngredientFlow.emit(productsToIngredients(it.products) + it.mixers.map { it.ingredient } ) }
    }

    fun productsToIngredients(products: List<CabinetProduct>):List<GeneralIngredient>{
        val ownedByFilter = generalIngredientList.toList().map { it.second }.filter {
            it.use_static_filter_c && products.any { p -> p.product.categoryId == it.static_filter_c && p.usable }
                    || it.use_static_filter && products.any { p -> p.product.subcategoryId == it.static_filter && p.usable }
        }.toMutableList()
        val ownedById = productIngredientListPointer.filter {
            it.products.find { p->
                products.find{ it.product.id == p.id && it.usable} != null
            } != null
        }.map { it.ingredient }
        ownedByFilter.addAll(ownedById)
        return ownedByFilter.toList()
    }

    fun createCabinet(name: String) {
        cabinetRepository.newCabinet(NewCabinet(name))
    }

    fun changeSelectedCabinet(cabinet: Cabinet?){
        selectedCabinet = cabinet
        if (cabinet != null) {
            settings.cabinet = cabinet.id
        }
        CoroutineScope(Dispatchers.IO).launch {
            emitCurrent()
            selectedCabinetFlow.emit(selectedCabinet)
        }
    }

    /**
     * Can only modify products you own
     */
    fun addOrModifyToSelected(pid: Int, amount: Int?) {
        selectedCabinet?.let {
            val userid = it.getOwnUserId()
            val owned = it.products.find { p-> p.product.id == pid && userid == p.ownerId }
            if (owned != null){
                modifyCabinetProductAmount(it.id, owned.id, amount)
            } else {
                addItemToCabinet(it.id, pid, amount)
            }
        }
    }

    fun addOrModifyMixerToSelected(ingredientId: Int, amount: Int?){
        selectedCabinet?.let {
            val userId = it.getOwnUserId()
            val owned = it.mixers.find { p-> p.ingredient.id == ingredientId && p.ownerId == userId }
            if (owned != null){
                modifyMixer(it.id, owned.id, amount)
            } else {
                addMixer(it.id, ingredientId, amount)
            }
        }
    }

    fun deleteCabinet(id: Int) {
        cabinetRepository.deleteCabinet(DeleteCabinet(id))
    }

    /**
     * Adds Selected product to selected cabinet. ATTENTION: Does not check that the cabinet does not have said product
     */
    private fun addItemToCabinet(id: Int, pid: Int, amount: Int?) {
        cabinetRepository.addItemToCabinet(AddItemToCabinet(id,pid, amount))
    }

    fun removeItemFromCabinet(id: Int, pid: Int) {
        cabinetRepository.removeItemFromCabinet(RemoveItemFromCabinet(id,pid))
    }

    fun setProductUsable(id: Int, pid: Int){
        cabinetRepository.setProductUsable(MakeItemUsable(id,pid))
    }

    fun setProductUnusable(id: Int, pid: Int){
        cabinetRepository.setProductUnUsable(MakeItemUnusable(id, pid))
    }

    fun joinCabinet(code: String){
        cabinetRepository.joinCabinet(JoinCabinet(code))
    }

    fun exitCabinet(id: Int){
        cabinetRepository.exitCabinet(ExitCabinet(id))
    }

    fun addMixer(cabinetId: Int, ingredientId: Int, amount: Int?){
        cabinetRepository.runCabinetOperation(AddMixer(cabinetId, ingredientId, amount))
    }

    fun removeMixer(cabinetId: Int, mixerId: Int){
        cabinetRepository.runCabinetOperation(RemoveMixer(cabinetId, mixerId))
    }

    fun modifyMixer(cabinetId: Int, mixerId: Int, amount: Int?){
        cabinetRepository.runCabinetOperation(ModifyMixer(cabinetId, mixerId, amount))
    }

    fun setMixerUsable(cabinetId: Int, mixerId: Int){
        cabinetRepository.runCabinetOperation(SetMixerUsable(cabinetId, mixerId))
    }

    fun setMixerUnusable(cabinetId: Int, mixerId: Int){
        cabinetRepository.runCabinetOperation(SetMixerUnusable(cabinetId, mixerId))
    }

    fun bulkMoveMixers(originId: Int, targetId: Int, mixerIds: List<Int>){
        cabinetRepository.runCabinetOperation(BulkMoveMixers(originId, targetId, mixerIds))
    }

    @Suppress("Unused")
    fun bulkMoveItems(originId: Int, targetId: Int, pids: List<Int>){
        cabinetRepository.bulkMoveItems(BulkMoveItems(originId,targetId,pids))
    }

    private fun modifyCabinetProductAmount(id: Int, pid: Int, amount: Int?){
        cabinetRepository.modifyCabinetProductAmount(ModifyCabinetProductAmount(id, pid, amount))
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                productRepository.dataFlow.collect {
                    lock.withLock {
                        it.forEach { p ->
                            productMap[p.id] = p
                        }
                        emitCurrent()
                    }
                }
            }
            launch {
                cabinetRepository.stateFlow.collect {
                    lock.withLock {
                        cabinetList = it
                        emitCurrent()
                    }
                }
            }
            launch {
                productToIngredientRepository.dataFlow.collect{
                    lock.withLock {
                        productIngredientList = it
                        emitCurrent()
                    }
                }
            }
            launch {
                ingredientRepository.dataFlow.collect{
                    lock.withLock {
                        generalIngredientList.clear()
                        it.forEach { generalIngredientList[it.id] = it }
                        emitCurrent()
                    }
                }
            }
        }
    }
}

class TotalDrinkRepository(context: Context) {
    private val recipeRepo = IngredientForDrinkRepository(context)
    private val drinkRepository = DrinkRepository(context)
    private var ingredientList: List<GeneralIngredient> = listOf()
    private var recipeList: List<IngredientsForDrink> = listOf()
    private var drinkList: List<DrinkInfo> = listOf()
    var totalDrinkList: List<DrinkTotal> = listOf()

    val dataFlow: MutableSharedFlow<List<DrinkTotal>> = MutableSharedFlow(1)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            suspend fun emitCurrent() {
                val list = recipeList.mapNotNull { ings ->
                    drinkList.find { it.recipeId == ings.recipeId }?.let {
                        ings.toPointer(ingredientList)
                            ?.let { it1 -> DrinkTotal(it, it1) }
                    }
                }
                totalDrinkList = list
                dataFlow.emit(list)
            }
            launch {
                drinkRepository.dataFlow.collect {
                    drinkList = it
                    emitCurrent()
                }
            }
            launch {
                ingredientRepository.dataFlow.collect {
                    ingredientList = it
                    emitCurrent()
                }
            }
            launch {
                recipeRepo.dataFlow.collect {
                    recipeList = it
                    emitCurrent()
                }
            }
        }
    }

    /**
     * Returns list of drinks that can be made with current alcoholic ingredients
     */
    fun makeableWithAlcohol(owned: TreeMap<Int,GeneralIngredient>): List<DrinkTotal>{
        return totalDrinkList.filter { it.canMakeAlcoholic(owned) }
    }

    fun makeableWithStrict(owned: TreeMap<Int,GeneralIngredient>): List<DrinkTotal>{
        return totalDrinkList.filter { it.canMakeStrict(owned) }
    }

    fun makeableWithMixers(owned: TreeMap<Int, GeneralIngredient>): List<DrinkTotal>{
        return totalDrinkList.filter { it.canMakeNonAlcoholic(owned) }
    }

    fun makeableWithNewAlcoholic(owned: TreeMap<Int,GeneralIngredient>, new: List<GeneralIngredient>): List<DrinkTotal>{
        val previousMakeable = makeableWithAlcohol(owned)
        val newList = owned.toMutableMap()
        newList += new.associateBy { it.id }
        val makeableWithCart = totalDrinkList.filter { it.canMakeAlcoholic(newList.toSortedMap() as TreeMap<Int, GeneralIngredient>)}
        return makeableWithCart.filter { !previousMakeable.contains(it) }
    }

    fun makeableWithNewStrict(owned: TreeMap<Int,GeneralIngredient>, new: List<GeneralIngredient>): List<DrinkTotal>{
        val previousMakeable = makeableWithStrict(owned)
        val newList = owned.toMutableMap()
        newList += new.associateBy { it.id }
        val makeableWithCart = totalDrinkList.filter { it.canMakeStrict(newList.toSortedMap() as TreeMap<Int, GeneralIngredient>)}
        return makeableWithCart.filter { !previousMakeable.contains(it) }
    }
}