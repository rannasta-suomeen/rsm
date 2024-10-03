package com.rannasta_suomeen.main_fragments.shopping_cart_fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.rannasta_suomeen.adapters.ShoppingMixerAdapter
import com.rannasta_suomeen.adapters.ShoppingProductAdapter

class ShoppingFragmentFactory(
    private val shoppingProductAdapter: ShoppingProductAdapter,
    private val shoppingMixerAdapter: ShoppingMixerAdapter
): FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            ShoppingProductsFragment::class.java.name -> ShoppingProductsFragment(shoppingProductAdapter)
            ShoppingMixersFragment::class.java.name -> ShoppingMixersFragment(shoppingMixerAdapter)
            else -> super.instantiate(classLoader, className)
        }
    }
}