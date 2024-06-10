package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.RecipePartAdapter
import com.rannasta_suomeen.data_classes.DrinkTotal

class PopupDrink(drink: DrinkTotal, activity: Activity) {

    private var window: PopupWindow
    init {
        val adapter = RecipePartAdapter(activity.applicationContext)
        val view = activity.layoutInflater.inflate(R.layout.popup_drink_recipe, null)
        window = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        window.isFocusable = true
        with(view) {

            findViewById<TextView>(R.id.textViewRecipeDrinkName).text = drink.drink.name

            val t = findViewById<RecyclerView>(R.id.recyclerViewDrinkParts)
            t.adapter = adapter
            t.layoutManager = LinearLayoutManager(context)
            t.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        }
        adapter.submitItems(drink.ingredients.recipeParts.toList())
    }

    fun show(parent: View){
        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0,0)
    }
}