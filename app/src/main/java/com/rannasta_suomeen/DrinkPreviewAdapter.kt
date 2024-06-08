package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.DrinkInfo
import com.rannasta_suomeen.data_classes.DrinkTotal
import com.rannasta_suomeen.popup_windows.PopupDrink

class DrinkPreviewAdapter(val activity: Activity): RecyclerView.Adapter<DrinkPreviewAdapter.DrinkPreviewViewHolder>() {

    private var items: List<DrinkTotal> = listOf()

    class DrinkPreviewViewHolder(itemView: View,val  activity: Activity):RecyclerView.ViewHolder(itemView){
        fun bind(i: DrinkTotal){
            val item = i.drink
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewName).text = item.name
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewShots).text = displayDecimal(item.standard_servings, R.string.shots)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewPrice).text = displayDecimal(item.price(), R.string.price)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAbv).text = displayDecimal(item.abv_average, R.string.abv)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewVolume).text = itemView.resources.getString(R.string.volume, item.total_volume/10)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewType).text = item.type.toString()
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAer).text = displayDecimal(item.pricePerServing(), R.string.aer)
            itemView.setOnClickListener {
                PopupDrink(i, activity).show(it)
            }
        }
        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<DrinkTotal>){
        items = input
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkPreviewViewHolder {
        val layout = R.layout.item_drink
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return DrinkPreviewViewHolder(itemView, activity)
    }

    override fun onBindViewHolder(holder: DrinkPreviewViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}