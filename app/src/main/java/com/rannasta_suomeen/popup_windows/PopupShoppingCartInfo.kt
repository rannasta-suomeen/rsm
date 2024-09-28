package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupShoppingCartInfo(activity: Activity,private val shoppingCart: ShoppingCart,private val settings: Settings,private val totalCabinetRepository: TotalCabinetRepository,private val totalDrinkRepository: TotalDrinkRepository):PopupRsm(activity, R.layout.popup_shopping_cart, null) {
    private var owned = listOf<GeneralIngredient>()
    private var drinks = listOf<DrinkTotal>()
    override fun bind(view: View) {
        with(view){
            findViewById<TextView>(R.id.textViewShoppingCartPrice).text = displayDecimal(shoppingCart.totalPrice(),R.string.price)
            findViewById<TextView>(R.id.textViewShoppingCartVolume).text = UnitType.Ml.displayInDesiredUnit(shoppingCart.totalVolume(UnitType.Ml), settings.prefUnit)
            findViewById<TextView>(R.id.textViewShoppingCartPps).text = displayDecimal(shoppingCart.pps(),R.string.aer)
            findViewById<TextView>(R.id.textViewShoppingCartItems).text = resources.getString(R.string.kpl_int,shoppingCart.amountOfItems())
            findViewById<TextView>(R.id.textViewShoppingCartShots).text = displayDecimal(shoppingCart.amountOfShots(),R.string.shots)
            val newDrinksText = findViewById<TextView>(R.id.textViewShoppingCartNewDrinks)

            CoroutineScope(Dispatchers.IO).launch {
                launch {
                    totalCabinetRepository.ownedIngredientFlow.collect{
                        owned = it
                        CoroutineScope(Dispatchers.Main).launch {
                            newDrinksText.text = newDrinksFromEither(view)
                        }
                    }
                }
                totalDrinkRepository.dataFlow.collect{
                    drinks = it
                    CoroutineScope(Dispatchers.Main).launch {
                        newDrinksText.text = newDrinksFromEither(view)
                    }
                }
            }

            findViewById<Button>(R.id.buttonClose).setOnClickListener {
                window.dismiss()
            }
        }
    }

    private fun newDrinksFromEither(v: View): String{
        return v.resources.getString(
            R.string.n_new_drinks,
            newDrinksWithThis(shoppingCart.getItems().map { CabinetProduct(0,it.product,0,null,true) }).size)
    }

    private fun newDrinksWithThis(newProducts: List<CabinetProduct>):List<DrinkTotal>{
        return totalDrinkRepository.makeableWithNew(owned.toTreemap(), totalCabinetRepository.productsToIngredients(newProducts))
    }
}