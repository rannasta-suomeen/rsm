package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedStorage(context: Context) {
    private val fileName = "com.rannasta-suomeen.rsm.encrypted"
    private val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val preferences = try {
        EncryptedSharedPreferences(
        context,
        fileName,
        key,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    } catch (e: RuntimeException){
        Log.d("Storage", "Creating encryptedPreferences failed, deleting old file and trying anew")
        context.filesDir.parentFile?.listFiles()?.find {
            it.name=="shared_prefs"
        }?.listFiles()?.find { it.name == fileName }?.delete()
        EncryptedSharedPreferences(
            context,
            fileName,
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }

    var userName
    get() = preferences.getString("userName", null)
    set(value) {preferences.edit().putString("userName", value).apply()}

    var password
        get() = preferences.getString("password", null)
        set(value) {preferences.edit().putString("password", value).apply()}
}