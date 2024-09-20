package com.rannasta_suomeen.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R

class ProductAdapterItemTouchHelper(recyclerView: ProductAdapter, color: Int, context: Context) :
    RsmItemTouchHelper(ProductCallBack(recyclerView, color, context))

class ProductCallBack(private val recyclerView: ProductAdapter, color: Int, context: Context) :
    RsmCallback(
        color,
        context,
        R.drawable.ic_baseline_storefront_24,
        R.drawable.ic_baseline_wine_bar_24
    ) {
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        recyclerView.notifySwipe(viewHolder as ProductAdapter.ProductViewHolder, direction)
    }
}