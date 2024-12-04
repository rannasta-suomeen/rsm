package com.rannasta_suomeen.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.RandomizerItem
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.popup_windows.PopupDrink
import com.rannasta_suomeen.storage.Randomizer
import com.rannasta_suomeen.storage.Settings
import java.util.*

class DrinkRandomizerAdapter(val activity: Activity, private val settings: Settings, private val randomizer: Randomizer,private val hiddenItemClickCallback: (RandomizerItem) -> Unit, private val onLongClickCallback: (RandomizerItem) -> Unit): RecyclerView.Adapter<DrinkRandomizerAdapter.DrinkPreviewViewHolder>() {

    private var items: MutableList<RandomizerItem> = mutableListOf()
    private var owned: TreeMap<Int,GeneralIngredient> = TreeMap()

    class DrinkPreviewViewHolder(itemView: View,val activity: Activity):RecyclerView.ViewHolder(itemView){
        // TODO: sometimes drinks with multiline names fail to render the second line
        fun bindVisible(rItem: RandomizerItem, owned:TreeMap<Int,GeneralIngredient>, randomizer: Randomizer, settings: Settings,onLongClickCallback: (RandomizerItem) -> Unit){
            val i = rItem.drinkTotal
            val item = i.drink
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewName).text = item.name
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewShots).text = displayDecimal(
                item.standardServings,
                R.string.shots
            )
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewPrice).text = displayDecimal(
                item.price(settings),
                R.string.price
            )
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAbv).text = displayDecimal(
                item.abvAvg,
                R.string.abv
            )
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewVolume).text =
                UnitType.Ml.displayInDesiredUnit(i.drink.volume, settings.prefUnit)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewTags).text =
                item.displayTagList()
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAer).text = displayDecimal(item.pricePerServing(settings),
                R.string.aer
            )
            itemView.findViewById<TextView>(R.id.textViewDrinkMissingAlcohol).text = i.amountOfMissingIngredientsAlcoholic(owned).toString()
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewMissingGrocery).text = i.amountOfMissingIngredientsNonAlcoholic(owned).toString()
            itemView.setOnClickListener {
                PopupDrink(i, activity, owned, randomizer,settings).show(it)
            }
            itemView.setOnLongClickListener {
                onLongClickCallback(rItem)
                true
            }
        }
        fun bindHidden(rItem: RandomizerItem, hiddenItemClickCallback: (RandomizerItem) -> Unit, onLongClickCallback: (RandomizerItem) -> Unit){
            itemView.setOnLongClickListener {
                onLongClickCallback(rItem)
                true
            }
            itemView.setOnClickListener {
                hiddenItemClickCallback(rItem)
            }
        }
        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format(Locale.UK,"%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<RandomizerItem>, o: TreeMap<Int,GeneralIngredient>){
        items = input.toMutableList()
        owned = o
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkPreviewViewHolder {
        val layout = when (viewType){
            TYPE_HIDDEN -> R.layout.item_hidden
            TYPE_VISIBLE -> R.layout.item_drink
            else -> throw IllegalStateException("Illegal number")
        }
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return DrinkPreviewViewHolder(itemView, activity)
    }

    override fun onBindViewHolder(holder: DrinkPreviewViewHolder, position: Int) {
        val item = items[position]
        when (item.hidden){
            true -> holder.bindHidden(item, hiddenItemClickCallback,onLongClickCallback)
            false -> holder.bindVisible(item, owned,randomizer, settings, onLongClickCallback)
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position].hidden){
            true -> TYPE_HIDDEN
            false -> TYPE_VISIBLE
        }
    }

    companion object{
        private const val TYPE_HIDDEN = 0
        private const val TYPE_VISIBLE = 1
    }
}