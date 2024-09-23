package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.ExportProductAdapter
import com.rannasta_suomeen.data_classes.CabinetCompact
import com.rannasta_suomeen.data_classes.CabinetProduct
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupExportProducts(private val activity: Activity, private val totalCabinetRepository: TotalCabinetRepository): PopupRsm(activity, R.layout.popup_export_products, null), AdapterView.OnItemSelectedListener {
    private val originAdapter = ExportProductAdapter(::onClickItemOrigin)
    private val targetAdapter = ExportProductAdapter(::onClickItemTarget)
    private var targetCabinet: CabinetCompact? = null
    private var cabinetTargetList: List<String> = listOf()
    override fun bind(view: View) {
        with(view){
            findViewById<TextView>(R.id.textViewExportOrigin).text = totalCabinetRepository.selectedCabinet?.name?:"No selected cabinet"
            val spinnerTarget = findViewById<Spinner>(R.id.spinnerExportTarget)
            val spinnerAdapter: ArrayAdapter<String> = ArrayAdapter(activity, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
            spinnerTarget.adapter = spinnerAdapter
            val recyclerViewOrigin = findViewById<RecyclerView>(R.id.recyclerViewOrigin)
            val recyclerViewTarget = findViewById<RecyclerView>(R.id.recyclerViewTarget)
            recyclerViewOrigin.layoutManager = LinearLayoutManager(activity)
            recyclerViewTarget.layoutManager = LinearLayoutManager(activity)
            recyclerViewOrigin.adapter = originAdapter
            recyclerViewTarget.adapter = targetAdapter
            refreshOrigin()
            cabinetTargetList = totalCabinetRepository.cabinetList.filter { it.id != totalCabinetRepository.selectedCabinet?.id }.map { it.name }
            spinnerAdapter.addAll(cabinetTargetList)
            spinnerTarget.onItemSelectedListener = this@PopupExportProducts

            val buttonOk = findViewById<Button>(R.id.buttonExportConfirm)
            val buttonCancel = findViewById<Button>(R.id.buttonExportCancel)
            buttonOk.setOnClickListener {
                if (targetCabinet == null){
                    Toast.makeText(activity, "No target", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (targetAdapter.list.isNotEmpty()){
                    CoroutineScope(Dispatchers.IO).launch {
                        NetworkController.tryNTimes(5,
                            NetworkController.CabinetOperation.BulkMoveItems(totalCabinetRepository.selectedCabinet!!.id,targetCabinet!!.id,
                                targetAdapter.list.map { it.id }),
                            NetworkController::moveItemsIntoCabinet)
                    }
                }
                window.dismiss()
            }
            buttonCancel.setOnClickListener {
                window.dismiss()
            }
        }
    }

    private fun refreshOrigin(){
        totalCabinetRepository.selectedCabinet?.let {
            val oid = it.getOwnUserId()
            val items = it.products.filter { it.ownerId == oid }
            originAdapter.submitItems(items)
        }
    }

    private fun onClickItemOrigin(x: CabinetProduct){
        originAdapter.deleteItem(x)
        targetAdapter.addItem(x)
    }

    private fun onClickItemTarget(x: CabinetProduct){
        targetAdapter.deleteItem(x)
        originAdapter.addItem(x)
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        targetCabinet = totalCabinetRepository.cabinetList.find {it.name == cabinetTargetList[p2]}
        targetCabinet?.let {
        }
        (p0?.getChildAt(0) as TextView).setTextColor(MaterialColors.getColor(p0,
            androidx.appcompat.R.attr.colorPrimary))
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        targetCabinet = totalCabinetRepository.cabinetList.find { it.name ==cabinetTargetList.getOrNull(0)}
    }
}