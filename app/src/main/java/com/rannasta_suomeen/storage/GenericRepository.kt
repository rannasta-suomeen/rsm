package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.data_classes.DrinkRecipe
import com.rannasta_suomeen.data_classes.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileNotFoundException
import java.util.Optional

private const val DRINKFILENAME = "drinks"
private const val PRODUCTFILENAME = "products"

abstract class GenericRepository<R,T>(context: Context, networkController: NetworkController) {
    private var memoryCopy: Optional<List<R>> = Optional.empty()
    private var syncedFromInternet = false
    abstract val input: T
    abstract val file: File
    abstract val getFn: (T) -> Result<List<R>>

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

    abstract fun loadFromFile(): List<R>?

    private fun writeToFile(list: List<R>){
        file.writeText(Gson().toJson(list))
    }
}

class DrinkRepository(context: Context, networkController: NetworkController):
    GenericRepository<DrinkRecipe, Unit>(context, networkController){
    override val file = File(context.filesDir, DRINKFILENAME)
    override val input = Unit
    override val getFn = networkController::getDrinks

    override fun loadFromFile(): List<DrinkRecipe>?{
        return try{
            val typeToken = object: TypeToken<Array<DrinkRecipe>>() {}.type
            val t: Array<DrinkRecipe> = Gson().fromJson(file.readText(), typeToken)
            t.toList()
        } catch (e: FileNotFoundException){
            null
        }
        catch (e: JsonParseException){
            Log.d("Storage","Failed json parse")
            null
        }
    }
}

class ProductRepository(context: Context, networkController: NetworkController): GenericRepository<Product, Pair<Int, Int>>(context,networkController){
    override val file: File = File(context.filesDir, PRODUCTFILENAME)
    override val input = Pair(10000,0)
    override val getFn = networkController::getProducts

    override fun loadFromFile(): List<Product>?{
        return try{
            val typeToken = object: TypeToken<Array<Product>>() {}.type
            val t: Array<Product> = Gson().fromJson(file.readText(), typeToken)
            t.toList()
        } catch (e: FileNotFoundException){
            null
        }
        catch (e: JsonParseException){
            Log.d("Storage","Failed json parse")
            null
        }
    }
}