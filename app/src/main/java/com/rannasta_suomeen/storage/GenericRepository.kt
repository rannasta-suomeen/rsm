package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.data_classes.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileNotFoundException
import java.lang.reflect.Type
import java.util.Optional
import kotlin.reflect.typeOf

private const val DRINKFILENAME = "drinks"
private const val PRODUCTFILENAME = "products"
private const val INGREDIENTFILENAME = "ingredients"
private const val INGREDIENTFORDRINKFILENAME = "drink_ingredients"

abstract class GenericRepository<R,T>(context: Context, fn: String) {
    private var memoryCopy: Optional<List<R>> = Optional.empty()
    private var syncedFromInternet = false
    abstract val input: T
    private val file: File =  File(context.filesDir, fn)
    abstract val getFn: (T) -> Result<List<R>>
    // BECAUSE FUCK YOU KOTLIN
    abstract val type: Class<Array<R>>


    val dataFlow: Flow<List<R>> = flow{
        when (memoryCopy.isPresent){
            true -> emit(memoryCopy.get())
            false -> {
                val t = loadFromFile()
                t?.let { emit(it) }
                memoryCopy = Optional.ofNullable(t)
                // TODO There is a (small) performance improvement in starting the network request BEFORE making the file fetch
                if (!syncedFromInternet){
                    val res = NetworkController.tryNTimes(5, input, getFn)
                    if (res.isSuccess){
                        val t = res.getOrThrow()
                        emit(t)
                        writeToFile(t)
                        memoryCopy = Optional.of(t)
                        syncedFromInternet = true
                    }
                }
            }
        }
        return@flow
    }

    private fun loadFromFile(): List<R>?{
        return try{
            val t: Array<R> = Gson().fromJson(file.readText(), type)
            t.toList()
        } catch (e: FileNotFoundException){
            null
        }
        catch (e: JsonParseException){
            Log.d("Storage","Failed json parse")
            null
        }
    }

    private fun writeToFile(list: List<R>){
        Log.d("Storage", "Ran write to file")
        file.writeText(Gson().toJson(list))
    }
}

class DrinkRepository(context: Context):
    GenericRepository<DrinkInfo, Unit>(context, DRINKFILENAME){
    override val input = Unit
    override val getFn = NetworkController::getDrinks
    override val type = Array<DrinkInfo>::class.java
}

class ProductRepository(context: Context): GenericRepository<Product, Pair<Int, Int>>(context, PRODUCTFILENAME){
    override val input = Pair(100000,0)
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

class TotalDrinkRepository(context: Context){
    private val ingRepo = IngredientRepository(context)
    private val recipeRepo = IngredientForDrinkRepository(context)
    private var drinkRepository = DrinkRepository(context)
    private var ingredientList: List<GeneralIngredient> = listOf()
    private var recipeList: List<IngredientsForDrink> = listOf()
    private var drinkList: List<DrinkInfo> = listOf()

    val dataFlow:Flow<List<DrinkTotal>> = flow {
        suspend fun emitCurrent(){
            emit(recipeList.mapNotNull { ings ->
                drinkList.find{it.recipe_id == ings.recipe_id}?.let { ings.toPointer(ingredientList)
                    ?.let { it1 -> DrinkTotal(it, it1) } }
            })
        }
        drinkRepository.dataFlow.collect{
            drinkList = it
            emitCurrent()
        }
        ingRepo.dataFlow.collect{
            ingredientList = it
            emitCurrent()
        }
        recipeRepo.dataFlow.collect{
            recipeList = it
            emitCurrent()
        }
    }
}