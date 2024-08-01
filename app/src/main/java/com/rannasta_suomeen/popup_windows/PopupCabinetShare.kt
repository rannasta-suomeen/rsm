package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.data_classes.CabinetMember
import com.rannasta_suomeen.displayDecimal

class PopupCabinetShare(activity: Activity,private val cabinet: Cabinet): PopupRsm(activity, R.layout.popup_cabinet_sharing, root = null) {

    private val adapter = MemberAdapter(cabinet)

    private class MemberAdapter(private var cabinet: Cabinet): BaseAdapter(){
        override fun getCount(): Int {
            return cabinet.members.size
        }

        override fun getItem(p0: Int): Any {
            return cabinet.members[p0]
        }

        override fun getItemId(p0: Int): Long {
            return p0.toLong()
        }

        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            val itemView = p1?: LayoutInflater.from(p2!!.context).inflate(R.layout.item_cabinet_member,p2, false)
            val item = getItem(p0) as CabinetMember
            with(itemView){
                findViewById<TextView>(R.id.textViewShareCabinetName).text = cabinet.name
                findViewById<TextView>(R.id.textViewMemberName).text = item.userName
                findViewById<TextView>(R.id.textViewMemberAmount).text = getMemberVolume(item).toString()
                findViewById<TextView>(R.id.textViewMemberFsd).text = showMemberFsd(item, itemView)
                findViewById<TextView>(R.id.textViewMemberValue).text = showMemberValue(item, itemView)
                findViewById<ImageView>(R.id.imageViewMemberOwner).isVisible = cabinet.ownerId == item.userId
            }
            return itemView
        }

        fun getMemberVolume(x: CabinetMember):Int{
            return cabinet.products.filter { it.ownerId == x.userId }.count()
        }

        fun showMemberFsd(x: CabinetMember, view: View):String{
            val sum = cabinet.products.filter { it.ownerId == x.userId }.map {
                it.estimatedFsd()
            }.sum()
            return displayDecimal(sum, R.string.shots, view)
        }

        fun showMemberValue(x: CabinetMember, view: View):String{
            val sum = cabinet.products.filter { it.ownerId == x.userId }.map {
                it.estimatedPrice()
            }.sum()
            return displayDecimal(sum, R.string.price, view)
        }
    }
    override fun bind(view: View) {
        with(view){
            if (cabinet.accessKey != null) {
                findViewById<TextView>(R.id.textViewCabinetCode).text = cabinet.accessKey
            } else{
                findViewById<TextView>(R.id.textViewCabinetCode).text = context.resources.getString(R.string.not_shared)
            }
            findViewById<ListView>(R.id.listViewCabinetMembers).adapter = adapter
            val button = findViewById<Button>(R.id.buttonCabinetGenerate)
            button.setEnableDisable(cabinet.accessKey == null)
            // TODO: Implement a generate button for access key
            // TODO: Implement a way to join cabinets
        }
    }

    private fun Button.disable(){
        alpha = 0.5f
        isClickable = false
    }
    private fun Button.enable(){
        alpha = 1f
        isClickable = true
    }
    private fun Button.setEnableDisable(b: Boolean){
        when(b){
            true -> enable()
            false -> disable()
        }
    }
}
