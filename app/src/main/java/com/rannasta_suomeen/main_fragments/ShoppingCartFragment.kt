package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.ShoppingCartAdapter
import com.rannasta_suomeen.popup_windows.PopupShoppingCartInfo
import com.rannasta_suomeen.storage.*

class ShoppingCartFragment(private val activity: Activity,private val shoppingCart: ShoppingCart,imageRepository: ImageRepository,private val totalCabinetRepository: TotalCabinetRepository,private val totalDrinkRepository: TotalDrinkRepository,private val settings: Settings) : Fragment(R.layout.fragment_shopping_cart){
    private val shoppingCartAdapter = ShoppingCartAdapter(activity,imageRepository, totalCabinetRepository, settings, shoppingCart)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            val shoppingCartView = findViewById<RecyclerView>(R.id.recyclerViewShoppingCart)
            shoppingCartView.adapter = shoppingCartAdapter
            shoppingCartView.layoutManager = LinearLayoutManager(context)
            val buttomInfo = findViewById<ImageButton>(R.id.imageButtonCartInfo)
            buttomInfo.setOnClickListener {
                PopupShoppingCartInfo(activity,shoppingCart, settings, totalCabinetRepository, totalDrinkRepository).show(buttomInfo)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}