package com.rannasta_suomeen

import android.util.Log
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.google.gson.Gson
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

    private const val serverAddress: String = "https://api.rannasta-suomeen.fi"
    private val client = OkHttpClient()

    sealed class Error(override val message: String) : Throwable() {
        class NetworkError : Error("No network")
        class TokenError : Error("Token not valid")
        class CredentialsError : Error("Username or password is wrong")
        class NoCredentialsError : Error("No username, password or token given")
        class RetryError(e: Throwable) : Error("Retried too many times, cause $e")
        class MiscError(code: Int, body: String) : Error("Error $code with body $body")
        class JsonError(s: String) : Error("Json $s could not be parsed")
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    sealed class CabinetOperation{
        val timestamp: Instant = Instant.now()
        class NewCabinet(val name: String): CabinetOperation()
        class DeleteCabinet(val id: Int): CabinetOperation()
        class AddItemToCabinet(val id:Int, val pid: Int, val amount: Int?): CabinetOperation()
        class ModifyCabinetProductAmount(val id: Int, val pid: Int, val amount: Int?): CabinetOperation()
        class RemoveItemFromCabinet(val id:Int, val pid: Int): CabinetOperation()
        class MakeItemUsable(val id:Int, val pid: Int): CabinetOperation()
        class MakeItemUnusable(val id:Int, val pid: Int): CabinetOperation()
    }


    /** Logs the user in. First item of the pair is the username, second is the password.
     * @return Result.success(Unit) if successful, Result.failure(e)
     */
    suspend fun login(payload: Pair<String, String>): Result<Unit> {
        val user = payload.first
        val password = payload.second
        val body: RequestBody = (user + "\n" + password).toRequestBody()
        val request = Request.Builder().url("$serverAddress/login").post(body).build()
        try {
            client.newCall(request).execute().use {
                when (it.code) {
                    200 -> {
                        // Server should always return a body with a successful login request
                        val token = it.body!!.string()
                        jwtToken = token.strip()
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

    /** Makes a request and returns a list of all drinks
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [DrinkInfo] or an [Error]
     */
    suspend fun getDrinks(_payload: Unit): Result<List<DrinkInfo>> {
        val request = Request.Builder().url("$serverAddress/drinks").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = Gson().fromJson(s, Array<DrinkInfo>::class.java)
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
            val list = Gson().fromJson(s, Int::class.java)
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

    /** Makes a request and tries to get
     * @return Result, either List of [CabinetCompact] or an [Error]
     */
    suspend fun getCabinets(_payload: Unit): Result<List<CabinetCompact>> {
        val request = Request.Builder().url("$serverAddress/cabinet").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = Gson().fromJson(s, Array<CabinetCompact>::class.java)
            list.toList()
        }
    }

    /** Makes a request and tries to delete a cabinet
     * @param cabinet Int, the id of the cabinet to delete
     * @return Result, either [Unit] or an [Error]
     */
    private suspend fun getCabinetProducts(cabinet: Int): Result<List<CabinetProductCompact>> {
        val request = Request.Builder().url("$serverAddress/items/$cabinet").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = Gson().fromJson(s, Array<CabinetProductCompact>::class.java)
            list.toList()
        }
    }

    /** Make a request and gets all cabinets owned by a user
     * @return [Result] of [List] of [CabinetStorable]
     */
    suspend fun getCabinetsTotal(_payload: Unit, dispatcher: CoroutineDispatcher = Dispatchers.IO): Result<List<CabinetStorable>>{
        val cabs = tryNTimes(5, Unit, ::getCabinets)
        return cabs.map {
            val t = it.map {
                CoroutineScope(dispatcher).async {
                    val products = tryNTimes(5,it.id,::getCabinetProducts)
                    when (products.isSuccess){
                        true -> it.toStorable(products.getOrThrow())
                        false -> null
                    }
                }
            }.awaitAll()
            Log.d("Networking", "Got $t as cabinet state")
            t.requireNoNulls()
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

    /** Makes a request and returns a list of all IngredientProductFilters
     * @param _payload An empty parameter to fit with rest of the architecture
     * @return Result, either a list of [IngredientProductFilter] or [Error]
     */
    suspend fun getProductIngredientFilter(_payload: Unit): Result<List<IngredientProductFilter>>{
        val request = Request.Builder().url("$serverAddress/ingprofilter").get()
        return makeTokenRequest(request){
            val s = it.body?.string()
            val list = Gson().fromJson(s, Array<IngredientProductFilter>::class.java)
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
            val list = Gson().fromJson(s, Array<Product>::class.java)
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
            val list = Gson().fromJson(s, Array<IngredientsForDrink>::class.java)
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
            val list = Gson().fromJson(s, Array<GeneralIngredient>::class.java)
            list.toList()
        }
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
                        Log.d("Networking", "${it.code}")
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
                        return Result.failure(Error.RetryError(res.exceptionOrNull()!!))
                    }
                        tryNTimes(n - 1, payload, function)
                    }
                    is Error.MiscError -> {
                        if (n == 0) {
                            return Result.failure(Error.RetryError(res.exceptionOrNull()!!))
                        }
                        tryNTimes(n - 1, payload, function)
                    }
                    is Error.NoCredentialsError -> {
                        if (n == 0) {
                            return Result.failure(Error.RetryError(res.exceptionOrNull()!!))
                        }
                        tryNTimes(n - 1, payload, function)
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