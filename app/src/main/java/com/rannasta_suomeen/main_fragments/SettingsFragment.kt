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
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rannasta_suomeen.ProductAdapter
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

class SettingsFragment(private val settings: Settings): Fragment(R.layout.fragment_settings), AdapterView.OnItemSelectedListener{

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private var selectedCabinet: Cabinet? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spinnerAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinnerAdapter.addAll(UnitType.kpl.listVolumeOptions().map { it.second })
        setUpUi(view)
    }

    private fun setUpUi(view: View){
        with(view) {
            val spinner = findViewById<Spinner>(R.id.spinnerSelectUnit)
            spinner.adapter = spinnerAdapter
            spinner.onItemSelectedListener = this@SettingsFragment
            val l = UnitType.kpl.listVolumeOptions()
            spinner.setSelection(l.indexOf(l.find{ it.first == settings.prefUnit}))
            val switch = findViewById<SwitchCompat>(R.id.switchPreferAlko)
            switch.isChecked = settings.prefAlko
            switch.setOnCheckedChangeListener { compoundButton, b ->
                settings.prefAlko = b
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        settings.prefUnit = UnitType.kpl.listVolumeOptions()[p2].first
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}