package com.rannasta_suomeen

import android.util.Log
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rannasta_suomeen.NetworkController.CabinetOperation.*
import com.rannasta_suomeen.data_classes.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.URL
import java.time.Instant

object NetworkController {
    private var jwtToken: String? = null
    private val jackson = jacksonObjectMapper()
    var username: String? = null
    private var password: String? = null

    private const val serverAddress: String = "https://api.rannasta-suomeen.fi"
    //private const val serverAddress: String = "http://10.0.2.2:8000"
    private val client = OkHttpClient()

    init {
        jackson.findAndRegisterModules()
    }

    fun logout(){
        jwtToken = null
        username = null
        password = null
    }

    sealed class Error(override val message: String) : Throwable() {
        class NetworkError : Error("No network")
        class TokenError : Error("Token not valid")
        class CredentialsError : Error("Username or password is wrong")
        class NoCredentialsError : Error("No username, password or token given")
        class RetryError(e: Throwable) : Error("Retried too many times, cause $e")
        class MiscError(code: Int, body: String) : Error("Error $code with body $body")
        class JsonError(s: String) : Error("Json $s could not be parsed")
        class ConflictError(userName: String): Error("Username: $userName is allrady taken")
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    sealed class CabinetOperation{
        val timestamp: Instant = Instant.now()
        class NewCabinet(val name: String): CabinetOperation()
        class DeleteCabinet(val id: Int): CabinetOperation()
        class AddItemToCabinet(val id: Int, val pid: Int, val amount: Int?): CabinetOperation()
        class ModifyCabinetProductAmount(val id: Int, val pid: Int, val amount: Int?): CabinetOperation()
        class RemoveItemFromCabinet(val id:Int, val pid: Int): CabinetOperation()
        class MakeItemUsable(val id:Int, val pid: Int): CabinetOperation()
        class MakeItemUnusable(val id:Int, val pid: Int): CabinetOperation()
        class JoinCabinet(val code: String): CabinetOperation()
        class ExitCabinet(val id:Int):CabinetOperation()
        class BulkMoveItems(val originId:Int, val targetId: Int, val items: List<Int>):CabinetOperation()
        class AddMixer(val cabinetId: Int, val ingredientId: Int, val  amount: Int?): CabinetOperation()
        class RemoveMixer(val cabinetId: Int, val mixerId: Int): CabinetOperation()
        class SetMixerUsable(val cabinetId: Int, val mixerId: Int): CabinetOperation()
        class SetMixerUnusable(val cabinetId: Int, val mixerId: Int): CabinetOperation()
        class ModifyMixer(val cabinetId: Int, val mixerId: Int, val amount: Int?): CabinetOperation()
        class BulkMoveMixers(val originId: Int,val targetId: Int, val items: List<Int>): CabinetOperation()
    }

    /** Logs the user in. First item of the pair is the username, second is the password.
     * @return Result.success(Unit) if successful, Result.failure(e)
     */
    suspend fun login(payload: Pair<String, String>): Result<Unit> {
        val user = payload.first
        val pwd = payload.second
        username = user
        password = pwd
        val body: RequestBody = (user + "\n" + pwd).toRequestBody()
        val request = Request.Builder().url("$serverAddress/login").post(body).build()
        try {
            client.newCall(request).execute().use {
                when (it.code) {
                    200 -> {
                        // Server should always return a body with a successful login request
                        val token = it.body!!.string()
                        jwtToken = token.trim()
                        return Result.success(Unit)
                    }
                    401 -> {
                        Log.d("Networking", "Please give username and password")
                        return Result.failure(Error.NoCredentialsError())
                    }
                    403 -> {
                        Log.d("Networking", "Username or password wrong")
                        return Result.failure(Error.CredentialsError())
                    }
                    else -> {
                        Log.d("Networking", "${it.code}")
                        return Result.failure(
                            Error.MiscError(
                                it.code,
                                it.body?.string() ?: "No body"
                            )
                        )
                    }
                }
            }
        } catch (e: IOException) {
            Log.d("Networking", "$e")
            return Result.failure(Error.NetworkError())
        }
    }

