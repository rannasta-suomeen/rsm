package com.rannasta_suomeen

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.Error

object NetworkController {
    private var jwtToken: String? = null
    // TODO: Temp address that is the host
    private const val serverAddress: String = "http://10.0.2.2:8000"
    private val client = OkHttpClient()

    sealed class Error(override val message: String): Throwable(){
        class NetworkError: Error("No network")
        class TokenError: Error("Token not valid")
        class CredentialsError: Error("Username or password is wrong")
        class NoCredentialsError: Error("No username, password or token given")
    }

    suspend fun login(user: String, password: String):Result<Unit> {
        val body: RequestBody = (user + "\n" + password).toRequestBody()
        val request = Request.Builder().url("$serverAddress/login").post(body).build()
        try {
            client.newCall(request).execute().use {
                when (it.code){
                    200 -> {
                        // Server should always return a body with a successful login request
                        val token = it.body!!.string()
                        Log.d("Networking", "Token is $token")
                        jwtToken = token
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
                        return Result.failure(Error.TokenError())
                    }
                }
            }
        } catch (e: IOException){
            Log.d("Networking", "$e")
            return Result.failure(Error.NetworkError())
        }
    }
}