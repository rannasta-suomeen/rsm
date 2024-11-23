package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SwitchCompat
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.FilterMap
import com.rannasta_suomeen.adapters.checkDrinkAllowed
import com.rannasta_suomeen.addSimpleOnTextChangeLister
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.ingredientRepository
import java.util.*
import kotlin.math.absoluteValue

data class RandomizerSettings(
    var ingredientMap: FilterMap,
    var allowNewAlcohol: Boolean,
    var allowNewMixers: Boolean,
    var allowDuplicates: Boolean,
){
    private val random = Random()
    constructor():this(FilterMap(),true, true, true)
    private fun filterDrinks(drinks: List<DrinkTotal>, owned: TreeMap<Int, GeneralIngredient>): List<DrinkTotal>{
        var t = drinks
        if (!allowNewAlcohol){
            t = t.filter { it.canMakeAlcoholic(owned) }
        }
        if (!allowNewMixers){
            t= t.filter { it.canMakeNonAlcoholic(owned) }
        }
        return t.filter { ingredientMap.checkDrinkAllowed(it) }
    }

    fun generateDrink(drinks: List<DrinkTotal>, owned: TreeMap<Int, GeneralIngredient>, amount: Int): Result<List<DrinkTotal>>{
        val t = filterDrinks(drinks, owned).toMutableList()
        val x = mutableListOf<DrinkTotal>()
        (0..amount).forEach { _ ->
            if (t.isEmpty()){
                return Result.failure(IllegalStateException())
            }
            val pos = random.nextInt().absoluteValue % t.size
            x.add(t[pos])
            if (!allowDuplicates){
                t.removeAt(pos)
            }
        }
        return Result.success(x.toList())
    }
}

class PopupRandomizerOptions(activity: Activity,private val randomizerSettings: RandomizerSettings,private val parentView: View,private val callback: (RandomizerSettings, Int) -> Unit, private val clearCallback: () -> Unit): PopupRsm(activity, R.layout.popup_randomizer_options, null) {
    var amount: Int = 1
    override fun bind(view: View) {
        with(view){
            val amountText = findViewById<EditText>(R.id.editTextRandomizerAmount)
            val switchAlcohol = findViewById<SwitchCompat>(R.id.switchRandomizerAlcoholic)
            val switchMixers = findViewById<SwitchCompat>(R.id.switchRandomizerNonAlcoholic)
            val switchDuplicates = findViewById<SwitchCompat>(R.id.switchRandomizerDuplicates)
            val buttonIngredients = findViewById<Button>(R.id.buttonRandomizerIngredients)
            val buttonOk = findViewById<Button>(R.id.buttonRandomizerOk)
            val buttonCancel = findViewById<Button>(R.id.buttonRandomizerCancel)
            val buttonClear = findViewById<Button>(R.id.buttonRandomizerClear)

            amountText.addSimpleOnTextChangeLister {
                amount = it.toIntOrNull()?:1
            }

            buttonIngredients.setOnClickListener {
                PopupFilterIngredients(activity, ingredientRepository,randomizerSettings.ingredientMap){
                    randomizerSettings.ingredientMap = it
                }.show(parentView)
            }

            buttonClear.setOnClickListener {
                clearCallback()
                window.dismiss()
            }

            switchAlcohol.isChecked = randomizerSettings.allowNewAlcohol
            switchMixers.isChecked = randomizerSettings.allowNewMixers
            switchDuplicates.isChecked = randomizerSettings.allowDuplicates

            switchDuplicates.setOnCheckedChangeListener { compoundButton, b ->
                randomizerSettings.allowDuplicates = b
            }
            switchAlcohol.setOnCheckedChangeListener { _, b ->
                randomizerSettings.allowNewAlcohol = b
            }

            switchMixers.setOnCheckedChangeListener { _, b ->
                randomizerSettings.allowNewMixers = b
            }

            buttonOk.setOnClickListener {
                callback(randomizerSettings, amount)
                window.dismiss()
            }

            buttonCancel.setOnClickListener {
                window.dismiss()
            }
        }
    }
}