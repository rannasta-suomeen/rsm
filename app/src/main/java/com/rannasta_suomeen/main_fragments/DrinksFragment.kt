package com.rannasta_suomeen.main_fragments

import android.os.Bundle
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
import com.rannasta_suomeen.data_classes.DrinkPreview
import com.rannasta_suomeen.data_classes.DrinkType
import com.rannasta_suomeen.data_classes.SortDrinkPreview

class DrinksFragment : Fragment(R.layout.fragment_drinks), AdapterView.OnItemSelectedListener {

    private val drinkPreviewAdapter = DrinkPreviewAdapter()
    private val drinkListFull = listOf<DrinkPreview>(
        DrinkPreview("Pina Colada", DrinkType.Cocktail, 20,7.5,1.155),
        DrinkPreview("Kelkka", DrinkType.Cocktail, 18,14.888,1.573),
        DrinkPreview("I miss her -shot", DrinkType.Shot, 4,65.375,0.988)
    ).sortedBy { it.abv }

    private lateinit var popupMenu: PopupMenu

    private var drinkListFiltered = drinkListFull

    private var sortType = DrinkPreview.SortTypes.Aer
    private var sortByAsc = false

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val popupMenu = PopupMenu(filterButton.context,filterButton,GravityCompat.START)
        popupMenu.inflate(R.menu.menu_drink_filter)
        filterButton.setOnClickListener {
            popupMenu.setOnMenuItemClickListener { m ->
                if (m.isCheckable){
                    m.isChecked = !m.isChecked
                }
                val tList = drinkListFull.toMutableList()
                for (i in popupMenu.menu){
                    if (!i.isChecked){
                        tList.removeAll { d ->
                            when (i.itemId){
                                R.id.menuDrinkFilterCocktail -> d.type == DrinkType.Cocktail
                                R.id.menuDrinkFilterPunch -> d.type == DrinkType.Punch
                                R.id.menuDrinkFilterShot -> d.type == DrinkType.Shot
                                // TODO implement this
                                R.id.menuDrinkFilterMakeable -> false
                                else -> false
                            }
                        }
                    }
                }
                drinkListFiltered = tList.toList()
                updateSelection()
                true
            }
            popupMenu.show()
        }
    }

    private fun updateSelection(){
        drinkPreviewAdapter.submitItems(SortDrinkPreview(drinkListFiltered, sortType, sortByAsc))
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
         sortType = when (p0!!.getItemAtPosition(p2)){
             "Abv" ->  DrinkPreview.SortTypes.Abv
             "Volume" -> DrinkPreview.SortTypes.Volume
             "Aer" ->  DrinkPreview.SortTypes.Aer
             "Fsd" -> DrinkPreview.SortTypes.Fsd
             "Price" -> DrinkPreview.SortTypes.Price
             "Name" -> DrinkPreview.SortTypes.Name
            else -> {throw IllegalArgumentException("Unknown thing to sort by")}
        }
        updateSelection()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        p0!!.setSelection(0)
    }

}
