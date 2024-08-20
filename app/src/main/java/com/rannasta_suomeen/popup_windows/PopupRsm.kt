package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow

// TODO: Refactor all popups to use this
abstract class PopupRsm(activity: Activity, layout: Int, root: ViewGroup?) {

    val view = activity.layoutInflater.inflate(layout, root)
    protected val window: PopupWindow = PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

    private fun create(){
        window.isFocusable = true
        bind(view)
    }

    abstract fun bind(view: View)

    fun show(parent: View){
        create()
        window.showAtLocation(parent, Gravity.NO_GRAVITY, 0,0)
    }
}