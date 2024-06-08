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
import com.rannasta_suomeen.storage.DrinkRepository
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.DrinkInfo
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.DrinkType
import com.rannasta_suomeen.data_classes.sortDrinkPreview
import com.rannasta_suomeen.storage.TotalDrinkRepository
import kotlinx.coroutines.*

class DrinksFragment : Fragment(R.layout.fragment_drinks), AdapterView.OnItemSelectedListener {

    private lateinit var drinkPreviewAdapter: DrinkPreviewAdapter
    private lateinit var drinkRepository: TotalDrinkRepository
    private var drinkListFull = listOf<DrinkTotal>()
    private var drinkListFiltered = drinkListFull

    private var sortType = DrinkInfo.SortTypes.Pps
    private var sortByAsc = false

    override fun onCreate(savedInstanceState: Bundle?) {
        drinkPreviewAdapter = DrinkPreviewAdapter(requireActivity())
        super.onCreate(savedInstanceState)
        drinkRepository = TotalDrinkRepository(requireContext())
        updateSelection()

        CoroutineScope(Dispatchers.IO).launch {
            drinkRepository.dataFlow.collect{
                drinkListFull = it
                drinkListFiltered = drinkListFull
                requireActivity().runOnUiThread { updateSelection() }
            }
        }
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
                        tList.removeAll { r ->
                            val d = r.drink
                            when (i.itemId){
                                R.id.menuDrinkFilterCocktail -> d.type == DrinkType.cocktail
                                R.id.menuDrinkFilterPunch -> d.type == DrinkType.punch
                                R.id.menuDrinkFilterShot -> d.type == DrinkType.shot
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
        drinkPreviewAdapter.submitItems(sortDrinkPreview(drinkListFiltered, sortType, sortByAsc))
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
