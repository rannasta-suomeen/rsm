package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.ProductAdapter
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class CabinetFragment(private val activity: Activity, private val imageRepository: ImageRepository, private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository): Fragment(R.layout.fragment_cabinets), AdapterView.OnItemSelectedListener{

    private lateinit var adapter: ProductAdapter
    private var cabinetList = listOf<Cabinet>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var selectedCabinet: Cabinet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("Cabinets", "Created Again")
        super.onCreate(savedInstanceState)
        adapter = ProductAdapter(activity, totalCabinetRepository, imageRepository, settings)
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
            val fabAddToCabinet = findViewById<FloatingActionButton>(R.id.fabAddToCabinet)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            recyclerView.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
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
                b.show()
            }

            buttonNewCabinet.setOnLongClickListener {
                val b = AlertDialog.Builder(requireContext())
                with(totalCabinetRepository){
                    b.setTitle("Sure you want to delete cabinet ${selectedCabinet?.name}")
                    b.setPositiveButton("Yes") { _dialogInterface: DialogInterface, _i: Int ->
                        CoroutineScope(Dispatchers.IO).launch {
                            selectedCabinet?.let { it1 -> deleteCabinet(it1.id) }
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
            selectedCabinet?.products?.let { adapter.submitItems(it.map { it.product }) }
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