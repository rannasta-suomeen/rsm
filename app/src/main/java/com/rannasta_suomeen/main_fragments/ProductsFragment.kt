package com.rannasta_suomeen.main_fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.PopupFilter
import com.rannasta_suomeen.ProductAdapter
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.data_classes.Product.SortTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class ProductsFragment : Fragment(R.layout.fragment_products), AdapterView.OnItemSelectedListener {

    private val productAdapter = ProductAdapter()
    private var productListFull = listOf<Product>().sortedBy {it.name}

    private var sortType = SortTypes.Pps
    private var sortByAsc = false

    private lateinit var filterMenu: PopupFilter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val res = CoroutineScope(Dispatchers.IO).async {
            val res = NetworkController.tryNTimes(5, Pair(100000, 0), NetworkController::getProducts)
            productListFull = res.getOrNull()?: listOf()
            Log.d("Products", "Got ${productListFull.size} products")
            requireActivity().runOnUiThread{
                updateSelection()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val recyclerViewDrinks = view.findViewById<RecyclerView>(R.id.recyclerViewPreviewDrinks)
        recyclerViewDrinks.layoutManager = LinearLayoutManager(this.context)
        recyclerViewDrinks.adapter = productAdapter
        recyclerViewDrinks.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))

        val spinner = view.findViewById<Spinner>(R.id.spinnerDrinkSort)
        spinner.onItemSelectedListener = this
        val filterButton = view.findViewById<ImageButton>(R.id.imageButtonDrinkFilter)

        filterMenu = PopupFilter(requireActivity(), filterButton, ::updateSelection)

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

        filterButton.setOnClickListener {
            filterMenu.show()
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
