package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.Product
import com.rannasta_suomeen.data_classes.from
import com.rannasta_suomeen.popup_windows.PopupProductAdd
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProductAdapter(
    private val activity: Activity,
    private val totalCabinetRepository: TotalCabinetRepository, private val imageRepository: ImageRepository, private val settings: Settings): RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var items: List<Product> = listOf()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            imageRepository.updateFlow.collect{
                submitImageFound(it)
            }
        }
    }

    class ProductViewHolder(
        itemView: View,
        private val imageRepository: ImageRepository,
        private val totalCabinetRepository: TotalCabinetRepository,
        private var activity: Activity,
        private val settings: Settings):RecyclerView.ViewHolder(itemView){
        fun bind(item: Product){
            with(itemView){
                findViewById<TextView>(R.id.textViewProductName).text = item.name
                findViewById<TextView>(R.id.textViewProductPrice).text = displayDecimal(item.price, R.string.price)
                findViewById<TextView>(R.id.textViewProductVolume).text = itemView.resources.getString(R.string.volume,item.volumeCl())
                findViewById<TextView>(R.id.textViewProductAbv).text = displayDecimal(item.abv, R.string.abv)
                findViewById<TextView>(R.id.textViewRetailer).text = item.retailer.toString()
                findViewById<TextView>(R.id.textViewProductFsd).text = displayDecimal(item.fsd(), R.string.shots)
                findViewById<TextView>(R.id.textViewProductPpl).text = displayDecimal(item.unit_price, R.string.ppl)
                findViewById<TextView>(R.id.textViewProductPps).text = displayDecimal(item.pps(), R.string.aer)
                findViewById<TextView>(R.id.textViewProductSubcategory).text = from(item.subcategory_id).toString()
                findViewById<ImageView>(R.id.imageViewProduct).setImageResource(R.drawable.ic_baseline_wine_bar_24)
                findViewById<ImageView>(R.id.imageViewProduct).setImageBitmap(imageRepository.getFromMemoryOrMiss(item.img))
                findViewById<TextView>(R.id.textViewProductOwned).text = totalCabinetRepository.selectedCabinet?.owned(item)?.show(settings)


                itemView.setOnClickListener {
                    val popup = PopupProductAdd(item, totalCabinetRepository,imageRepository, activity, settings)
                    popup.show(it)
                }
                itemView.setOnLongClickListener {
                    with(totalCabinetRepository) {
                        val b = AlertDialog.Builder(activity)
                        selectedCabinet?.let {
                            b.setTitle("Sure you want to delete product ${item.name} from ${it.name}")
                            b.setPositiveButton("Yes") { _dialogInterface: DialogInterface, _i: Int ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    removeItemFromCabinet(it.id, item.id)
                                }
                            }
                            b.setNegativeButton("Cancel"){ dialogInterface: DialogInterface, _i: Int ->
                                dialogInterface.cancel()
                            }
                        }
                        b.show()
                    }
                    true
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

    private fun submitImageFound(url: String){
        val item = items.find{ it.img == url}
        CoroutineScope(Dispatchers.Main).launch {
            notifyItemChanged(items.indexOf(item))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layout = R.layout.item_product
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ProductViewHolder(itemView, imageRepository, totalCabinetRepository, activity, settings)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}