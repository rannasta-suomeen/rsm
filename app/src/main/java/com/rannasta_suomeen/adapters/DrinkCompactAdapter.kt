package com.rannasta_suomeen.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.DrinkTotal
import java.util.*

class DrinkCompactAdapter(private val callBack: (DrinkTotal) -> Unit): RecyclerView.Adapter<DrinkCompactAdapter.DrinkPreviewViewHolder>() {

    private var items: List<DrinkTotal> = listOf()

    class DrinkPreviewViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        fun bind(i: DrinkTotal, callBack: (DrinkTotal) -> Unit) {
            with(itemView){
                setOnClickListener { callBack(i) }
                findViewById<TextView>(R.id.textViewDrinkNameSmall).text = i.drink.name
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<DrinkTotal>){
        items = input
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkPreviewViewHolder {
        val layout = R.layout.item_drink_small
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return DrinkPreviewViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: DrinkPreviewViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, callBack)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}