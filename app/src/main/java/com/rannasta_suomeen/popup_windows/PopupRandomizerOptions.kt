package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.FilterMap
import com.rannasta_suomeen.adapters.checkDrinkAllowed
import com.rannasta_suomeen.addSimpleOnTextChangeLister
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.DrinkType
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.findAllTags
import com.rannasta_suomeen.ingredientRepository
import java.util.Random
import java.util.TreeMap
import kotlin.math.absoluteValue

data class RandomizerSettings(
    var ingredientMap: FilterMap,
    var allowDuplicates: Boolean,
    var hidden: Boolean,
    var tags: List<String>,
    val allTags: List<String>,
){
    private val random = Random()
    constructor(allTags: List<String>):this(FilterMap(),false, true, allTags, allTags)
    private fun filterDrinks(drinks: List<DrinkTotal>, owned: TreeMap<Int, GeneralIngredient>): List<DrinkTotal>{
        val disAllowedTags = allTags.toMutableList()
        disAllowedTags.removeAll{tags.contains(it)}
        fun checkTags(drink: DrinkTotal):Boolean{
            for (i in disAllowedTags){
                if(when (i) {
                        // TODO: Make this work without hardcoded strings
                        "Cocktail" -> drink.drink.type == DrinkType.Cocktail
                        "Missing Alcohol" -> !drink.canMakeAlcoholic(owned)
                        "Missing Groceries" -> !drink.canMakeNonAlcoholic(owned)
                        "Shot" -> drink.drink.type == DrinkType.Shot
                        "Punch" -> drink.drink.type == DrinkType.Punch
                        "Tagless" -> drink.drink.tagList.isEmpty()
                        else -> {
                            drink.drink.tagList.contains(i)
                        }
                    }){
                    return false
                }
            }
            return true
        }
        return drinks.filter {
            checkTags(it)
        }.filter { ingredientMap.checkDrinkAllowed(it) }
    }


    fun generateDrink(drinks: List<DrinkTotal>, owned: TreeMap<Int, GeneralIngredient>, amount: Int): Result<List<DrinkTotal>>{
        val t = filterDrinks(drinks, owned).toMutableList()
        val x = mutableListOf<DrinkTotal>()
        (0 until amount).forEach { _ ->
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

class PopupRandomizerOptions(activity: Activity,private val drinkList: List<DrinkTotal>,private val randomizerSettings: RandomizerSettings,private val parentView: View,private val callback: (RandomizerSettings, Int) -> Unit, private val clearCallback: () -> Unit): PopupFilterBase(activity, R.layout.popup_randomizer_options) {
    var amount: Int = 1
    override fun bind(view: View) {
        with(view){
            val amountText = findViewById<EditText>(R.id.editTextRandomizerAmount)
            val switchDuplicates = findViewById<SwitchCompat>(R.id.switchRandomizerDuplicates)
            val buttonIngredients = findViewById<Button>(R.id.buttonRandomizerIngredients)
            val buttonOk = findViewById<Button>(R.id.buttonRandomizerOk)
            val buttonCancel = findViewById<Button>(R.id.buttonRandomizerCancel)
            val buttonClear = findViewById<Button>(R.id.buttonRandomizerClear)
            val switchHide = findViewById<SwitchCompat>(R.id.switchRandomizerHidden)
            val buttonTags = findViewById<Button>(R.id.buttonRandomizerTags)

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

            switchDuplicates.isChecked = randomizerSettings.allowDuplicates
            switchHide.isChecked = randomizerSettings.hidden

            switchDuplicates.setOnCheckedChangeListener { compoundButton, b ->
                randomizerSettings.allowDuplicates = b
            }
            switchHide.setOnCheckedChangeListener { _, b ->
                randomizerSettings.hidden = b
            }

            buttonOk.setOnClickListener {
                callback(randomizerSettings, amount)
                window.dismiss()
            }

            buttonCancel.setOnClickListener {
                window.dismiss()
            }

            buttonTags.setOnClickListener {
                val tagDialog = multiOptionDialog(BASE_TAGS.map { activity.getString(it) } + drinkList.findAllTags(),{it},randomizerSettings.tags,"Select Tags"){
                    randomizerSettings.tags = it.toList()
                }
                tagDialog.show()
            }

            buttonTags.setOnLongClickListener {
                if (randomizerSettings.tags.isNotEmpty()){
                    randomizerSettings.tags = listOf()
                    Toast.makeText(activity, "Deselected all tags", Toast.LENGTH_SHORT).show()
                } else {
                    randomizerSettings.tags = drinkList.findAllTags()
                    Toast.makeText(activity, "Selected all tags", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }
    }
}