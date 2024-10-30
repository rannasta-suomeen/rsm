package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.DrinkCompactAdapter
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt
import kotlin.reflect.KFunction3

class PopupMixer(
    activity: Activity,
    private val mixer: GeneralIngredient,
    private val totalDrinkRepository: TotalDrinkRepository,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val settings: Settings,
    private val parentView: View,
    private val shoppingCart: ShoppingCart,
    private val randomizer: Randomizer): PopupRsm(activity, R.layout.popup_mixer,null) {

    private var drinkList = listOf<DrinkTotal>()
    private var ownedTotal: TreeMap<Int, GeneralIngredient> = TreeMap()
    private val nowAdapter = DrinkCompactAdapter(::onClickDrink)
    private val totalAdapter = DrinkCompactAdapter(::onClickDrink)
    private val usedAdapter = DrinkCompactAdapter(::onClickDrink)

    private fun onClickDrink(x: DrinkTotal){
        PopupDrink(x, activity, ownedTotal, randomizer, settings).show(parentView)
    }

    override fun bind(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                totalCabinetRepository.selectedCabinetFlow.collect{
                    it?.let {
                        val ownedMixers = it.mixers
                        val ownedAlcohol = totalCabinetRepository.productsToIngredients(it.products)
                        ownedTotal = (ownedMixers.map { it.ingredient } + ownedAlcohol).toTreemap()
                        CoroutineScope(Dispatchers.Main).launch {
                            upDateOnDataChange(view)
                        }
                    }
                }
            }
            totalDrinkRepository.dataFlow.collect{
                drinkList = it
                CoroutineScope(Dispatchers.Main).launch {
                    upDateOnDataChange(view)
                }
            }
        }
        with(view){
            findViewById<TextView>(R.id.textViewPopupMixerName).text = mixer.name
            // TODO: Separate mixer with price per liter and price per kpl
            findViewById<TextView>(R.id.textViewPopupMixerPrice).text = displayDecimal(mixer.price(settings), R.string.ppl)
            findViewById<Button>(R.id.buttonMixerInvCancel).setOnClickListener {
                this@PopupMixer.window.dismiss()
            }
            val switch = findViewById<SwitchCompat>(R.id.switchMixerInfinite)
            val textBox = findViewById<EditText>(R.id.editTextMixerAmount)
            switch.setOnCheckedChangeListener { compoundButton, b ->
                textBox.visibility = when (switch.isChecked){
                    true -> View.INVISIBLE
                    false -> View.VISIBLE
                }
            }
            findViewById<TextView>(R.id.textViewPopupMixerOwned).text = totalCabinetRepository.selectedCabinet?.containedAmount(mixer)?.show(settings)
            findViewById<Button>(R.id.buttonMixerInvAdd).setOnClickListener {
                totalCabinetRepository.selectedCabinet?.let {
                    val t = parseVolume(textBox.text.toString())
                    when (switch.isChecked){
                        true -> {
                            totalCabinetRepository.addOrModifyMixerToSelected(mixer.id, null)
                            window.dismiss()
                        }
                        false -> {
                            if (t.isFailure){
                                Toast.makeText(context, "${textBox.text} is not a valid volume", Toast.LENGTH_SHORT).show()
                            } else {
                                totalCabinetRepository.addOrModifyMixerToSelected(mixer.id, t.getOrThrow())
                                window.dismiss()
                            }
                        }
                    }
                }
            }

            findViewById<Button>(R.id.buttonMixerAddToCart).setOnClickListener {
                val t = parseVolume(textBox.text.toString())
                when (switch.isChecked){
                    true -> {
                        shoppingCart.addMixer(ShoppingCartMixer(mixer, null))
                        window.dismiss()
                    }
                    false -> {
                        if (t.isFailure){
                            Toast.makeText(context, "${textBox.text} is not a valid volume", Toast.LENGTH_SHORT).show()
                        } else {
                            shoppingCart.addMixer(ShoppingCartMixer(mixer, t.getOrThrow()))
                            window.dismiss()
                        }
                    }
                }
            }
            val recyclerNow = findViewById<RecyclerView>(R.id.recyclerMixerNewList)
            val recyclerTotal = findViewById<RecyclerView>(R.id.recyclerTotalList)
            val recyclerUsed = findViewById<RecyclerView>(R.id.recyclerUsedList)
            listOf(recyclerNow, recyclerTotal, recyclerUsed).forEach {
                it.layoutManager = LinearLayoutManager(context)
            }

            recyclerNow.adapter = nowAdapter
            recyclerTotal.adapter = totalAdapter
            recyclerUsed.adapter = usedAdapter
        }
    }

    private fun upDateOnDataChange(v: View){
        fun fast(fn: KFunction3<DrinkTotal, TreeMap<Int, GeneralIngredient>, GeneralIngredient, Boolean>): List<DrinkTotal>{
            return drinkList.filter{fn(it, ownedTotal, mixer)}
        }
        val now = fast(DrinkTotal::isMissingOnlyOrHas)
        val total = fast(DrinkTotal::isMissingOrOwnsAndHasAlcoholic).filter { !now.contains(it)}
        val used = drinkList.filter { it.contains(mixer) }.filter { !now.contains(it) && !total.contains(it)}

        with(v){
            findViewById<TextView>(R.id.textViewPopupMixerNewDrinks).text = resources.getString(R.string.new_now, now.size)
            findViewById<TextView>(R.id.textViewPopupMixerTotalDrinks).text = resources.getString(R.string.new_total, total.size)
            findViewById<TextView>(R.id.textViewPopupMixerUsedDrinks).text = resources.getString(R.string.used_total, used.size)
            nowAdapter.submitItems(now)
            totalAdapter.submitItems(total)
            usedAdapter.submitItems(used)
        }
    }

    private fun parseVolume(input: String): Result<Int>{
        val text = input.filter { !it.isWhitespace() }
        val numbers = text.takeWhile { it.isDigit() || listOf('.', ',').contains(it) }.toDoubleOrNull()
        val unit = text.dropWhile { it.isDigit() || listOf('.', ',').contains(it) }
        val unitActual = when (unit.lowercase()) {
            "cl" -> UnitType.Cl
            "ml" -> UnitType.Ml
            "oz" -> UnitType.Oz
            "" -> UnitType.Kpl
            "b" -> UnitType.Kpl
            "l" -> UnitType.L
            else -> null
        }

        val convertedVolume = when (unitActual) {
            null -> 0.0
            UnitType.Kpl -> numbers ?: 0.0
            else -> numbers?.let { unitActual.convert(it, UnitType.Ml) }
        }

        return when(numbers != null && unitActual != null){
            true -> Result.success(convertedVolume!!.roundToInt())
            false -> Result.failure(java.lang.NumberFormatException("$text cannot be converted to volume"))
        }
    }
}