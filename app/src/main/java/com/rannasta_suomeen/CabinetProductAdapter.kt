package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.CabinetProduct
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.data_classes.from
import com.rannasta_suomeen.popup_windows.PopupProductAdd
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

class CabinetProductAdapter(
    private val activity: Activity,
    private val totalCabinetRepository: TotalCabinetRepository, private val imageRepository: ImageRepository, private val settings: Settings): RecyclerView.Adapter<CabinetProductAdapter.ProductViewHolder>() {

    private var items: List<CabinetProduct> = listOf()

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
        ):RecyclerView.ViewHolder(itemView){
        fun bind(item: CabinetProduct, settings: Settings){
            with(itemView){
                findViewById<TextView>(R.id.textViewProductName).text = item.product.name
                findViewById<TextView>(R.id.textViewProductPrice).text = displayDecimal(item.product.price, R.string.price)
                findViewById<TextView>(R.id.textViewProductVolume).text = item.product.volumeDesired(settings)
                findViewById<TextView>(R.id.textViewProductAbv).text = displayDecimal(item.product.abv, R.string.abv)
                findViewById<TextView>(R.id.textViewRetailer).text = item.product.retailer.toString()
                findViewById<TextView>(R.id.textViewProductFsd).text = displayDecimal(item.product.fsd(), R.string.shots)
                findViewById<TextView>(R.id.textViewProductPpl).text = displayDecimal(item.product.unit_price, R.string.ppl)
                findViewById<TextView>(R.id.textViewProductPps).text = displayDecimal(item.product.pps(), R.string.aer)
                findViewById<TextView>(R.id.textViewProductSubcategory).text = from(item.product.subcategory_id).toString()
                findViewById<ImageView>(R.id.imageViewProduct).setImageResource(R.drawable.ic_baseline_wine_bar_24)
                findViewById<ImageView>(R.id.imageViewProduct).setImageBitmap(imageRepository.getFromMemoryOrMiss(item.product.img))
                findViewById<TextView>(R.id.textViewProductOwned).text = totalCabinetRepository.selectedCabinet?.containedAmountCabinet(item)?.show(settings)

                itemView.setOnClickListener {
                    val popup = PopupProductAdd(item.product, totalCabinetRepository,imageRepository, activity, settings)
                    popup.show(it)
                }
                itemView.setOnLongClickListener {
                    with(totalCabinetRepository) {
                        val b = AlertDialog.Builder(activity)
                        selectedCabinet?.let {
                            b.setTitle("Sure you want to delete product ${item.product.name} from ${it.name}")
                            b.setPositiveButton("Yes") { _dialogInterface: DialogInterface, _i: Int ->
                                if (item.ownerId == it.getOwnUserId()){
                                    CoroutineScope(Dispatchers.IO).launch {
                                        removeItemFromCabinet(it.id, item.id)
                                    }
                                } else {
                                    Toast.makeText(context, "Cannot remove product owned by someone else", Toast.LENGTH_SHORT).show()
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
            val number = String.format(Locale.UK,"%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<CabinetProduct>){
        items = input
        notifyDataSetChanged()
    }

    fun notifySwipe(viewHolder: ProductViewHolder, direction: Int){
        val item = items[viewHolder.adapterPosition]
        val amount = when (direction){
            ItemTouchHelper.RIGHT -> null
            ItemTouchHelper.LEFT -> item.product.volumeMl().roundToInt()
            else -> null
        }
        totalCabinetRepository.addOrModifyToSelected(item.product.id, amount)
        val displayAmount = amount?.let{UnitType.ml.displayInDesiredUnit(it.toDouble(), settings.prefUnit)}?: "Inf"
        Toast.makeText(activity.baseContext, "Added $displayAmount of ${item.product.name}", Toast.LENGTH_SHORT).show()
    }

    private fun submitImageFound(url: String){
        val item = items.find{ it.product.img == url}
        CoroutineScope(Dispatchers.Main).launch {
            notifyItemChanged(items.indexOf(item))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layout = R.layout.item_product
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ProductViewHolder(itemView, imageRepository, totalCabinetRepository, activity)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, settings)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}