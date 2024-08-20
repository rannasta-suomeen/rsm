package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.adapters.CabinetProductAdapter
import com.rannasta_suomeen.adapters.CabinetProductAdapterItemTouchHelper
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.popup_windows.PopupCabinetShare
import com.rannasta_suomeen.popup_windows.PopupExportProducts
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class CabinetFragment(private val activity: Activity, private val imageRepository: ImageRepository, private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository): Fragment(R.layout.fragment_cabinets), AdapterView.OnItemSelectedListener{

    private lateinit var adapter: CabinetProductAdapter
    private var cabinetList = listOf<Cabinet>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var selectedCabinet: Cabinet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Cabinets", "Created Again")
        super.onCreate(savedInstanceState)
        adapter = CabinetProductAdapter(activity, totalCabinetRepository, imageRepository, settings)
        spinnerAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpUi(view)
    }

    private fun setUpUi(view: View){
        with(view) {
            val spinner = findViewById<Spinner>(R.id.spinnerSelectCabinet)
            val buttonNewCabinet = findViewById<Button>(R.id.buttonNewCabinet)
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewCabinetProducts)
            val fabShare = findViewById<FloatingActionButton>(R.id.fabSharing)
            val fabMove = findViewById<FloatingActionButton>(R.id.fabMove)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
            val helper = CabinetProductAdapterItemTouchHelper(adapter, MaterialColors.getColor(context, com.google.android.material.R.attr.colorTertiary, Color.GREEN),context)
            helper.attachToRecyclerView(recyclerView)

            spinner.adapter = spinnerAdapter
            spinner.onItemSelectedListener = this@CabinetFragment

            buttonNewCabinet.setOnClickListener {
                val b = AlertDialog.Builder(requireContext())
                b.setTitle("Choose a name for the new cabinet")
                val editText = EditText(requireContext())
                editText.inputType = InputType.TYPE_CLASS_TEXT
                b.setView(editText)
                b.setPositiveButton("Ok"){ dialogInterface: DialogInterface, i: Int ->
                    CoroutineScope(Dispatchers.IO).launch {
                        totalCabinetRepository.createCabinet(editText.text.toString())
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(requireContext(), "Created cabinet ${editText.text}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                b.setNegativeButton("Cancel"){ dialogInterface: DialogInterface, i: Int ->
                    dialogInterface.cancel()
                }
                b.setNeutralButton("Join"){_,_->
                    CoroutineScope(Dispatchers.IO).launch {
                        totalCabinetRepository.joinCabinet(editText.text.toString().trim())
                    }
                }
                b.show()
            }

            buttonNewCabinet.setOnLongClickListener {
                val b = AlertDialog.Builder(requireContext())
                with(totalCabinetRepository){
                    b.setTitle("Sure you want to delete cabinet ${selectedCabinet?.name}")
                    b.setPositiveButton("Yes") { _dialogInterface: DialogInterface, _i: Int ->
                        CoroutineScope(Dispatchers.IO).launch {
                            selectedCabinet?.let { it1 ->
                                if (it1.ownerId == it1.getOwnUserId()){
                                    deleteCabinet(it1.id)
                                } else {
                                    exitCabinet(it1.id)
                                }
                            }
                            totalCabinetRepository.changeSelectedCabinet(null)
                            CoroutineScope(Dispatchers.Main).launch {
                                changeSelectedCabinet()
                            }
                        }
                    }
                }
                b.setNegativeButton("Cancel"){ dialogInterface: DialogInterface, _i: Int ->
                    dialogInterface.cancel()
                }
                b.show()
                true
            }

            fabShare.setOnClickListener {
                if (selectedCabinet != null){
                    Log.d("Cabinets", "Selected cabinet is ${selectedCabinet!!.name}")
                    PopupCabinetShare(activity, selectedCabinet!!).show(it)
                } else {
                    Toast.makeText(activity, "Cannot share a non existent cabinet", Toast.LENGTH_SHORT).show()
                }
            }

            fabMove.setOnClickListener{
                if (selectedCabinet != null){
                    Log.d("Cabinets", "Selected cabinet is ${selectedCabinet!!.name}")
                    PopupExportProducts(activity, totalCabinetRepository).show(it)
                } else {
                    Toast.makeText(activity, "Cannot move a non existent cabinet", Toast.LENGTH_SHORT).show()
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                launch {
                    totalCabinetRepository.cabinetFlow.collect{
                        cabinetList = it
                        CoroutineScope(Dispatchers.Main).launch {
                            spinnerAdapter.clear()
                            spinnerAdapter.addAll(cabinetList.map { it.name })
                            val pos = cabinetList.indexOfFirst { it.id == settings.cabinet }
                            spinner.setSelection(max(pos, 0))
                            changeSelectedCabinet()
                        }
                    }
                }
                totalCabinetRepository.selectedCabinetFlow.collect{
                    selectedCabinet = it
                    changeSelectedCabinet()
                }
            }
        }
    }

    private fun changeSelectedCabinet(){
        CoroutineScope(Dispatchers.Main).launch {
            selectedCabinet?.products?.let { adapter.submitItems(it.sortedBy { it.product.name }) }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val item = spinnerAdapter.getItem(p2)
        totalCabinetRepository.changeSelectedCabinet(cabinetList.find { it.name == item })
        changeSelectedCabinet()
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}