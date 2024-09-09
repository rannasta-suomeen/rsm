package com.rannasta_suomeen.storage

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.UnitType

val DRINK_VOLUME_UNIT = UnitType.Ml

class Settings(activity: Activity) {
    private val jackson = jacksonObjectMapper()

    init {
        jackson.findAndRegisterModules()
    }

    var prefAlko: Boolean
        get() = preferences.getBoolean(PREFALKO, false)
        set(value) {
            preferences.edit().putBoolean(PREFALKO, value).apply()
        }

    var prefUnit: UnitType
        get() {
            val t = preferences.getString(UNIT, "cl")
            return try {
                jackson.readValue(t, UnitType::class.java)
            } catch (e: Exception) {
                Log.d("Storage", "Encountered $e")
                UnitType.Cl
            }
        }
        set(value) {
            val t = jackson.writeValueAsString(value)
            preferences.edit().putString(UNIT, t).apply()
        }

    // Id of the cabinet last used
    var cabinet: Int
        get() = preferences.getInt(CABINET, 0)
        set(value) {
            preferences.edit().putInt(CABINET,value).apply()
        }

    private val preferences: SharedPreferences = activity.getSharedPreferences(activity.getString(R.string.pref_file_key),Context.MODE_PRIVATE)

    companion object{
        private const val CABINET = "cabinet"
        private const val PREFALKO = "prefalko"
        private const val UNIT = "unit"
    }
}