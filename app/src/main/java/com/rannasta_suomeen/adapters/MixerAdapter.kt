package com.rannasta_suomeen.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.*
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.popup_windows.normalize
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.reflect.KFunction3

class MixerAdapter(settings: Settings, drinkList: List<DrinkTotal>, onTouchCallBack: (GeneralIngredient) -> Unit, onLongTouchCallback: (GeneralIngredient) -> Unit): MixerAdapterBase<MixerAdapter.ViewHolder, GeneralIngredient>(settings, drinkList, onTouchCallBack, onLongTouchCallback){
    class ViewHolder(itemView: View): BindableViewHolder<GeneralIngredient>(itemView){
        override fun bind(
            mixer: GeneralIngredient,
            owned: TreeMap<Int,CabinetMixer>,
            drinkList: List<DrinkTotal>,
            settings: Settings,
            ownedAlcohol: TreeMap<Int, GeneralIngredient>,
            onTouchCallBack: (GeneralIngredient) -> Unit,
            onLongTouchCallback: (GeneralIngredient) -> Unit){
            with(itemView){
                setOnClickListener { onTouchCallBack(mixer) }
                setOnLongClickListener {
                    onLongTouchCallback(mixer)
                    true
                }
                findViewById<TextView>(R.id.textViewMixerName).text = mixer.name
                findViewById<TextView>(R.id.textViewMixerPrice).text = displayDecimal(mixer.price(settings), R.string.ppl)
                val now = findViewById<TextView>(R.id.textViewMixerNewRecipesNow)
                val total = findViewById<TextView>(R.id.textViewMixerNewRecipesTotal)
                val used = findViewById<TextView>(R.id.textViewMixerUsedInTotal)
                val ownedAmount = findViewById<TextView>(R.id.textViewMixerOwned)
                val image = findViewById<ImageView>(R.id.imageViewMixerOwned)
                when (mixer.isOwned(owned)){
                    true -> {
                        now.visibility = View.INVISIBLE
                        total.visibility = View.INVISIBLE
                        used.visibility = View.INVISIBLE
                        image.visibility = View.VISIBLE
                        ownedAmount.visibility = View.VISIBLE
                        ownedAmount.text = mixer.showAmount(owned, settings)
                    }
                    false -> {
                        val ownedMap = (owned.mapValues { it.value.ingredient }.toSortedMap() + ownedAlcohol).toSortedMap() as TreeMap<Int, GeneralIngredient>

                        fun fast(fn: KFunction3<DrinkTotal, TreeMap<Int, GeneralIngredient>, GeneralIngredient, Boolean>): List<DrinkTotal>{
                            return drinkList.filter{fn(it, ownedMap, mixer)}
                        }
                        val nowList = fast(DrinkTotal::isMissingOnly)
                        val totalList = fast(DrinkTotal::isMissingButHasAlcoholic).filter { !nowList.contains(it)}
                        val usedList = drinkList.filter { it.contains(mixer) }.filter { !nowList.contains(it) && !totalList.contains(it)}
                        now.visibility = View.VISIBLE
                        total.visibility = View.VISIBLE
                        used.visibility = View.VISIBLE
                        ownedAmount.visibility = View.INVISIBLE
                        image.visibility = View.INVISIBLE
                        now.text = resources.getString(R.string.new_now,nowList.count())
                        total.text = resources.getString(R.string.new_total,totalList.count())
                        used.text = resources.getString(R.string.used_total, usedList.count())
                    }
                }
            }
        }
    }


