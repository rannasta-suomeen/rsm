package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.DrinkPreviewAdapter
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.storage.Randomizer
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.absoluteValue

class RandomizerFragment(
    private val activity: Activity,
    settings: Settings,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val totalDrinkRepository: TotalDrinkRepository,
    private val randomizer: Randomizer): Fragment(R.layout.fragment_randomizer) {
    val adapter = DrinkPreviewAdapter(activity, settings, randomizer)
    private var allDrinks = listOf<DrinkTotal>()
    private var ownedProducts: List<CabinetProduct> = listOf()
    private var ownedMixers: List<CabinetMixer> = listOf()
    private var randomizerItems = listOf<RandomizerItem>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val random = Random()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerRandomizer)
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(activity.baseContext)
            scope.launch {
                launch {
                    randomizer.dataFlow.collect{
                        randomizerItems = it
                        updateRecycler()
                    }
                }
                launch {
                    totalCabinetRepository.selectedCabinetFlow.collect{
                        it?.let {
                            ownedProducts = it.products
                            ownedMixers = it.mixers
                        }
                        updateRecycler()
                    }
                }
                totalDrinkRepository.dataFlow.collect{
                    allDrinks = it
                }
            }
            val fab = findViewById<FloatingActionButton>(R.id.fabAddDrinkToRandomizer)
            fab.setOnClickListener {
                randomizer.addItem(generateRandomDrink())
                adapter.submitItems(randomizer.getItems().map { it.drinkTotal },combineOwned())
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
    private fun combineOwned(): TreeMap<Int, GeneralIngredient>{
        return (ownedMixers.map { it.ingredient }+totalCabinetRepository.productsToIngredients(ownedProducts)).toTreemap()
    }

    private fun updateRecycler(){
        mainScope.launch {
            adapter.submitItems(randomizerItems.map{it.drinkTotal}, combineOwned())
        }
    }

    private fun generateRandomDrink(): RandomizerItem{
        val drink = allDrinks[random.nextInt().absoluteValue % allDrinks.size]
        return RandomizerItem(drink, true)
    }
}