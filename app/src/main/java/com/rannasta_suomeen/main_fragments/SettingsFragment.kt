package com.rannasta_suomeen.main_fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.storage.Settings

class SettingsFragment(private val settings: Settings): Fragment(R.layout.fragment_settings), AdapterView.OnItemSelectedListener{

    private lateinit var spinnerAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spinnerAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        spinnerAdapter.addAll(UnitType.Kpl.listVolumeOptions().map { it.second })
        setUpUi(view)
    }

    private fun setUpUi(view: View){
        with(view) {
            val spinner = findViewById<Spinner>(R.id.spinnerSelectUnit)
            spinner.adapter = spinnerAdapter
            spinner.onItemSelectedListener = this@SettingsFragment
            val l = UnitType.Kpl.listVolumeOptions()
            spinner.setSelection(l.indexOf(l.find{ it.first == settings.prefUnit}))
            val switch = findViewById<SwitchCompat>(R.id.switchPreferAlko)
            switch.isChecked = settings.prefAlko
            switch.setOnCheckedChangeListener { compoundButton, b ->
                settings.prefAlko = b
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        settings.prefUnit = UnitType.Kpl.listVolumeOptions()[p2].first
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}