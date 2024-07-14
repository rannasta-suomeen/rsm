package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.IngredientsForDrinkPointer
import com.rannasta_suomeen.storage.Settings
import java.util.Locale
import kotlin.math.roundToInt

class RecipePartAdapter(private val owned: List<GeneralIngredient>,private val settings: Settings): RecyclerView.Adapter<RecipePartAdapter.ProductViewHolder>() {

    private var items: List<IngredientsForDrinkPointer.RecipePartPointer> = listOf()
    private var amount: Double = 1.0

    class ProductViewHolder(itemView: View, private val owned: List<GeneralIngredient>,private val settings: Settings):RecyclerView.ViewHolder(itemView){
        fun bind(item: IngredientsForDrinkPointer.RecipePartPointer, amount: Double){
            with(itemView) {
                findViewById<TextView>(R.id.textViewRecipePartName).text = item.name
                findViewById<TextView>(R.id.textViewRecipePartVolume).text = item.unit.displayInDesiredUnit((item.amount * amount).roundToInt(), settings.prefUnit)
                // TODO: make this show price per desired unit
                findViewById<TextView>(R.id.textViewRecipePartPrice).text = displayDecimal(amount * item.price(settings),R.string.price)
                findViewById<TextView>(R.id.textViewRecipePartAer).text = displayDecimal(item.ingredient.price(settings),R.string.ppl)
                val img = findViewById<ImageView>(R.id.imageViewRecipePartOwned)
                when (owned.contains(item.ingredient)){
                    true -> {
                        img.setImageResource(R.drawable.ic_baseline_check_24)
                        img.setColorFilter(context.resources.getColor(R.color.green))
                    }
                    false -> {
                        img.setImageResource(R.drawable.ic_baseline_remove_24)
                        img.setColorFilter(context.resources.getColor(R.color.red))
                    }
                }
            }
        }

        private fun displayDecimal(x: Double, stringId: Int): String{
            val number = String.format(Locale.UK,"%.1f", x)
            return itemView.resources.getString(stringId, number)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitItems(input: List<IngredientsForDrinkPointer.RecipePartPointer>, o: List<GeneralIngredient>){
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
        return ProductViewHolder(itemView, owned, settings)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item,amount)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}