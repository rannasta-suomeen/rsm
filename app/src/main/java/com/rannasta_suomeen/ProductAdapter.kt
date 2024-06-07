package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.Product
import com.rannasta_suomeen.data_classes.from
import com.rannasta_suomeen.storage.ImageRepository
import kotlinx.coroutines.*

class ProductAdapter(context: Context): RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var items: List<Product> = listOf()
    private val imageRepository = ImageRepository(context)

    class ProductViewHolder(itemView: View, private val imageRepository: ImageRepository):RecyclerView.ViewHolder(itemView){
        fun bind(item: Product){
            itemView.findViewById<TextView>(R.id.textViewProductName).text = item.name
            itemView.findViewById<TextView>(R.id.textViewProductPrice).text = displayDecimal(item.price, R.string.price)
            itemView.findViewById<TextView>(R.id.textViewProductVolume).text = itemView.resources.getString(R.string.volume,item.volumeCl())
            itemView.findViewById<TextView>(R.id.textViewProductAbv).text = displayDecimal(item.abv, R.string.abv)
            itemView.findViewById<TextView>(R.id.textViewRetailer).text = item.retailer.toString()
            itemView.findViewById<TextView>(R.id.textViewProductFsd).text = displayDecimal(item.fsd(), R.string.shots)
            itemView.findViewById<TextView>(R.id.textViewProductPpl).text = displayDecimal(item.unit_price, R.string.ppl)
            itemView.findViewById<TextView>(R.id.textViewProductPps).text = displayDecimal(item.pps(), R.string.aer)
            itemView.findViewById<TextView>(R.id.textViewProductSubcategory).text = from(item.subcategory_id).toString()
            itemView.findViewById<ImageView>(R.id.imageViewProduct).setImageResource(R.drawable.ic_baseline_wine_bar_24)
            CoroutineScope(Dispatchers.IO).launch {
                val img = imageRepository.getImage(item.img)
                CoroutineScope(Dispatchers.Main).launch {
                    itemView.findViewById<ImageView>(R.id.imageViewProduct).setImageBitmap(img)
                }
            }
        }

        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<Product>){
        items = input
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layout = R.layout.item_product
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ProductViewHolder(itemView, imageRepository)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}