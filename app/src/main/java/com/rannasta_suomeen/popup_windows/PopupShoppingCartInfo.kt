package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.DrinkCompactAdapter
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock

class PopupShoppingCartInfo(private val fragmentView: View,private val activity: Activity,private val shoppingCart: ShoppingCart,private val settings: Settings,private val totalCabinetRepository: TotalCabinetRepository,private val totalDrinkRepository: TotalDrinkRepository):PopupRsm(activity, R.layout.popup_shopping_cart, null) {
    private var owned = TreeMap<Int,GeneralIngredient>()
    private var drinks = listOf<DrinkTotal>()
    private val rwLock = ReentrantReadWriteLock()
    override fun bind(view: View) {
        with(view){
            findViewById<TextView>(R.id.textViewShoppingCartPrice).text = displayDecimal(shoppingCart.totalPrice(),R.string.price)
            findViewById<TextView>(R.id.textViewShoppingCartVolume).text = UnitType.Ml.displayInDesiredUnit(shoppingCart.totalVolume(UnitType.Ml), settings.prefUnit)
            findViewById<TextView>(R.id.textViewShoppingCartPps).text = displayDecimal(shoppingCart.pps(),R.string.aer)
            findViewById<TextView>(R.id.textViewShoppingCartItems).text = resources.getString(R.string.kpl_int,shoppingCart.amountOfItems())
            findViewById<TextView>(R.id.textViewShoppingCartShots).text = displayDecimal(shoppingCart.amountOfShots(),R.string.shots)
            val newDrinksTextStrict = findViewById<TextView>(R.id.textViewShoppingCartNewDrinksStrict)
            val newDrinksTextAlcohol = findViewById<TextView>(R.id.textViewShoppingCartNewDrinksAlcohol)
            val recyclerStrict = findViewById<RecyclerView>(R.id.recyclerViewShoppingCartNewDrinksStrict)
            val recyclerAlcohol = findViewById<RecyclerView>(R.id.recyclerViewShoppingCartNewAlcoholic)
            recyclerStrict.layoutManager = LinearLayoutManager(view.context)
            recyclerAlcohol.layoutManager = LinearLayoutManager(view.context)
            fun onClickDrink(x: DrinkTotal){
                rwLock.readLock().lock()
                PopupDrink(x, activity, owned, settings).show(fragmentView)
                rwLock.readLock().unlock()
            }
            val strictAdapter = DrinkCompactAdapter(::onClickDrink)
            val alcoholAdapter = DrinkCompactAdapter(::onClickDrink)
            recyclerStrict.adapter = strictAdapter
            recyclerAlcohol.adapter = alcoholAdapter

            CoroutineScope(Dispatchers.IO).launch {
                launch {
                    totalCabinetRepository.ownedIngredientFlow.collect{
                        rwLock.writeLock().lock()
                        owned = it.toTreemap()
                        rwLock.writeLock().unlock()
                        CoroutineScope(Dispatchers.Main).launch {
                            rwLock.readLock().lock()
                            newDrinksTextStrict.text = newDrinksFromEitherStrict(view)
                            newDrinksTextAlcohol.text = newDrinksFromEitherAlcoholic(view)
                            strictAdapter.submitItems(newDrinksWithThisStrict(shoppingCart.getItems().map { it.toCabinetProduct() }, shoppingCart.getMixers().map { it.mixer }))
                            alcoholAdapter.submitItems(newDrinksWithThisAlcoholic(shoppingCart.getItems().map { it.toCabinetProduct() }))
                            rwLock.readLock().lock()
                        }
                    }
                }
                totalDrinkRepository.dataFlow.collect{
                    rwLock.writeLock().lock()
                    drinks = it
                    rwLock.writeLock().unlock()
                    CoroutineScope(Dispatchers.Main).launch {
                        rwLock.readLock().lock()
                        newDrinksTextStrict.text = newDrinksFromEitherStrict(view)
                        newDrinksTextAlcohol.text = newDrinksFromEitherAlcoholic(view)
                        strictAdapter.submitItems(drinks.filter { it.canMakeStrict(owned) })
                        alcoholAdapter.submitItems(drinks.filter { it.canMakeAlcoholic(owned) })
                        rwLock.readLock().unlock()
                    }
                }
            }

            findViewById<Button>(R.id.buttonClose).setOnClickListener {
                window.dismiss()
            }
        }
    }

    private fun newDrinksFromEitherAlcoholic(v: View): String{
        return v.resources.getString(
            R.string.new_drinks_alcohol,
            newDrinksWithThisAlcoholic(shoppingCart.getItems().map { it.toCabinetProduct() }).size)
    }

    private fun newDrinksFromEitherStrict(v: View): String{
        return v.resources.getString(
            R.string.n_new_drinks,
            newDrinksWithThisStrict(shoppingCart.getItems().map { it.toCabinetProduct() },shoppingCart.getMixers().map { it.mixer }).size)
    }

    /** This function does not set id or owner id correctly so use only when a temporary [CabinetProduct] is needed
     * 
     */
    private fun ShoppingCartItem.toCabinetProduct(): CabinetProduct{
        return CabinetProduct(0, this.product, 0, null, true)
    }

    private fun newDrinksWithThisAlcoholic(newProducts: List<CabinetProduct>):List<DrinkTotal>{
        return totalDrinkRepository.makeableWithNewAlcoholic(owned, totalCabinetRepository.productsToIngredients(newProducts))
    }

    private fun newDrinksWithThisStrict(newProducts: List<CabinetProduct>, newMixers: List<GeneralIngredient>):List<DrinkTotal>{
        return totalDrinkRepository.makeableWithNewStrict(owned, totalCabinetRepository.productsToIngredients(newProducts) + newMixers)
    }
}