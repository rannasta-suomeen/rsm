package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.IngredientsForDrinkPointer
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.storage.Settings
import kotlinx.coroutines.*
import kotlin.math.roundToInt

class RecipePartAdapter(context: Context): RecyclerView.Adapter<RecipePartAdapter.ProductViewHolder>() {

    private var items: List<IngredientsForDrinkPointer.RecipePartPointer> = listOf()
    private var amount: Double = 1.0

    class ProductViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        fun bind(item: IngredientsForDrinkPointer.RecipePartPointer, amount: Double){
            with(itemView) {
                findViewById<TextView>(R.id.textViewRecipePartName).text = item.name
                findViewById<TextView>(R.id.textViewRecipePartVolume).text = item.unit.displayInDesiredUnit((item.amount * amount).roundToInt(), Settings.prefUnit)
                findViewById<TextView>(R.id.textViewRecipePartPrice).text = displayDecimal(amount * item.ingredient.price()*item.unit.convert(item.amount,UnitType.cl)/100,R.string.price)
                findViewById<TextView>(R.id.textViewRecipePartAer).text = displayDecimal(item.ingredient.price(),R.string.ppl)
            }
        }

        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format("%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<IngredientsForDrinkPointer.RecipePartPointer>){
        items = input
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitAmount(x: Double){
        amount = x
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val layout = R.layout.item_recipe_part
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item,amount)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}