package com.rannasta_suomeen

import android.view.View
import com.rannasta_suomeen.storage.IngredientRepository
import com.rannasta_suomeen.storage.ProductRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository

lateinit var totalDrinkRepository: TotalDrinkRepository
lateinit var productRepository: ProductRepository
lateinit var ingredientRepository: IngredientRepository

fun displayDecimal(x: Double, stringId: Int, view: View): String{
    val number = String.format("%.1f", x)
    return view.resources.getString(stringId, number)
}