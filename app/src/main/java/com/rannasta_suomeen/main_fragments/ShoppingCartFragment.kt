package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.ShoppingMixerAdapter
import com.rannasta_suomeen.adapters.ShoppingProductAdapter
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.toTreemap
import com.rannasta_suomeen.main_fragments.shopping_cart_fragments.ShoppingFragmentFactory
import com.rannasta_suomeen.popup_windows.PopupMixer
import com.rannasta_suomeen.popup_windows.PopupShoppingCartInfo
import com.rannasta_suomeen.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingCartFragment(
    private val activity: Activity,
    private val shoppingCart: ShoppingCart,
    imageRepository: ImageRepository,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val totalDrinkRepository: TotalDrinkRepository,
    private val settings: Settings,
    private val randomizer: Randomizer)
    : Fragment(R.layout.fragment_shopping_cart){
    private val shoppingProductAdapter = ShoppingProductAdapter(activity,imageRepository, totalCabinetRepository, settings, shoppingCart)
    private lateinit var shoppingMixerAdapter : ShoppingMixerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        fun openPopupFun(m: GeneralIngredient){
            PopupMixer(activity, m, totalDrinkRepository, totalCabinetRepository, settings, view, shoppingCart, randomizer).show(view)
        }
        shoppingMixerAdapter = ShoppingMixerAdapter(::openPopupFun,totalCabinetRepository,shoppingCart, settings, totalDrinkRepository.totalDrinkList)
        with(view){
            val buttonInfo = findViewById<ImageButton>(R.id.imageButtonCartInfo)
            val tabs = findViewById<TabLayout>(R.id.tabsCart)
            childFragmentManager.fragmentFactory = ShoppingFragmentFactory(shoppingProductAdapter, shoppingMixerAdapter)
            val navHostFragment = childFragmentManager.findFragmentById(R.id.navHostCart) as NavHostFragment
            val navController = navHostFragment.findNavController()
            navController.setGraph(R.navigation.nav_cart)
            tabs.addOnTabSelectedListener(TabListener(navController))
            buttonInfo.setOnClickListener {
                PopupShoppingCartInfo(view,activity,shoppingCart, settings, totalCabinetRepository, totalDrinkRepository, randomizer).show(buttonInfo)
            }
            shoppingMixerAdapter.submitItems(shoppingCart.getMixers())
        }
        CoroutineScope(Dispatchers.IO).launch {
            launch {
                totalCabinetRepository.selectedCabinetFlow.collect {
                    it?.let { cabinet ->
                        shoppingMixerAdapter.submitNewAlcohol(
                            totalCabinetRepository.productsToIngredients(
                                cabinet.products
                            ).toTreemap()
                        )
                        shoppingMixerAdapter.submitNewOwned(cabinet.mixers.toTreemap())
                    }
                }
            }
            launch {
                totalDrinkRepository.dataFlow.collect{
                    shoppingMixerAdapter.submitNewDrinks(it)
                }
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }

    private class TabListener(private val navController: NavController) : TabLayout.OnTabSelectedListener{
        override fun onTabSelected(tab: TabLayout.Tab?) {
            val target = when (tab?.text){
                "Alcoholic" -> R.id.fragmentCartProducts
                "Mixers" -> R.id.fragmentCartMixers
                else -> {
                    Log.d("Cart", "Selected tab with text ${tab?.text}")
                    R.id.fragmentCartProducts
                }
            }
            navController.navigate(target)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabReselected(tab: TabLayout.Tab?) {}

    }
}