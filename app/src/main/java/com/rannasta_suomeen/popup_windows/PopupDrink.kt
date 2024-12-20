package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.RecipePartAdapter
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.storage.DRINK_VOLUME_UNIT
import com.rannasta_suomeen.storage.Randomizer
import com.rannasta_suomeen.storage.Settings
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max

class PopupDrink(private val drink: DrinkTotal, activity: Activity,private val owned: TreeMap<Int,GeneralIngredient>,private val randomizer: Randomizer,private val settings: Settings):PopupRsm(activity, R.layout.popup_drink_recipe, root = null) {

    private var amount = 1.0
    private var volume = DRINK_VOLUME_UNIT.convert(drink.drink.volume, settings.prefUnit) * amount
    private val df = DecimalFormat("#.#")

    override fun bind(view: View) {
        val adapter = RecipePartAdapter(owned, settings)

        fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return view.resources.getString(stringId, number)
        }
        with(view) {

            findViewById<TextView>(R.id.textViewRecipeAbv).text =
                displayDecimal(drink.drink.abvAvg, R.string.abv)
            findViewById<TextView>(R.id.textViewRecipeAer).text =
                displayDecimal(drink.drink.pricePerServing(settings), R.string.aer)
            findViewById<TextView>(R.id.textViewRecipeTags).text = drink.drink.displayTagList()
            val fsdView = findViewById<TextView>(R.id.textViewRecipeFsd)
            fsdView.text = displayDecimal(drink.drink.standardServings, R.string.shots)
            val priceView = findViewById<TextView>(R.id.textViewRecipePrice)
            priceView.text = displayDecimal(drink.drink.price(settings), R.string.price)
            findViewById<TextView>(R.id.textViewRecipeDescription).text = drink.drink.info
            findViewById<TextView>(R.id.textViewRecipeDrinkName).text = drink.drink.name
            findViewById<TextView>(R.id.textViewRecipeVolumeInfo).text = view.resources.getString(R.string.volumeUnit, settings.prefUnit)

            val volCounter = findViewById<EditText>(R.id.editTextRecipeVolume)
            val amountCounter = findViewById<EditText>(R.id.editTextRecipeParts)
            amountCounter.text.clear()
            amountCounter.text.append(amount.toString())
            volCounter.text.clear()
            volCounter.text.append(df.format(volume))

            // Setup recyclerview
            val t = findViewById<RecyclerView>(R.id.recyclerViewDrinkParts)
            t.adapter = adapter
            t.layoutManager = LinearLayoutManager(context)
            t.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

            // Edit texts that change the data
            fun updateByAmount(){
                volCounter.text.clear()
                volume = DRINK_VOLUME_UNIT.convert(drink.drink.volume, settings.prefUnit) * amount
                volCounter.text.append(df.format(volume))
                priceView.text = displayDecimal(calculatePrice(), R.string.price)
                fsdView.text = displayDecimal(drink.drink.standardServings * amount, R.string.shots)
                adapter.submitAmount(amount)
            }

            amountCounter.doAfterTextChanged { text ->
                if (amountCounter.hasFocus()){
                    amount = text.toString().toDoubleOrNull()?:4.0
                    updateByAmount()
                }
            }

            fun updateByVolume(){
                amount = volume / DRINK_VOLUME_UNIT.convert(drink.drink.volume, settings.prefUnit)
                amountCounter.text.clear()
                amountCounter.text.append(df.format(amount))
                priceView.text = displayDecimal(calculatePrice(), R.string.price)
                fsdView.text = displayDecimal(drink.drink.standardServings * amount, R.string.shots)
                adapter.submitAmount(amount)
            }

            volCounter.doAfterTextChanged { text ->
                if (volCounter.hasFocus()){
                    volume = text.toString().toDoubleOrNull()?:1.0
                    updateByVolume()
                }
            }

            // Buttons
            findViewById<ImageButton>(R.id.buttonRemoveAmount).setOnClickListener {
                amount -= 1
                amount = max(amount, 0.0)
                updateByAmount()
                updateByVolume()
            }
            findViewById<ImageButton>(R.id.buttonAddAmount).setOnClickListener {
                amount += 1
                updateByAmount()
                updateByVolume()
            }
            findViewById<ImageButton>(R.id.buttonRemoveVolume).setOnClickListener {
                volume -= 1
                volume = max(volume, 0.0)
                updateByVolume()
                updateByAmount()
            }
            findViewById<ImageButton>(R.id.buttonAddVolume).setOnClickListener {
                volume += 1
                updateByVolume()
                updateByAmount()
            }

            findViewById<Button>(R.id.buttonRecipeDrink).setOnClickListener {
                drinkTheDrink()
                window.dismiss()
            }

        }
        adapter.submitItems(drink.ingredients.recipeParts.toList(), owned)
        adapter.submitAmount(amount)
    }

    private fun calculatePrice(): Double{
        return drink.drink.price(settings) * amount
    }
    private fun drinkTheDrink(){
        val index = randomizer.getItems().indexOfFirst { it.drinkTotal == drink }
        randomizer.removeItemAt(index)
    }
}