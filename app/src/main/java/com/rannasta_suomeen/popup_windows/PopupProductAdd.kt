package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
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
                      private val settings: Settings):PopupRsm(activity, R.layout.popup_add_to_cabinet, root = null) {

    override fun bind(view: View) {
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
                when (switch.isChecked) {
                    true -> {
                        repo.addOrModifyToSelected(product.id,null)
                        window.dismiss()
                    }
                    false -> when(t.isSuccess){
                        true -> {
                            repo.addOrModifyToSelected(product.id,t.getOrThrow())
                            window.dismiss()
                        }
                        false -> Toast.makeText(context, "$edv is not a valid volume", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            findViewById<TextView>(R.id.textViewInvOwned).text = repo.selectedCabinet?.containedAmount(product)
                ?.show(settings)
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
}