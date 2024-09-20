package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.ShoppingCartAdapter
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository

class ShoppingCartFragment(activity: Activity,shoppingCart: ShoppingCart, imageRepository: ImageRepository, totalCabinetRepository: TotalCabinetRepository, settings: Settings) : Fragment(R.layout.fragment_shopping_cart){
    private val shoppingCartAdapter = ShoppingCartAdapter(activity,imageRepository, totalCabinetRepository, settings, shoppingCart)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            val shoppingCartView = findViewById<RecyclerView>(R.id.recyclerViewShoppingCart)
            shoppingCartView.adapter = shoppingCartAdapter
            shoppingCartView.layoutManager = LinearLayoutManager(context)
        }
        super.onViewCreated(view, savedInstanceState)
    }
}