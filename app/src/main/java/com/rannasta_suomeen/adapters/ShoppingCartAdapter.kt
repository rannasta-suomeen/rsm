package com.rannasta_suomeen.adapters

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.ShoppingCartItem
import com.rannasta_suomeen.popup_windows.PopupProductAdd
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.roundToInt

class ShoppingCartAdapter(
    private val activity: Activity,
    private val imageRepository: ImageRepository,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val settings: Settings,
    private val shoppingCart: ShoppingCart): RecyclerView.Adapter<ShoppingCartAdapter.ProductViewHolder>() {

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
        private val activity: Activity,
        private val shoppingCart: ShoppingCart,
        ):RecyclerView.ViewHolder(itemView){
        fun bind(item: ShoppingCartItem, settings: Settings, callback: (ShoppingCartItem) -> Unit, deleteCallBack: (ShoppingCartItem) -> Unit){
            with(itemView){
                findViewById<TextView>(R.id.textViewProductName).text = item.product.name
                findViewById<TextView>(R.id.textViewRetailer).text = item.product.retailer.toString()
                findViewById<TextView>(R.id.textViewshoppingKpl).text = resources.getString(R.string.kpl_int, item.amount)
                findViewById<TextView>(R.id.textViewShoppingVolume).text = settings.prefUnit.displayInDesiredUnit(item.volume(settings.prefUnit),settings.prefUnit)
                findViewById<TextView>(R.id.textViewShoppingPrice).text = displayDecimal(item.price(),R.string.price)
                findViewById<TextView>(R.id.textViewShoppingPps).text = displayDecimal(item.product.pps(),R.string.aer)
                findViewById<ImageView>(R.id.imageViewShoppingProductName).setImageResource(R.drawable.ic_baseline_wine_bar_24)
                findViewById<ImageView>(R.id.imageViewShoppingProductName).setImageBitmap(imageRepository.getFromMemoryOrMiss(item.product.img))
                val buttonBuy = findViewById<Button>(R.id.buttonMarkAsBought)
                buttonBuy.setOnClickListener {
                    callback(item)
                }
                setOnLongClickListener {
                    deleteCallBack(item)
                    true
                }
                setOnClickListener {
                    val popup = PopupProductAdd(item.product,totalCabinetRepository, imageRepository, activity, settings, shoppingCart)
                    popup.show(this)
                }
            }
        }

        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format(Locale.UK,"%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    private fun notifyBought(shoppingCartItem: ShoppingCartItem){
        totalCabinetRepository.addOrModifyToSelected(shoppingCartItem.product.id,(shoppingCartItem.amount*shoppingCartItem.product.volumeMl()).roundToInt())
        notifyDelete(shoppingCartItem)
    }

    private fun notifyDelete(shoppingCartItem: ShoppingCartItem){
        val index = shoppingCart.getItems().indexOf(shoppingCartItem)
        shoppingCart.removeItemAt(index)
        notifyItemRemoved(index)
    }

    private fun submitImageFound(url: String){
        val item = shoppingCart.getItems().find{ it.product.img == url}
        CoroutineScope(Dispatchers.Main).launch {
            notifyItemChanged(shoppingCart.getItems().indexOf(item))
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layout = R.layout.item_product_shopping
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ProductViewHolder(itemView, imageRepository,totalCabinetRepository, activity, shoppingCart)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = shoppingCart.getItems()[position]
        holder.bind(item, settings,::notifyBought, ::notifyDelete)
    }

    override fun getItemCount(): Int {
        return shoppingCart.getItems().size
    }
}