    suspend fun register(payload: Pair<String, String>): Result<Unit>{
        val usr = payload.first
        val pwd = payload.second
        val body : RequestBody = (usr + "\n" + pwd).toRequestBody()
        val request = Request.Builder().url("$serverAddress/register").post(body).build()
        try {
            client.newCall(request).execute().use {
                return when (it.code){
                    201 -> {
                        username = usr
                        password = pwd
                        Result.success(Unit)
                    }
                    400 -> {
                        Log.e("Networking", "Username and password given in the wrong format")
                        Result.failure(Error.MiscError(400,"Wrong format for username and password"))
                    }
                    409 -> {
                        Log.d("Networking", "Conflict")
                        Result.failure(Error.ConflictError(usr))
                    }
                    else -> {
                        Log.e("Networking", "${it.code}")
                        Result.failure(Error.MiscError(it.code, it.body?.string()?:""))
                    }
                }
            }
        } catch (e: IOException){
            Log.d("Networking", "$e")
            return Result.failure(Error.NetworkError())
        }
    }

    /** Makes a request and returns a list of all drinks
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [DrinkInfo] or an [Error]
     */
    suspend fun getDrinks(_payload: Unit): Result<List<DrinkInfo>> {
        val request = Request.Builder().url("$serverAddress/drinks").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(DrinkInfo::class.java).readValue(s,Array<DrinkInfo>::class.java)
            Log.d("Drinks", "Drinks: ${list.toList().size}")
            list.toList()
        }
    }

