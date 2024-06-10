package com.rannasta_suomeen

import android.util.Log
import com.google.gson.Gson
import com.rannasta_suomeen.data_classes.DrinkInfo
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.IngredientsForDrink
import com.rannasta_suomeen.data_classes.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.URL

object NetworkController {
    private var jwtToken: String? = null

    // TODO: Temp address that is the host
    private const val serverAddress: String = "http://10.0.2.2:8000"
    private val client = OkHttpClient()

    sealed class Error(override val message: String) : Throwable() {
        class NetworkError : Error("No network")
        class TokenError : Error("Token not valid")
        class CredentialsError : Error("Username or password is wrong")
        class NoCredentialsError : Error("No username, password or token given")
        class RetryError : Error("Retried too many times, cause")
        class MiscError(code: Int, body: String) : Error("Error $code with body $body")
        class JsonError(s: String) : Error("Json $s could not be parsed")
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
    fun getDrinks(_payload: Unit): Result<List<DrinkInfo>> {
        val request = Request.Builder().url("$serverAddress/drinks").get()
        return makeTokenRequest(request) {
            val s = it.body?.string()
            val list = Gson().fromJson(s, Array<DrinkInfo>::class.java)
            list.toList()
        }
    }

    /** Makes a request and returns a list of all drinks
     * @param payload a [Pair] of Limit and Offset
     * @return Result, either a list of [Product] or an [Error]
     */
    fun getProducts(_payload: Unit): Result<List<Product>> {
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
     * @return Result, either a list of [IngredientsForDrink] or [Error]
     */
    fun getDrinkRecipes(_payload: Unit):Result<List<IngredientsForDrink>>{
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
     * @return Result, either a list of [GeneralIngredient] or [Error]
     */
    fun getIngredients(_payload: Unit):Result<List<GeneralIngredient>>{
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
    private fun <R> makeTokenRequest(rb: Request.Builder, function: (Response) -> R): Result<R> {
        try {
            while (jwtToken == null) {
                /* no-op */
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
        if (n == 0) {
            return Result.failure(Error.RetryError())
        }
        val res = function(payload)
        return when (res.isSuccess) {
            true -> res
            false -> {
                when (res.exceptionOrNull()) {
                    is Error.NetworkError -> tryNTimes(n - 1, payload, function)
                    is Error.MiscError -> tryNTimes(n - 1, payload, function)
                    is Error.NoCredentialsError -> {
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