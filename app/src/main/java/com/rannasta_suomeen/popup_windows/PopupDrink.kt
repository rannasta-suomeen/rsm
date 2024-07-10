package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.R
import com.rannasta_suomeen.RecipePartAdapter
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.storage.DRINK_VOLUME_UNIT
import com.rannasta_suomeen.storage.Settings
import java.text.DecimalFormat
import kotlin.math.max

class PopupDrink(val drink: DrinkTotal, activity: Activity, owned: List<GeneralIngredient>,private val settings: Settings) {

    private var amount = 1.0
    private var window: PopupWindow
    private var volume = DRINK_VOLUME_UNIT.convert(drink.drink.total_volume, settings.prefUnit) * amount
    init {
        val adapter = RecipePartAdapter(activity.applicationContext, owned, settings)
        val view = activity.layoutInflater.inflate(R.layout.popup_drink_recipe, null)

        fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return view.resources.getString(stringId, number)
        }

        window = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.isFocusable = true
        with(view) {

            findViewById<TextView>(R.id.textViewRecipeAbv).text = displayDecimal(drink.drink.abv_average, R.string.abv)
            findViewById<TextView>(R.id.textViewRecipeAer).text = displayDecimal(drink.drink.pricePerServing(settings), R.string.aer)
            val fsdView = findViewById<TextView>(R.id.textViewRecipeFsd)
            fsdView.text = displayDecimal(drink.drink.standard_servings, R.string.shots)
            val priceView = findViewById<TextView>(R.id.textViewRecipePrice)
            priceView.text = displayDecimal(drink.drink.price(settings), R.string.price)
            findViewById<TextView>(R.id.textViewRecipeDescription).text = drink.drink.info
            findViewById<TextView>(R.id.textViewRecipeDrinkName).text = drink.drink.name
            findViewById<TextView>(R.id.textViewRecipeVolumeInfo).text = "Volume[${settings.prefUnit}]"

            val volCounter = findViewById<EditText>(R.id.editTextRecipeVolume)
            val amountCounter = findViewById<EditText>(R.id.editTextRecipeParts)
            amountCounter.text.clear()
            amountCounter.text.append(amount.toString())
            volCounter.text.clear()
            volCounter.text.append(volume.toString())

            // Setup recyclerview
            val t = findViewById<RecyclerView>(R.id.recyclerViewDrinkParts)
            t.adapter = adapter
            t.layoutManager = LinearLayoutManager(context)
            t.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

            // Edit texts that change the data
            fun updateByAmount(){
                volCounter.text.clear()
                volume = DRINK_VOLUME_UNIT.convert(drink.drink.total_volume, settings.prefUnit) * amount
                volCounter.text.append(volume.toString())
                priceView.text = displayDecimal(calculatePrice(), R.string.price)
                fsdView.text = displayDecimal(drink.drink.standard_servings*amount, R.string.shots)
                adapter.submitAmount(amount)
            }

            amountCounter.doAfterTextChanged { text ->
                if (amountCounter.hasFocus()){
                    amount = text.toString().toDoubleOrNull()?:4.0
                    updateByAmount()
                }
            }

            fun updateByVolume(){
                amount = volume/DRINK_VOLUME_UNIT.convert(drink.drink.total_volume, settings.prefUnit)
                val df = DecimalFormat("#.#")
                amount = df.format(amount).toDouble()
                amountCounter.text.clear()
                amountCounter.text.append(amount.toString())
                priceView.text = displayDecimal(calculatePrice(), R.string.price)
                fsdView.text = displayDecimal(drink.drink.standard_servings*amount, R.string.shots)
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

        }
        adapter.submitItems(drink.ingredients.recipeParts.toList(), owned)
        adapter.submitAmount(amount)
    }

    private fun calculatePrice(): Double{
        return drink.drink.price(settings) * amount
    }

    fun show(parent: View){
        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0,0)
    }
}