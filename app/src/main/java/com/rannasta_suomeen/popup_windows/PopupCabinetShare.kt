package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.*
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.data_classes.CabinetMember
import com.rannasta_suomeen.data_classes.toTreemap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupCabinetShare(activity: Activity,private val cabinet: Cabinet): PopupRsm(activity, R.layout.popup_cabinet_sharing, root = null) {

    private val adapter = MemberAdapter(cabinet)

    private class MemberAdapter(private var cabinet: Cabinet): RecyclerView.Adapter<MemberAdapter.MemberViewHolder>(){
        private class MemberViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            fun bind(item: CabinetMember, cabinet: Cabinet){
                fun getMemberOwned(x: CabinetMember):Int{
                    return cabinet.products.count { it.ownerId == x.userId }
                }

                fun showMemberFsd(x: CabinetMember, view: View):String{
                    val sum = cabinet.products.filter { it.ownerId == x.userId }.map {
                        it.estimatedFsd()
                    }.sum()
                    return view.displayDecimal(sum, R.string.shots)
                }

                fun showMemberValue(x: CabinetMember, view: View):String{
                    val sum = cabinet.products.filter { it.ownerId == x.userId }.map {
                        it.estimatedPrice()
                    }.sum()
                    return view.displayDecimal(sum, R.string.price)
                }
                with(itemView){
                    findViewById<TextView>(R.id.textViewMemberName).text = item.userName
                    findViewById<TextView>(R.id.textViewMemberAmount).text = itemView.resources.getString(R.string.kpl,getMemberOwned(item).toString())
                    findViewById<TextView>(R.id.textViewMemberFsd).text = showMemberFsd(item, itemView)
                    findViewById<TextView>(R.id.textViewMemberValue).text = showMemberValue(item, itemView)
                    findViewById<ImageView>(R.id.imageViewMemberOwner).isVisible = cabinet.ownerId == item.userId
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            val layout = R.layout.item_cabinet_member
            val itemView = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return MemberViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            val item = cabinet.members[position]
            holder.bind(item, cabinet)
        }

        override fun getItemCount(): Int {
            return cabinet.members.size
        }
    }

    override fun bind(view: View) {
        with(view){
            val accessKeyView = findViewById<TextView>(R.id.textViewCabinetCode)
            findViewById<TextView>(R.id.textViewShareCabinetName).text = cabinet.name
            if (cabinet.accessKey != null) {
                accessKeyView.text = cabinet.accessKey
            } else{
                accessKeyView.text = context.resources.getString(R.string.not_shared)
            }
            val listView = findViewById<RecyclerView>(R.id.listViewCabinetMembers)
            listView.adapter = adapter
            listView.layoutManager = LinearLayoutManager(context)
            val button = findViewById<Button>(R.id.buttonCabinetGenerate)
            button.setEnableDisable(cabinet.accessKey == null)
            button.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val res = NetworkController.tryNTimes(5, cabinet.id, NetworkController::shareCabinet)
                    if (res.isSuccess){
                        CoroutineScope(Dispatchers.Main).launch {
                            accessKeyView.text = res.getOrThrow()
                        }
                    }
                }
            }
            val makeableDrinksTotal = totalDrinkRepository.makeableWithStrict((totalCabinetRepository.productsToIngredients(cabinet.products) + cabinet.mixers.map { it.ingredient }).toTreemap())
            val makeableDrinksAlcoholic = totalDrinkRepository.makeableWithAlcohol(totalCabinetRepository.productsToIngredients(cabinet.products).toTreemap())
            val makeableDrinksMixers = totalDrinkRepository.makeableWithMixers(cabinet.mixers.map { it.ingredient }.toTreemap())
            val allDrinks = totalDrinkRepository.totalDrinkList
            findViewById<TextView>(R.id.textViewCabinetCoverage).text = makeableDrinksTotal.size.toString()
            findViewById<TextView>(R.id.textViewCabinetCoverageMax).text = allDrinks.size.toString()
            val percentTotal = (100*makeableDrinksTotal.size)/allDrinks.size
            findViewById<ProgressBar>(R.id.progressBarCabinetCoverage).progress = percentTotal
            findViewById<TextView>(R.id.textViewCabinetCoveragePercent).text = resources.getString(R.string.percentage, percentTotal)

            findViewById<TextView>(R.id.textViewCabinetAlcoholCoverage).text = makeableDrinksAlcoholic.size.toString()
            findViewById<TextView>(R.id.textViewCabinetAlcoholCoverageMax).text = allDrinks.size.toString()
            val percentAlcohol = (100*makeableDrinksAlcoholic.size)/allDrinks.size
            findViewById<ProgressBar>(R.id.progressBarCabinetAlcoholCoverage).progress = percentAlcohol
            findViewById<TextView>(R.id.textViewCabinetAlcoholCoveragePercent).text = resources.getString(R.string.percentage, percentAlcohol)

            findViewById<TextView>(R.id.textViewCabinetMixerCoverage).text = makeableDrinksMixers.size.toString()
            findViewById<TextView>(R.id.textViewCabinetMixerCoverageMax).text = allDrinks.size.toString()
            val percentMixer = (100*makeableDrinksMixers.size)/allDrinks.size
            findViewById<ProgressBar>(R.id.progressBarCabinetMixerCoverage).progress = percentMixer
            findViewById<TextView>(R.id.textViewCabinetMixerCoveragePercent).text = resources.getString(R.string.percentage, percentMixer)
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
