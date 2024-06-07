package com.rannasta_suomeen.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedStorage(context: Context) {
    private val fileName = "com.rannasta-suomeen.rsm.encrypted"
    private val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val preferences = EncryptedSharedPreferences(
        context,
        fileName,
        key,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)

    var userName
    get() = preferences.getString("userName", null)
    set(value) {preferences.edit().putString("userName", value).apply()}

    var password
        get() = preferences.getString("password", null)
        set(value) {preferences.edit().putString("password", value).apply()}
}