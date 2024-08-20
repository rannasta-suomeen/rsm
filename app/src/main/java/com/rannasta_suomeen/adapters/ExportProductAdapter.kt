package com.rannasta_suomeen.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.CabinetProduct

class ExportProductAdapter(private val onItemClickFun: (CabinetProduct) -> Unit): RecyclerView.Adapter<ExportProductAdapter.ExportProductViewHolder>() {
    var list: MutableList<CabinetProduct> = mutableListOf()
    class ExportProductViewHolder(itemView: View,val notifyClickFun: (CabinetProduct) -> Unit): RecyclerView.ViewHolder(itemView){
        fun bind(item: CabinetProduct){
            with(itemView){
                findViewById<TextView>(R.id.textViewExportName).text = item.product.name
                findViewById<ImageView>(R.id.buttonExportSwap).setOnClickListener {
                    notifyClickFun(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportProductViewHolder {
        val layout = R.layout.item_product_export
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ExportProductViewHolder(itemView, onItemClickFun)
    }

    override fun onBindViewHolder(holder: ExportProductViewHolder, position: Int) {
        holder.bind(list[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(x: List<CabinetProduct>){
        list = x.toMutableList()
        notifyDataSetChanged()
    }

    fun deleteItem(x: CabinetProduct){
        val index = list.indexOf(x)
        if (index != -1) {
            list.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addItem(x: CabinetProduct){
        list.add(x)
        notifyItemInserted(list.size-1)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}