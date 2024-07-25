package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.popup_windows.PopupDrink
import com.rannasta_suomeen.storage.Settings
import java.util.Locale

class DrinkPreviewAdapter(val activity: Activity, private val settings: Settings): RecyclerView.Adapter<DrinkPreviewAdapter.DrinkPreviewViewHolder>() {

    private var items: List<DrinkTotal> = listOf()
    private var owned: List<GeneralIngredient> = listOf()

    class DrinkPreviewViewHolder(itemView: View,val activity: Activity):RecyclerView.ViewHolder(itemView){
        fun bind(i: DrinkTotal, owned:List<GeneralIngredient>, settings: Settings){
            val item = i.drink
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewName).text = item.name
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewShots).text = displayDecimal(item.standard_servings, R.string.shots)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewPrice).text = displayDecimal(item.price(settings), R.string.price)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAbv).text = displayDecimal(item.abv_average, R.string.abv)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewVolume).text = UnitType.ml.displayInDesiredUnit(i.drink.total_volume, settings.prefUnit)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewTags).text = item.tag_list.fold(""){r, t ->
                if (t != ""){
                    return@fold "$r#$t"
                }
                ""
            }
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAer).text = displayDecimal(item.pricePerServing(settings), R.string.aer)
            itemView.findViewById<TextView>(R.id.textViewDrinkMissingAlcohol).text = i.missingIngredientsAlcoholic(owned).toString()
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewMissingGrocery).text = i.missingIngredientsNonAlcoholic(owned).toString()
            itemView.setOnClickListener {
                PopupDrink(i, activity, owned, settings).show(it)
            }
        }
        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format(Locale.UK,"%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<DrinkTotal>, o: List<GeneralIngredient>){
        items = input
        owned = o
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkPreviewViewHolder {
        val layout = R.layout.item_drink
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return DrinkPreviewViewHolder(itemView, activity)
    }

    override fun onBindViewHolder(holder: DrinkPreviewViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, owned, settings)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}