    /** Makes a request and tries to create a new cabinet
     * @param payload String the name of the cabinet to create
     * @return Result, either [Int] the id of the new cabinet or an [Error]
     */
    suspend fun createCabinet(payload: NewCabinet): Result<Int> {
        val request = Request.Builder().url("$serverAddress/cabinet?name=${payload.name}").post("".toRequestBody())
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = jackson.readValue(s,Int::class.java)
            list
        }
    }

    /** Makes a request and tries to delete a cabinet
     * @param Cabinet Int, the id of the cabinet to delete
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun deleteCabinet(Cabinet: DeleteCabinet): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/cabinet/${Cabinet.id}").delete()
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
               Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to get all cabinets. Including shared ones
     * @return Result, either List of [CabinetCompact] or an [Error]
     */
    suspend fun getCabinets(_payload: Unit): Result<List<CabinetCompact>> {
        val request = Request.Builder().url("$serverAddress/cabinet").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(CabinetCompact::class.java).readValue(s,Array<CabinetCompact>::class.java)
            list.toList()
        }
    }

    /** Makes a request and tries to put a product into a cabinet
     * @param payload [Pair] of a [Pair] and [Int]. First pair tells cabinet and product, second tell amount
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun insertCabinetProduct(payload: AddItemToCabinet): Result<Unit> {
        val amountStr = when(payload.amount == null){
            true -> ""
            false -> "\\${payload.amount}"
        }
        val request = Request.Builder().url("$serverAddress/items/${payload.id}/${payload.pid}$amountStr").put("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to delete a product from a cabinet
     * @param payload [Pair] of a [Int] and [Int]. Cabinet and product.
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun deleteCabinetProduct(payload: RemoveItemFromCabinet): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/items/${payload.id}/${payload.pid}").delete()
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to modify the amount of a product in a cabinet
     * @param payload [Pair] of a [Pair] and [Int]. First pair tells cabinet and product, second tell amount
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun modifyCabinetProduct(payload: ModifyCabinetProductAmount): Result<Unit> {
        val amountStr = when(payload.amount == null){
            true -> ""
            false -> "\\${payload.amount}"
        }
        val request = Request.Builder().url("$serverAddress/items/${payload.id}/${payload.pid}$amountStr").post("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to make a product usable
     * @param payload [Pair] of a [Int] and [Int]. Cabinet and product.
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun usableCabinetProduct(payload: MakeItemUsable): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/items/usable/${payload.id}/${payload.pid}").post("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to make a product unusable
     * @param payload [Pair] of a [Int] and [Int]. Cabinet and product.
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun unusableCabinetProduct(payload: MakeItemUnusable): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/items/usable/${payload.id}/${payload.pid}").delete()
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to put a mixer into a cabinet
     * @param payload [AddMixer]
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun insertCabinetMixer(payload: AddMixer): Result<Unit> {
        val amountStr = when(payload.amount == null){
            true -> ""
            false -> "\\${payload.amount}"
        }
        val request = Request.Builder().url("$serverAddress/cabinet/mixer/${payload.cabinetId}/${payload.ingredientId}$amountStr").put("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to delete a mixer from a cabinet
     * @param payload [RemoveMixer]
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun deleteCabinetMixer(payload: RemoveMixer): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/cabinet/mixer/${payload.cabinetId}/${payload.mixerId}").delete()
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to make a mixer usable
     * @param payload [SetMixerUsable] Cabinet and product.
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun usableCabinetMixer(payload: SetMixerUsable): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/cabinet/mixer/usable/${payload.cabinetId}/${payload.mixerId}").put("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to make a mixer unusable
     * @param payload [SetMixerUnusable] Cabinet and product.
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun unusableCabinetMixer(payload: SetMixerUnusable): Result<Unit> {
        val request = Request.Builder().url("$serverAddress/cabinet/mixer/usable/${payload.cabinetId}/${payload.mixerId}").delete()
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to modify the amount of a mixer in a cabinet
     * @param payload [ModifyMixer]
     * @return Result, either [Unit] or an [Error]
     */
    suspend fun modifyCabinetMixer(payload: ModifyMixer): Result<Unit> {
        val amountStr = when(payload.amount == null){
            true -> ""
            false -> "\\${payload.amount}"
        }
        val request = Request.Builder().url("$serverAddress/cabinet/mixer/${payload.cabinetId}/${payload.mixerId}$amountStr").post("".toRequestBody())
        return makeTokenRequest(request) {
            if (it.isSuccessful){
                Result.success(Unit)
            } else {
                Result.failure(Error.MiscError(it.code, it.body?.string()?: "No body"))
            }
        }
    }

    /** Makes a request and tries to put multiple mixers into a cabinet
     * @param payload [BulkMoveMixers]
     * @return Result, either [Unit] or an [Error]
     */
    // TODO: Implement this in a better way, for example now it fails with amounts
    suspend fun moveMixers(payload: BulkMoveMixers): Result<Unit> {
        val x = payload.items.map {
            CoroutineScope(Dispatchers.IO).async {
                val t = deleteCabinetMixer(RemoveMixer(payload.originId,it))
                if (t.isSuccess){
                    insertCabinetMixer(AddMixer(payload.targetId, it, null))
                } else {
                    t
                }
            }
        }
        return x.awaitAll().find { it.isFailure }?: Result.success(Unit)
    }

    /** Makes a request and returns a list of all IngredientProductFilters
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [IngredientProductFilter] or [Error]
     */
    suspend fun getProductIngredientFilter(_payload: Unit): Result<List<IngredientProductFilter>>{
        val request = Request.Builder().url("$serverAddress/ingprofilter").get()
        return makeTokenRequest(request){
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(IngredientProductFilter::class.java).readValue(s,Array<IngredientProductFilter>::class.java)
            list.toList()
        }
    }

    /** Makes a request and returns a list of all drinks
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [Product] or an [Error]
     */
    suspend fun getProducts(_payload: Unit): Result<List<Product>> {
        val request = Request.Builder()
            .url("$serverAddress/products")
            .get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(Product::class.java).readValue(s,Array<Product>::class.java)
            list.toList()
        }
    }

    /** Makes a request and returns a list of all drink recipes
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [IngredientsForDrink] or [Error]
     */
    suspend fun getDrinkRecipes(_payload: Unit):Result<List<IngredientsForDrink>>{
        val request = Request.Builder()
            .url("$serverAddress/recipes")
            .get()
        return makeTokenRequest(request){
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(IngredientsForDrink::class.java).readValue(s,Array<IngredientsForDrink>::class.java)
            list.toList()
        }
    }

    /** Makes a request and returns a list of all ingredients
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [GeneralIngredient] or [Error]
     */
    suspend fun getIngredients(_payload: Unit):Result<List<GeneralIngredient>>{
        val request = Request.Builder()
            .url("$serverAddress/ingredients")
            .get()
        return makeTokenRequest(request){
            val s = it.body?.string()
            val list = jackson.readerForArrayOf(GeneralIngredient::class.java).readValue(s,Array<GeneralIngredient>::class.java)
            list.toList()
        }
    }

    /** Shares a cabinet and returns it's access key
     * @param payload the id of the cabinet to share
     * @return The string that is the cabinets new access code
     */
    suspend fun shareCabinet(payload: Int):Result<String>{
        val request = Request.Builder()
            .url("$serverAddress/cabinet/shared/$payload")
            .put("".toRequestBody())
        return makeTokenRequest(request){
            val s = it.body?.string()
            // Body should never be not a string
            s?:""
        }
    }

    /** Tries to join cabinet with a certain access key
     * @param payload the string of the cabinet to join
     * @return [Result] [Unit]
     */
    suspend fun joinCabinet(payload: JoinCabinet):Result<Unit>{
        val request = Request.Builder()
            .url("$serverAddress/cabinet/shared/join/${payload.code}")
            .put("".toRequestBody())
        return makeTokenRequest(request){}
    }

    /** Tries to quit a cabinet with a certain id
     * @param payload the id of the cabinet to quit
     * @return [Result] [Unit]
     */
    suspend fun quitCabinet(payload: ExitCabinet):Result<Unit>{
        val request = Request.Builder()
            .url("$serverAddress/cabinet/shared/quit/${payload.id}")
            .delete("".toRequestBody())
        return makeTokenRequest(request){

        }
    }

    /** Tries to move a list of products
     * @param payload [BulkMoveItems] object to tell what items to move
     * @return [Result] [Unit]
     */
    suspend fun moveItemsIntoCabinet(payload: BulkMoveItems):Result<Unit>{
        val request = Request.Builder()
            .url("$serverAddress/cabinet/shared/move/${payload.originId}/${payload.targetId}")
            .post(jackson.writeValueAsString(payload.items).toRequestBody())
        return makeTokenRequest(request){}
    }

    /** Makes a request and returns the result or error it caused.
     * @param rb the Builder for the request to make
     * @param function that deals with a successful response that returns [R]
     * @return Result, either a [R] or a [Error]
     */
    private suspend fun <R> makeTokenRequest(rb: Request.Builder, function: (Response) -> R): Result<R> {
        try {
            while (jwtToken == null) {
                /* no-op */
                delay(100)
            }
            val request = rb.header("authorization", jwtToken!!).build()
            client.newCall(request).execute().use {
                return when (it.code) {
                    200 -> {
                        Result.success(function(it))
                    }
                    401 -> {
                        Log.d("Networking", "No authorization given")
                        Result.failure(Error.NoCredentialsError())
                    }
                    403 -> {
                        Log.d("Networking", "Token $jwtToken is wrong")
                        Result.failure(Error.TokenError())
                    }
                    else -> {
                        Log.d("Networking", "Returned ${it.code} from ${request.url}")
                        Result.failure(Error.MiscError(it.code, it.body?.string() ?: "No body"))
                    }
                }
            }
        } catch (e: IOException) {
            Log.d("Networking", "$e")
            return Result.failure(Error.NetworkError())
        } catch (e: ConnectException) {
            Log.d("Networking", "$e")
            return Result.failure(Error.NetworkError())
        }
    }

    /** Attempts a function. If the error is fixable the function retries, otherwise it returns the success or error
     * @param n times to try
     * @param payload The input for the function of type [T]
     * @param function the function to attempt that takes [T] and returns [Result] [R]
     * @return The output of the last try [Result] [R]
     */
    suspend fun <T, R> tryNTimes(
        n: Int,
        payload: T,
        function: suspend (T) -> Result<R>
    ): Result<R> {

        val res = function(payload)
        return when (res.isSuccess) {
            true -> res
            false -> {
                when (res.exceptionOrNull()) {
                    is Error.NetworkError -> {
                        if (n == 0) {
                        return Result.failure(res.exceptionOrNull()!!)
                    }
                        tryNTimes(n - 1, payload, function)
                    }
                    is Error.MiscError -> {
                        if (n == 0) {
                            return Result.failure(res.exceptionOrNull()!!)
                        }
                        tryNTimes(n - 1, payload, function)
                    }
                    is Error.NoCredentialsError -> {
                        if (n == 0) {
                            return Result.failure(res.exceptionOrNull()!!)
                        }
                        tryNTimes(n - 1, payload, function)
                    }
                    is Error.TokenError -> {
                        if (username!= null && password != null){
                            login(Pair(username!!, password!!))
                            tryNTimes(n-1, payload, function)
                        } else {
                            return res
                        }
                    }
                    else -> {
                        return res
                    }
                }
            }
        }
    }

    /** Get an image as [ByteArray] from an url
     * @param url The url to get the image from
     * @return the [Result] of [ByteArray]
     */
    suspend fun getImage(url: String): Result<ByteArray> {
        return try {
            val stream =
                withContext(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        URL(url).openConnection()
                    }.getInputStream()
                }
            val b = stream.readBytes()
            withContext(Dispatchers.IO) {
                stream.close()
            }
            Result.success(b)
        } catch (e: IOException) {
            Result.failure(Error.NetworkError())
        }
    }
}