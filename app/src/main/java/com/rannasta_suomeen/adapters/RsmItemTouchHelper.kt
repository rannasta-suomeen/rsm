package com.rannasta_suomeen.adapters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat

abstract class RsmItemTouchHelper(callback: RsmCallback) : ItemTouchHelper(
    callback
)

abstract class RsmCallback(
    private val color: Int, private val context: Context,
    @DrawableRes
    private val leftImage: Int,
    @DrawableRes
    private val rightImage: Int
) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return makeFlag(
            ItemTouchHelper.ACTION_STATE_SWIPE,
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && isCurrentlyActive) {
            val iW = viewHolder.itemView
            val paint = Paint()
            paint.color = color

            val iconHeight = iW.height / 3

            val data = when (dX > 0) {
                true -> {
                    Triple(
                        RectF(iW.left.toFloat(), iW.top.toFloat(), dX, iW.bottom.toFloat()),
                        VectorDrawableCompat.create(
                            context.resources,
                            leftImage, null
                        ),
                        iconHeight.toFloat() / 2
                    )
                }
                false -> {
                    Triple(
                        RectF(
                            iW.right.toFloat(),
                            iW.top.toFloat(),
                            iW.right.toFloat() + dX,
                            iW.bottom.toFloat()
                        ),
                        VectorDrawableCompat.create(
                            context.resources,
                            rightImage, null
                        ),
                        iW.width.toFloat() - iconHeight * 3 / 2
                    )
                }
            }
            c.drawRect(data.first, paint)
            data.second?.let {
                it.setBounds(0, 0, iconHeight, iconHeight)
                c.save()
                c.translate(data.third, iW.top + (iW.height - iconHeight).toFloat() / 2)
                it.draw(c)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX / 3, dY, actionState, isCurrentlyActive)
    }
}