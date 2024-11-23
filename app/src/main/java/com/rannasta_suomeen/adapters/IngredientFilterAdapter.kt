package com.rannasta_suomeen.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.popup_windows.normalize
import kotlinx.coroutines.*
import java.util.*

enum class FilterStatus{
    Include,
    Neutral,
    Exclude,
}

typealias FilterMap = TreeMap<Int, FilterStatus>

fun FilterMap.checkDrinkAllowed(d: DrinkTotal): Boolean{
    val ingredientList = d.ingredients.recipeParts.map { it.ingredient }
    return ingredientList.all {
        this[it.id] != FilterStatus.Exclude
    } && this.filter { it.value == FilterStatus.Include }.all { ingredientList.map { it.id }.contains(it.key) }
}

class IngredientFilterAdapter(initialStatus: FilterMap):RecyclerView.Adapter<IngredientFilterAdapter.ViewHolder>() {

    private var items:List<GeneralIngredient> = listOf()
    private var itemsTotal: List<GeneralIngredient> = listOf()
    private var search: String = ""
    private var filterMap: FilterMap = initialStatus.clone() as FilterMap
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    class ViewHolder(view: View): RecyclerView.ViewHolder(view){
         fun bind(item: GeneralIngredient, filterMap: FilterMap, callBack: (FilterStatus, GeneralIngredient) -> Unit){
             fun qf(x: FilterStatus) = callBack(x, item)
             val status = filterMap[item.id]?:FilterStatus.Neutral
             with(itemView){
                 findViewById<TextView>(R.id.textViewIngredientFilterName).text = item.name
                val btnInclude = findViewById<RadioButton>(R.id.radioButtonIngredientInclude)
                val btnNeutral = findViewById<RadioButton>(R.id.radioButtonIngredientNeutral)
                val btnExclude = findViewById<RadioButton>(R.id.radioButtonIngredientExclude)
                btnInclude.setOnClickListener {
                    qf(FilterStatus.Include)
                }
                btnNeutral.setOnClickListener {
                    qf(FilterStatus.Neutral)
                }
                btnExclude.setOnClickListener {
                    qf(FilterStatus.Exclude)
                }
                 when(status){
                     FilterStatus.Include -> btnInclude.isChecked = true
                     FilterStatus.Neutral -> btnNeutral.isChecked = true
                     FilterStatus.Exclude -> btnExclude.isChecked = true
                 }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = R.layout.item_ingredient_filter
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], filterMap){filterStatus, generalIngredient ->
            filterMap[generalIngredient.id] = filterStatus
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitItems(x: List<GeneralIngredient>){
        if (itemsTotal != x){
            itemsTotal = x
            calcNewItems()
        }
    }

    fun submitNewSearch(input: String){
        search = input
        calcNewItems()
    }

    fun includeAll(){
        applyToFilterMap(FilterStatus.Include)
    }

    fun excludeAll(){
        applyToFilterMap(FilterStatus.Exclude)
    }

    fun neutralAll(){
        applyToFilterMap(FilterStatus.Neutral)
    }

    private fun applyToFilterMap(x: FilterStatus){
        filterMap.clear()
        items.forEach {
            filterMap[it.id] = x
        }
        calcNewItems()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun calcNewItems(){
        job?.cancel()
        job = scope.launch {
            val t = search.normalize()
            items = itemsTotal.filter { it.name.normalize().contains(t) }
            mainScope.launch {
                notifyDataSetChanged()
            }
        }
    }

    fun getStatus(): FilterMap{
        return filterMap
    }
}