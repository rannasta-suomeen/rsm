package com.rannasta_suomeen.popup_windows


import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*

class PopupFilter(
    private val activity: FragmentActivity,
    private val updateFun: () -> Unit
) {

    private var window: PopupWindow

    private inline fun <reified T: Any> multiOptionDialog(items: List<T>, title: String, crossinline endfun: (Array<T>) -> Unit): AlertDialog {
        val dialog = AlertDialog.Builder(activity)

        dialog.setTitle(title)
        val booleanArray = items.map { true }.toBooleanArray()
        dialog.setMultiChoiceItems(items.map { it.toString() }.toTypedArray(),booleanArray){_,_,_->}
        dialog.setPositiveButton("Ok"){ x, y ->
            endfun(items.zip(booleanArray.toTypedArray()).filter { it.second }.map { it.first }.toTypedArray())
        }
        dialog.setNegativeButton("Cancel"){ _, _->}
        return dialog.create()
    }

    class Settings(
        var name: String?,
        var abvMin: Double?,
        var abvMax: Double?,
        var volMin: Int?,
        var volMax: Int?,
        var priceMin: Double?,
        var priceMax: Double?,
        var retailers: List<Retailer>
    ){
        var selectedSubcategory: Array<Subcategory> = Subcategory.values()
    }

    private var settings: Settings = Settings(null,null, null,null,null,null, null, listOf(Retailer.alko, Retailer.superalko))

    init{
        val view = activity.layoutInflater.inflate(R.layout.popup_product_filer, null)
        window = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.isFocusable = true

        /**
         * Shortens my time to write
         */
        fun<T: View> t(id: Int): T{
            return view.findViewById<T>(id)
        }
        fun ed(id: Int): EditText{
            return t(id)
        }

        val nameSelect = ed(R.id.editTextTextProductName)

        val abvMin = ed(R.id.editTextAbvMin)
        val abvMax = ed(R.id.editTextAbvMax)

        val volMin = ed(R.id.editTextVolumeMin)
        val volMax = ed(R.id.editTextVolumeMax)

        val priceMin = ed(R.id.editTextPriceMin)
        val priceMax = ed(R.id.editTextPriceMax)


        val buttonRetailer: Button = t(R.id.buttonRetailer)
        val buttonDismiss: Button = t(R.id.buttonProductFilterOk)

        val buttonCategory: Button = view.findViewById(R.id.buttonCategory)
        val categoryDialog = multiOptionDialog(Subcategory.values().toList(),"Choose categories"){settings.selectedSubcategory = it}

        val retailerDialog = multiOptionDialog(Retailer.values().toList(),"Choose Retailers"){settings.retailers = it.toList()}

        buttonCategory.setOnClickListener {
            categoryDialog.show()
        }
        buttonRetailer.setOnClickListener {
            retailerDialog.show()
        }

        buttonDismiss.setOnClickListener {
            this.settings.name = nameSelect.text.toString()
            this.settings.abvMin = abvMin.text.toString().toDoubleOrNull()
            this.settings.abvMax = abvMax.text.toString().toDoubleOrNull()

            this.settings.volMin = volMin.text.toString().toIntOrNull()
            this.settings.volMax = volMax.text.toString().toIntOrNull()

            this.settings.priceMin = priceMin.text.toString().toDoubleOrNull()
            this.settings.priceMax = priceMax.text.toString().toDoubleOrNull()

            updateFun()
            window.dismiss()
        }
    }

    fun show(parent: View){
        window.showAtLocation(parent, Gravity.TOP, 0, 0)
    }

    fun filter(list: List<Product>): List<Product>{
        val mutList = list.toMutableList()

        fun<T> quickRemove(cond: T?, predicate: (Product) -> Boolean){
            if (cond != null){
                mutList.removeAll(predicate)
            }
        }

        quickRemove(settings.name){!it.name.contains(settings.name!!,true)}
        quickRemove(settings.abvMin){it.abv<settings.abvMin!!}
        quickRemove(settings.abvMax){it.abv>settings.abvMax!!}

        quickRemove(settings.priceMin){it.price<settings.priceMin!!}
        quickRemove(settings.priceMax){it.price>settings.priceMax!!}

        quickRemove(settings.volMin){it.volumeCl()<settings.volMin!!}
        quickRemove(settings.volMax){it.volumeCl()>settings.volMax!!}

        return mutList.filter { settings.selectedSubcategory.contains(from(it.subcategory_id)) }.filter { settings.retailers.contains(it.retailer) }
    }
}