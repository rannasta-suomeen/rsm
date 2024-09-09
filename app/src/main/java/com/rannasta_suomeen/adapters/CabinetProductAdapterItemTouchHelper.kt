package com.rannasta_suomeen.adapters

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R

class CabinetProductAdapterItemTouchHelper(
    recyclerView: CabinetProductAdapter,
    color: Int,
    context: Context
) : RsmItemTouchHelper(CabinetProductCallBack(recyclerView, color, context))

class CabinetProductCallBack(
    private val recyclerView: CabinetProductAdapter,
    color: Int,
    context: Context
) : RsmCallback(color, context, R.drawable.ic_baseline_loop_24, R.drawable.ic_baseline_remove_24) {
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        recyclerView.notifySwipe(viewHolder as CabinetProductAdapter.ProductViewHolder, direction)
    }
}