package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.rannasta_suomeen.ProductAdapter
import com.rannasta_suomeen.ProductAdapterItemTouchHelper
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Product
import com.rannasta_suomeen.data_classes.Product.SortTypes
import com.rannasta_suomeen.data_classes.sort
import com.rannasta_suomeen.popup_windows.PopupFilter
import com.rannasta_suomeen.productRepository
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductsFragment(private val activity: Activity, private val imageRepository: ImageRepository,private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository) : Fragment(R.layout.fragment_products), AdapterView.OnItemSelectedListener {

    private lateinit var productAdapter: ProductAdapter
    private var productListFull = listOf<Product>().sortedBy {it.name}

    private var sortType = SortTypes.Pps
    private var sortByAsc = true

    private lateinit var filterMenu: PopupFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productAdapter = ProductAdapter(activity, totalCabinetRepository, imageRepository, settings)
        filterMenu = PopupFilter(activity, ::updateSelection)

        CoroutineScope(Dispatchers.IO).launch {
            launch {
                productRepository.dataFlow.collect{
                    productListFull = it
                    activity.runOnUiThread{
                        updateSelection()
                    }
                }
            }
            totalCabinetRepository.selectedCabinetFlow.collect{
                activity.runOnUiThread {
                    updateSelection()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val helper = ProductAdapterItemTouchHelper(productAdapter, MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.GREEN), requireContext())
        val recyclerViewDrinks = view.findViewById<RecyclerView>(R.id.recyclerViewPreviewDrinks)
        helper.attachToRecyclerView(recyclerViewDrinks)
        recyclerViewDrinks.layoutManager = LinearLayoutManager(this.context)
        recyclerViewDrinks.adapter = productAdapter
        recyclerViewDrinks.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        val spinner = view.findViewById<Spinner>(R.id.spinnerDrinkSort)
        spinner.onItemSelectedListener = this
        val sortByDirButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkSortDir)
        val filterButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkFilter)
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

        filterButton.setOnClickListener {
            filterMenu.show(filterButton)
        }
    }

    private fun updateSelection(){
        productAdapter.submitItems(sort(filterMenu.filter(productListFull), sortType, sortByAsc))
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
         sortType = when (p0!!.getItemAtPosition(p2)){
             "Abv" ->  SortTypes.Abv
             "Volume" -> SortTypes.Volume
             "Pps" ->  SortTypes.Pps
             "Fsd" -> SortTypes.Fsd
             "Price" -> SortTypes.Price
             "Name" -> SortTypes.Name
            else -> {throw IllegalArgumentException("Unknown thing to sort by")}
        }
        updateSelection()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        p0!!.setSelection(0)
    }

}
