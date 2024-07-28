package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Spinner
import androidx.core.view.GravityCompat
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.DrinkPreviewAdapter
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import com.rannasta_suomeen.totalDrinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DrinksFragment(val activity: Activity, private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository) : Fragment(R.layout.fragment_drinks), AdapterView.OnItemSelectedListener {

    private lateinit var drinkPreviewAdapter: DrinkPreviewAdapter
    private var drinkListFull = listOf<DrinkTotal>()
    private var drinkListFiltered = drinkListFull
    private var ownedIngredients = listOf<GeneralIngredient>()
    private lateinit var popupMenu: PopupMenu

    private var sortType = DrinkInfo.SortTypes.Pps
    private var sortByAsc = true

    override fun onCreate(savedInstanceState: Bundle?) {
        drinkPreviewAdapter = DrinkPreviewAdapter(activity, settings)
        super.onCreate(savedInstanceState)
        updateSelection()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val img  = when (sortByAsc){
                true -> R.drawable.ic_baseline_arrow_drop_up_24
                false -> R.drawable.ic_baseline_arrow_drop_down_24
            }
            sortByDirButton.setImageResource(img)
            updateSelection()
        }

        val filterButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkFilter)
        popupMenu = PopupMenu(filterButton.context,filterButton,GravityCompat.START)
        updatePopupMenu()

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                totalDrinkRepository.dataFlow.collect{
                    drinkListFull = it
                    updatePopupMenu()
                    drinkListFiltered = drinkListFull
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

        popupMenu.setOnMenuItemClickListener { m ->
            if (m.isCheckable){
                m.isChecked = !m.isChecked
            }
            val tList = drinkListFull.toMutableList()
            for (i in popupMenu.menu){
                if (!i.isChecked){
                    tList.removeAll { r ->
                        val d = r.drink
                        when (i.itemId){
                            R.id.menuDrinkFilterCocktail -> d.type == DrinkType.cocktail
                            R.id.menuDrinkFilterPunch -> d.type == DrinkType.punch
                            R.id.menuDrinkFilterShot -> d.type == DrinkType.shot
                            R.id.menuDrinkFilterMakeable -> r.missingIngredientsAlcoholic(ownedIngredients) != 0
                            R.id.menuDrinkFilterMakeableGrocery -> r.missingIngredientsNonAlcoholic(ownedIngredients) != 0
                            0 -> d.tag_list.contains(i.title)
                            else -> false
                        }
                    }
                }
            }
            drinkListFiltered = tList.toList()
            updateSelection()
            true
        }

        filterButton.setOnClickListener {
            popupMenu.show()
        }
    }

    private fun updateSelection(){
        drinkPreviewAdapter.submitItems(sortDrinkPreview(drinkListFiltered, sortType, sortByAsc, settings), ownedIngredients)
    }

    private fun updatePopupMenu(){
        popupMenu.menu.clear()
        popupMenu.inflate(R.menu.menu_drink_filter)
        addTagsToFilterMenu(popupMenu.menu, findAllTags(drinkListFull))
    }

    private fun addTagsToFilterMenu(x: Menu, tags: List<String>){
        tags.forEach {
            val t = x.add(it)
            t.isCheckable = true
            t.isChecked = true
        }
    }

    private fun findAllTags(drinksList: List<DrinkTotal>): List<String>{
        val tagMap = mutableSetOf<String>()
        drinksList.forEach {tagMap.addAll(it.drink.tag_list) }
        return tagMap.toList()
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
