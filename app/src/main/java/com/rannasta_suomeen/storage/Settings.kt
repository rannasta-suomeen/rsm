package com.rannasta_suomeen.storage

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.UnitType

val DRINK_VOLUME_UNIT = UnitType.ml

class Settings(activity: Activity) {

    var prefAlko: Boolean
        get() = preferences.getBoolean(PREFALKO, false)
        set(value) {
            preferences.edit().putBoolean(PREFALKO, value).apply()
        }

    var prefUnit: UnitType
        get() {
            val t = preferences.getString(UNIT, "cl")
            return Gson().fromJson<UnitType>(t, UnitType::class.java)
        }
        set(value) {
            val t = Gson().toJson(value)
            preferences.edit().putString(UNIT, t).apply()
        }

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