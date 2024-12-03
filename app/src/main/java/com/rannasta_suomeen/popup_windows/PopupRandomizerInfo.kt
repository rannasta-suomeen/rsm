package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.RecipePartAdapter
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.RandomizerList
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.displayDecimal
import com.rannasta_suomeen.storage.Settings
import java.util.*

class PopupRandomizerInfo(
    activity: Activity,
    private val items: RandomizerList,
    private val settings: Settings,
    private val owned: TreeMap<Int, GeneralIngredient>): PopupRsm(activity, R.layout.popup_randomizer_info,null) {
    override fun bind(view: View) {
        val mixerList = items.requiredMixers()
        val alcoholicList = items.requiredAlcoholic()
        with(view){
            findViewById<TextView>(R.id.textViewRandomizerPrice).text = displayDecimal(items.price(settings),R.string.price)
            findViewById<TextView>(R.id.textViewRandomizerVolume).text = UnitType.Ml.displayInDesiredUnit(items.volume(),settings.prefUnit)
            findViewById<TextView>(R.id.textViewRandomizerShots).text = displayDecimal(items.fsd(),R.string.shots)
            findViewById<TextView>(R.id.textViewRandomizerPps).text = displayDecimal(items.aer(settings),R.string.aer)
            findViewById<TextView>(R.id.textViewRandomizerAlcoholic).text = resources.getString(R.string.needed_alcoholic,alcoholicList.size)
            findViewById<TextView>(R.id.textViewRandomizerMixers).text = resources.getString(R.string.needed_mixers,mixerList.size)
            findViewById<TextView>(R.id.textViewRandomizerItems).text = resources.getString(R.string.kpl_int, alcoholicList.size + mixerList.size)
            val alcoholicRecycler = findViewById<RecyclerView>(R.id.recyclerViewRandomizerAlcoholic)
            val mixerRecycler = findViewById<RecyclerView>(R.id.recyclerViewRandomizerMixers)
            alcoholicRecycler.layoutManager = LinearLayoutManager(activity)
            mixerRecycler.layoutManager = LinearLayoutManager(activity)
            val alcoholicAdapter = RecipePartAdapter(owned, settings)
            val mixerAdapter = RecipePartAdapter(owned, settings)
            alcoholicRecycler.adapter = alcoholicAdapter
            mixerRecycler.adapter = mixerAdapter
            alcoholicAdapter.submitItems(alcoholicList,owned)
            mixerAdapter.submitItems(mixerList,owned)
            findViewById<Button>(R.id.buttonClose).setOnClickListener {
                window.dismiss()
            }
        }
    }
}