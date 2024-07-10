package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.SwitchCompat
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Product
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PopupProductAdd(private val product: Product,
                      private val repo: TotalCabinetRepository,
                      private val imgRepo :ImageRepository,
                      activity: Activity,
                      private val settings: Settings) {

    private var window: PopupWindow
    init {
        val view = activity.layoutInflater.inflate(R.layout.popup_add_to_cabinet, null)

        fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return view.resources.getString(stringId, number)
        }

        window = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.isFocusable = true
        with(view) {
            CoroutineScope(Dispatchers.IO).launch {
                val img = imgRepo.getImage(product.img)
                CoroutineScope(Dispatchers.Main).launch {
                    findViewById<ImageView>(R.id.imageViewInv).setImageBitmap(img)
                }
            }

            findViewById<TextView>(R.id.textViewInvName).text = product.name
            val edv = findViewById<EditText>(R.id.editTextInvVolume).text
            edv.clear()
            val showable = UnitType.cl.displayInDesiredUnit(product.volumeCl(), settings.prefUnit)
            edv.append(showable)

            findViewById<Button>(R.id.buttonInvCancel).setOnClickListener {
                window.dismiss()
            }

            val switch = findViewById<SwitchCompat>(R.id.switchInvInfinite)

            findViewById<Button>(R.id.buttonInvAdd).setOnClickListener {
                val t = parseVolume(edv.toString())
                val isOwned = repo.selectedCabinet?.isOwned(product) == true

                fun handleOperation(handler: (Int?)-> Unit){
                    when (switch.isChecked) {
                        true -> {
                            handler(null)
                            window.dismiss()
                        }
                        false -> when(t.isSuccess){
                            true -> {
                                handler(t.getOrThrow())
                                window.dismiss()
                            }
                            false -> Toast.makeText(context, "$edv is not a valid volume", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                when(isOwned){
                    true -> handleOperation (::modifyAmount)
                    false -> handleOperation (::addToCabinet)
                }
            }

            findViewById<TextView>(R.id.textViewInvOwned).text = repo.selectedCabinet?.owned(product)
                ?.show(settings)
        }
    }

    private fun addToCabinet(amount: Int?){
        CoroutineScope(Dispatchers.IO).launch {
            repo.selectedCabinet?.let {
                repo.addItemToCabinet(it.id,product.id, amount)
            }
        }
    }
    private fun modifyAmount(amount: Int?){
        CoroutineScope(Dispatchers.IO).launch {
            repo.selectedCabinet?.let {
                repo.modifyCabinetProductAmount(it.id, product.id, amount)
            }
        }
    }

    private fun parseVolume(input: String): Result<Int>{
        val text = input.filter { !it.isWhitespace() }
        val numbers = text.takeWhile { it.isDigit() || listOf('.', ',').contains(it) }.toDoubleOrNull()
        val unit = text.dropWhile { it.isDigit() || listOf('.', ',').contains(it) }
        val unitActual = when (unit.lowercase()){
            "cl" -> UnitType.cl
            "ml" -> UnitType.ml
            "oz" -> UnitType.oz
            "" -> UnitType.kpl
            "b" -> UnitType.kpl
            else -> null
        }

        val convertedVolume = when(unitActual){
            null -> 0.0
            UnitType.kpl -> numbers?.let { it * product.volume*1000 } ?: 0.0
            else -> numbers?.let {unitActual.convert(it, UnitType.ml)}
        }

        return when(numbers != null && unitActual != null){
            true -> Result.success(convertedVolume!!.roundToInt())
            false -> Result.failure(java.lang.NumberFormatException("$text cannot be converted to volume"))
        }
    }

    fun show(parent: View){
        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0,0)
    }
}