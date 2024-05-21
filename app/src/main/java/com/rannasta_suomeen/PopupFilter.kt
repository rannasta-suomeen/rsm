package com.rannasta_suomeen

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import com.rannasta_suomeen.data_classes.Product

class PopupFilter(
    activity: FragmentActivity,
    private val parent: View,
    private val updateFun: () -> Unit
) {

    private var window: PopupWindow

    class Settings(
        var name: String?,
        var abvMin: Double?,
        var abvMax: Double?,
        var volMin: Int?,
        var volMax: Int?,
        var priceMin: Double?,
        var priceMax: Double?,
    )

    private var settings: Settings = Settings(null,null, null,null,null,null, null)

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

        // TODO(Implement categories)
        val buttonCategory: Button = t(R.id.buttonCategory)
        val buttonSubCategory: Button = t(R.id.buttonSub)
        val buttonRetailer: Button = t(R.id.buttonRetailer)

        val buttonDismiss: Button = t(R.id.buttonProductFilterOk)

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

    fun show(){
        window.showAtLocation(this.parent, Gravity.TOP, 0, 0)
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

        return mutList.toList()
    }
}