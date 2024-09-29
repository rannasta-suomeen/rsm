package com.rannasta_suomeen.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.CabinetMixer
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.popup_windows.normalize
import com.rannasta_suomeen.storage.Settings
import java.util.*
import kotlin.reflect.KFunction3

class MixerAdapter(
    private val settings: Settings,
    private var drinkList: List<DrinkTotal>,
    private val onTouchCallBack: (GeneralIngredient) -> Unit,
    private val onLongTouchCallback: (GeneralIngredient) -> Unit): RecyclerView.Adapter<MixerAdapter.ViewHolder>()  {

    private var owned: TreeMap<Int, CabinetMixer> = TreeMap()
    private var items: List<GeneralIngredient> = listOf()
    private var filter: String = ""
    private var fullItems: List<GeneralIngredient> = listOf()
    private var ownedAlcohol: TreeMap<Int, GeneralIngredient> = TreeMap()

    class ViewHolder(itemView: View,private val settings: Settings): RecyclerView.ViewHolder(itemView){
        fun bind(
            item: GeneralIngredient,
            owned: TreeMap<Int,CabinetMixer>,
            drinkList: List<DrinkTotal>,
            ownedAlcohol: TreeMap<Int, GeneralIngredient>,
            onTouchCallBack: (GeneralIngredient) -> Unit,
            onLongTouchCallBack: (GeneralIngredient) -> Unit){
            with(itemView){
                setOnClickListener { onTouchCallBack(item) }
                setOnLongClickListener {
                    onLongTouchCallBack(item)
                true
                }
                findViewById<TextView>(R.id.textViewMixerName).text = item.name
                findViewById<TextView>(R.id.textViewMixerPrice).text = displayDecimal(item.price(settings), R.string.ppl)
                val now = findViewById<TextView>(R.id.textViewMixerNewRecipesNow)
                val total = findViewById<TextView>(R.id.textViewMixerNewRecipesTotal)
                val used = findViewById<TextView>(R.id.textViewMixerUsedInTotal)
                val ownedAmount = findViewById<TextView>(R.id.textViewMixerOwned)
                val image = findViewById<ImageView>(R.id.imageViewMixerOwned)
                when (item.isOwned(owned)){
                    true -> {
                        now.visibility = View.INVISIBLE
                        total.visibility = View.INVISIBLE
                        used.visibility = View.INVISIBLE
                        image.visibility = View.VISIBLE
                        ownedAmount.visibility = View.VISIBLE
                        ownedAmount.text = item.showAmount(owned, settings)
                    }
                    false -> {
                        val ownedMap = (owned.mapValues { it.value.ingredient }.toSortedMap() + ownedAlcohol).toSortedMap() as TreeMap<Int, GeneralIngredient>

                        fun fast(fn: KFunction3<DrinkTotal, TreeMap<Int, GeneralIngredient>, GeneralIngredient, Boolean>): List<DrinkTotal>{
                            return drinkList.filter{fn(it, ownedMap, item)}
                        }
                        val nowList = fast(DrinkTotal::isMissingOnly)
                        val totalList = fast(DrinkTotal::isMissingButHasAlcoholic).filter { !nowList.contains(it)}
                        val usedList = drinkList.filter { it.contains(item) }.filter { !nowList.contains(it) && !totalList.contains(it)}
                        now.visibility = View.VISIBLE
                        total.visibility = View.VISIBLE
                        used.visibility = View.VISIBLE
                        ownedAmount.visibility = View.INVISIBLE
                        image.visibility = View.INVISIBLE
                        now.text = resources.getString(R.string.new_now,nowList.count())
                        total.text = resources.getString(R.string.new_total,totalList.count())
                        used.text = resources.getString(R.string.used_total, usedList.count())
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = R.layout.item_mixer
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(itemView, settings)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], owned, drinkList, ownedAlcohol, onTouchCallBack, onLongTouchCallback)
    }

    fun submitItems(l: List<GeneralIngredient>){
        if (l == items) return
        fullItems = l

        reSort()
    }

    fun submitNewOwned(l: TreeMap<Int, CabinetMixer>){
        if (l == owned) return
        owned = l
        reSort()
    }

    fun submitNewDrinks(l: List<DrinkTotal>){
        if (l == drinkList) return
        drinkList = l
        reSort()
    }

    fun submitNewAlcohol(l:TreeMap<Int, GeneralIngredient>){
        if (l == ownedAlcohol) return
        ownedAlcohol = l
        reSort()
    }

    fun submitNewSearch(s: String){
        filter = s
        reSort()
    }

    @Suppress("NotifyDataSetChanged")
    private fun reSort(){
        val ownedMap = owned.filter { it.value.usable }.mapValues { it.value.ingredient }.toSortedMap() as TreeMap<Int, GeneralIngredient>
        ownedMap += ownedAlcohol
        val t = fullItems.filter { it.name.normalize().contains(filter.normalize()) }.sortedBy {d ->
            drinkList.count {it.isMissing(ownedMap, d)}
        }.sortedBy {d-> drinkList.count{it.isMissingOnly(ownedMap, d)} }.reversed()
        items = t
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }
}