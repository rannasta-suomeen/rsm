package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.DrinkPreview

class DrinkPreviewAdapter: RecyclerView.Adapter<DrinkPreviewAdapter.DrinkPreviewViewHolder>() {

    private var items: List<DrinkPreview> = listOf()

    class DrinkPreviewViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        fun bind(item: DrinkPreview){
            Log.d("Debug", "Bound $item")
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewName).text = item.name
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewShots).text = displayDecimal(item.fsd(), R.string.shots)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewPrice).text = displayDecimal(item.price, R.string.price)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAbv).text = displayDecimal(item.abv, R.string.abv)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewVolume).text = itemView.resources.getString(R.string.volume, item.volume)
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewType).text = item.type.toString()
            itemView.findViewById<TextView>(R.id.textViewDrinkPreviewAer).text = displayDecimal(item.aer(), R.string.aer)
        }
        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<DrinkPreview>){
        items = input
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkPreviewViewHolder {
        val layout = R.layout.item_drink
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return DrinkPreviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DrinkPreviewViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}