package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.DrinkPreviewAdapter
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.popup_windows.PopupDrinkFilter
import com.rannasta_suomeen.storage.Randomizer
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.totalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DrinksFragment(val activity: Activity, private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository, private val randomizer: Randomizer) : Fragment(R.layout.fragment_drinks), AdapterView.OnItemSelectedListener {

    private lateinit var drinkPreviewAdapter: DrinkPreviewAdapter
    private var drinkListFull = listOf<DrinkTotal>()
    private var ownedIngredients = listOf<GeneralIngredient>()
    private lateinit var filterMenu: PopupDrinkFilter

    private var sortType = DrinkInfo.SortTypes.Pps
    private var sortByAsc = true

    override fun onCreate(savedInstanceState: Bundle?) {
        drinkPreviewAdapter = DrinkPreviewAdapter(activity, settings, randomizer)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filterMenu = PopupDrinkFilter(activity, ::updateSelection,drinkListFull,settings, requireView())
        updateSelection()

        val recyclerViewDrinks = view.findViewById<RecyclerView>(R.id.recyclerViewPreviewDrinks)
        recyclerViewDrinks.layoutManager = LinearLayoutManager(this.context)
        recyclerViewDrinks.adapter = drinkPreviewAdapter
        recyclerViewDrinks.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        val spinner = view.findViewById<Spinner>(R.id.spinnerDrinkSort)
        spinner.onItemSelectedListener = this

        val sortByDirButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkSortDir)
        val img  = when (sortByAsc){
            true -> R.drawable.ic_baseline_arrow_drop_up_24
            false -> R.drawable.ic_baseline_arrow_drop_down_24
        }
        sortByDirButton.setImageResource(img)
        sortByDirButton.setOnClickListener {
            sortByAsc = !sortByAsc
            val imgDynamic  = when (sortByAsc){
                true -> R.drawable.ic_baseline_arrow_drop_up_24
                false -> R.drawable.ic_baseline_arrow_drop_down_24
            }
            sortByDirButton.setImageResource(imgDynamic)
            updateSelection()
        }

        val filterButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkFilter)

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                totalDrinkRepository.dataFlow.collect{
                    drinkListFull = it
                    filterMenu = PopupDrinkFilter(activity, ::updateSelection,drinkListFull,settings, view)
                    drinkListFull
                    activity.runOnUiThread { updateSelection() }
                }
            }
            totalCabinetRepository.ownedIngredientFlow.collect{
                ownedIngredients = it
                activity.runOnUiThread {
                    updateSelection()
                }
            }
        }

        filterButton.setOnClickListener {
            filterMenu.show(filterButton)
        }
    }

    private fun updateSelection(){
        val map = ownedIngredients.toTreemap()
        drinkPreviewAdapter.submitItems(sortDrinkPreview(filterMenu.filter(drinkListFull, ownedIngredients), sortType, sortByAsc, settings), map)
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
         sortType = when (p0!!.getItemAtPosition(p2)){
             "Abv" ->  DrinkInfo.SortTypes.Abv
             "Volume" -> DrinkInfo.SortTypes.Volume
             "Pps" ->  DrinkInfo.SortTypes.Pps
             "Fsd" -> DrinkInfo.SortTypes.Fsd
             "Price" -> DrinkInfo.SortTypes.Price
             "Name" -> DrinkInfo.SortTypes.Name
            else -> {throw IllegalArgumentException("Unknown thing to sort by")}
        }
        updateSelection()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        p0!!.setSelection(0)
    }

}
