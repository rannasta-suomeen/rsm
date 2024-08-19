package com.rannasta_suomeen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

// TODO: Refactor this and [ProductAdapterItemTouchHelper Together]
class CabinetProductAdapterItemTouchHelper(recyclerView: CabinetProductAdapter, color: Int, context: Context) : ItemTouchHelper(Callback(recyclerView, color, context)) {
    class Callback(private val recyclerView: CabinetProductAdapter, private val color: Int, private val context: Context): ItemTouchHelper.Callback(){
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            return makeFlag(ACTION_STATE_SWIPE, RIGHT or LEFT)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            recyclerView.notifySwipe(viewHolder as CabinetProductAdapter.ProductViewHolder, direction)
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) { // TODO: Make this look reasonable again, without dumb visual bugs
            if (actionState == ACTION_STATE_SWIPE && isCurrentlyActive){
                val iW = viewHolder.itemView
                val paint = Paint()
                paint.color = color

                val iconHeight = iW.height / 3

                val data = when(dX>0){
                    true -> {
                        Triple(
                            RectF(iW.left.toFloat(),iW.top.toFloat(), dX, iW.bottom.toFloat()),
                            VectorDrawableCompat.create(context.resources, R.drawable.ic_baseline_loop_24, null),
                            iconHeight.toFloat()/2
                        )
                    }
                    false -> {
                        Triple(
                            RectF(iW.right.toFloat(),iW.top.toFloat(),iW.right.toFloat() + dX, iW.bottom.toFloat()),
                            VectorDrawableCompat.create(context.resources, R.drawable.ic_baseline_remove_24, null),
                            iW.width.toFloat()-iconHeight*3/2
                        )
                    }
                }
                c.drawRect(data.first, paint)
                data.second?.let {
                    it.setBounds(0,0,iconHeight, iconHeight)
                    c.save()
                    c.translate(data.third, iW.top + (iW.height - iconHeight).toFloat()/2)
                    it.draw(c)
                }
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX/3, dY, actionState, isCurrentlyActive)
        }
    }
}