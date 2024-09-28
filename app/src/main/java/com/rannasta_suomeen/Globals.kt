package com.rannasta_suomeen

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import com.rannasta_suomeen.storage.IngredientRepository
import com.rannasta_suomeen.storage.ProductRepository
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository

lateinit var totalDrinkRepository: TotalDrinkRepository
lateinit var productRepository: ProductRepository
lateinit var ingredientRepository: IngredientRepository
lateinit var totalCabinetRepository: TotalCabinetRepository


@Deprecated("Use View.displayDecimal instead")
fun displayDecimal(x: Double, stringId: Int, view: View): String{
    val number = String.format("%.1f", x)
    return view.resources.getString(stringId, number)
}

fun View.displayDecimal(x: Double, stringId: Int): String{
    val number = String.format("%.1f", x)
    return resources.getString(stringId, number)
}

private class TextWatcherRsm(private val fn: () -> Unit) : TextWatcher {
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    override fun afterTextChanged(p0: Editable?) {
        fn()
    }

}

fun EditText.addSimpleOnTextChangeLister (fn: (s: String) -> Unit){
    this.addTextChangedListener(TextWatcherRsm{
        fn(this.text.toString())
    })
}