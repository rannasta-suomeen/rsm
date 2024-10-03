package com.rannasta_suomeen.main_fragments.shopping_cart_fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.ShoppingMixerAdapter

class ShoppingMixersFragment(private val shoppingMixerAdapter: ShoppingMixerAdapter): Fragment(R.layout.view_recycler_search) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            val shoppingCartView = findViewById<RecyclerView>(R.id.recyclerViewSearch)
            shoppingCartView.adapter = shoppingMixerAdapter
            shoppingCartView.layoutManager = LinearLayoutManager(context)
        }
        super.onViewCreated(view, savedInstanceState)
    }
}