package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.DrinkRandomizerAdapter
import com.rannasta_suomeen.data_classes.CabinetMixer
import com.rannasta_suomeen.data_classes.CabinetProduct
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.RandomizerItem
import com.rannasta_suomeen.data_classes.RandomizerList
import com.rannasta_suomeen.data_classes.findAllTags
import com.rannasta_suomeen.data_classes.toTreemap
import com.rannasta_suomeen.popup_windows.BASE_TAGS
import com.rannasta_suomeen.popup_windows.PopupRandomizerInfo
import com.rannasta_suomeen.popup_windows.PopupRandomizerOptions
import com.rannasta_suomeen.popup_windows.RandomizerSettings
import com.rannasta_suomeen.storage.Randomizer
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.storage.TotalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TreeMap

class RandomizerFragment(
    private val activity: Activity,
    private val settings: Settings,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val totalDrinkRepository: TotalDrinkRepository,

    private val randomizer: Randomizer): Fragment(R.layout.fragment_randomizer) {
    private val callBack: (RandomizerItem) -> Unit = {
        randomizer.removeItem(it)
    }
    private val hiddenCallback: (RandomizerItem) -> Unit = {
        val b = AlertDialog.Builder(activity)
        b.setTitle(R.string.reveal_drinks)
        b.setPositiveButton(android.R.string.ok){_,_ ->
            randomizer.modifyItem(it){
                it.hidden = false
            }
            updateRecycler()
        }
        b.setNegativeButton(android.R.string.cancel){dialogInterface, _ ->
            dialogInterface.dismiss()
        }
        b.show()
    }
    private var allDrinks = listOf<DrinkTotal>()
    private var ownedProducts: List<CabinetProduct> = listOf()
    private var ownedMixers: List<CabinetMixer> = listOf()
    private var randomizerItems = listOf<RandomizerItem>()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var randomizerSettings =  RandomizerSettings(BASE_TAGS.map { activity.getString(it) }+allDrinks.findAllTags())
    val adapter = DrinkRandomizerAdapter(activity, settings, randomizer,hiddenCallback, callBack)

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
                    randomizerSettings = RandomizerSettings(BASE_TAGS.map { activity.getString(it) }+allDrinks.findAllTags())
                }
            }
            val fab = findViewById<FloatingActionButton>(R.id.fabAddDrinkToRandomizer)
            fab.setOnClickListener {
                val t: (RandomizerSettings, Int) -> Unit = {randomizerSettings, n ->
                    val t = randomizerSettings.generateDrink(allDrinks, combineOwned(), n)
                    when (t.isSuccess){
                        true -> t.getOrThrow().forEach { randomizer.addItem(RandomizerItem(it,randomizerSettings.hidden)) }
                        false -> Toast.makeText(activity, "Not possible to complete list with current settings", Toast.LENGTH_SHORT).show()
                    }
                }
                PopupRandomizerOptions(activity,allDrinks,randomizerSettings,view,t){
                    scope.launch {
                        randomizer.clear()
                    }
                }.show(this)
            }
            findViewById<ImageButton>(R.id.imageButtonRandomizerInfo).setOnClickListener {
                PopupRandomizerInfo(activity, RandomizerList(randomizerItems),settings,combineOwned()).show(it)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
    private fun combineOwned(): TreeMap<Int, GeneralIngredient>{
        return (ownedMixers.map { it.ingredient }+totalCabinetRepository.productsToIngredients(ownedProducts)).toTreemap()
    }

    private fun updateRecycler(){
        mainScope.launch {
            adapter.submitItems(randomizerItems, combineOwned())
        }
    }
}