    @Suppress("NotifyDataSetChanged")
    override fun reSort(){
        val ownedMap = owned.filter { it.value.usable }.mapValues { it.value.ingredient }.toSortedMap() as TreeMap<Int, GeneralIngredient>
        ownedMap += ownedAlcohol
        val t = fullItems.filter { it.name.normalize().contains(filter.normalize()) }.sortedBy {d ->
            drinkList.count {it.isMissing(ownedMap, d)}
        }.sortedBy {d-> drinkList.count{it.isMissingOnly(ownedMap, d)} }.reversed()
        items = t
        CoroutineScope(Dispatchers.Main).launch {
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = R.layout.item_mixer
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ViewHolder(itemView)
    }
}

class ShoppingMixerAdapter(private val totalCabinetRepository: TotalCabinetRepository, private val shoppingCart: ShoppingCart, settings: Settings, drinkList: List<DrinkTotal>): MixerAdapterBase<ShoppingMixerAdapter.ViewHolder, ShoppingCartMixer>(settings, drinkList, {}, {}){
    class ViewHolder(itemView: View): BindableViewHolder<ShoppingCartMixer>(itemView){
        override fun bind(
            mixer: ShoppingCartMixer,
            owned: TreeMap<Int, CabinetMixer>,
            drinkList: List<DrinkTotal>,
            settings: Settings,
            ownedAlcohol: TreeMap<Int, GeneralIngredient>,
            onTouchCallBack: (ShoppingCartMixer) -> Unit,
            onLongTouchCallback: (ShoppingCartMixer) -> Unit
        ) {
            with(itemView){
                setOnLongClickListener {
                    onLongTouchCallback(mixer)
                    true
                }
                findViewById<TextView>(R.id.textViewShoppingMixerName).text = mixer.name
                findViewById<TextView>(R.id.textViewShoppingMixerAmount).text = mixer.amount?.toDouble()
                    ?.let { mixer.mixer.unit.displayInDesiredUnit(it,settings.prefUnit) }
                    ?:"inf"
                findViewById<TextView>(R.id.textViewShoppingMixerPrice).text = displayDecimal(mixer.price(), R.string.price)
                findViewById<Button>(R.id.buttonBuyMixer).setOnClickListener {
                    onTouchCallBack(mixer)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = R.layout.item_mixer_shopping
        val itemView = LayoutInflater.from(parent.context).inflate(layout, parent,false)
        return ViewHolder(itemView)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun reSort() {
        val t = fullItems.filter { it.name.normalize().contains(filter.normalize()) }.sortedBy { it.name }
        items = t
        CoroutineScope(Dispatchers.Main).launch {
            notifyDataSetChanged()
        }
    }

    private fun notifyBought(shoppingCartMixer: ShoppingCartMixer){
        totalCabinetRepository.addOrModifyMixerToSelected(shoppingCartMixer.mixer.id, shoppingCartMixer.amount)
        notifyDelete(shoppingCartMixer)
    }

    private fun notifyDelete(shoppingCartMixer: ShoppingCartMixer){
        val index = shoppingCart.getMixers().indexOf(shoppingCartMixer)
        shoppingCart.removeMixerAt(index)
        items = shoppingCart.getMixers()
        notifyItemRemoved(index)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], owned, drinkList,settings, ownedAlcohol, ::notifyBought, ::notifyDelete)
    }
}

abstract class BindableViewHolder<I: NameableItem>(itemView: View): RecyclerView.ViewHolder(itemView){
    abstract fun bind(mixer: I, owned: TreeMap<Int, CabinetMixer>, drinkList: List<DrinkTotal>,settings: Settings, ownedAlcohol: TreeMap<Int, GeneralIngredient>, onTouchCallBack: (I) -> Unit, onLongTouchCallback: (I) -> Unit)
}

abstract class NameableItem{
    abstract val name: String
}


abstract class MixerAdapterBase<T: BindableViewHolder<I>, I: NameableItem>(
    protected val settings: Settings,
    protected var drinkList: List<DrinkTotal>,
    protected val onTouchCallBack: (I) -> Unit,
    protected val onLongTouchCallback: (I) -> Unit): RecyclerView.Adapter<T>()  {

    protected var owned: TreeMap<Int, CabinetMixer> = TreeMap()
    protected var items: List<I> = listOf()
    protected var filter: String = ""
    protected var fullItems: List<I> = listOf()
    protected var ownedAlcohol: TreeMap<Int, GeneralIngredient> = TreeMap()

    override fun onBindViewHolder(holder: T, position: Int) {
        holder.bind(items[position], owned, drinkList,settings, ownedAlcohol, onTouchCallBack, onLongTouchCallback)
    }

    fun submitItems(l: List<I>){
        if (l == items) return
        fullItems = l
        reSort()
    }

    fun submitNewOwned(l: TreeMap<Int, CabinetMixer>){
        if (l == owned) return
        owned = l
        reSort()
    }

    fun submitNewDrinks(l: List<DrinkTotal>){
        if (l == drinkList) return
        drinkList = l
        reSort()
    }

    fun submitNewAlcohol(l:TreeMap<Int, GeneralIngredient>){
        if (l == ownedAlcohol) return
        ownedAlcohol = l
        reSort()
    }

    fun submitNewSearch(s: String){
        filter = s
        reSort()
    }

    abstract fun reSort()

    override fun getItemCount(): Int {
        return items.size
    }
}