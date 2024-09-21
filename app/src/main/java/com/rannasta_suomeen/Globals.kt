package com.rannasta_suomeen

import android.view.View
import com.rannasta_suomeen.storage.IngredientRepository
import com.rannasta_suomeen.storage.ProductRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository

lateinit var totalDrinkRepository: TotalDrinkRepository
lateinit var productRepository: ProductRepository
lateinit var ingredientRepository: IngredientRepository

@Deprecated("Use View.displayDecimal instead")
fun displayDecimal(x: Double, stringId: Int, view: View): String{
    val number = String.format("%.1f", x)
    return view.resources.getString(stringId, number)
}

fun View.displayDecimal(x: Double, stringId: Int): String{
    val number = String.format("%.1f", x)
    return resources.getString(stringId, number)
}