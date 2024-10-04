package com.rannasta_suomeen.main_fragments

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.R
import com.rannasta_suomeen.data_classes.UnitType
import com.rannasta_suomeen.storage.EncryptedStorage
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.storage.TotalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingsFragment(
    private val settings: Settings,
    private val encryptedStorage: EncryptedStorage,
    private val shoppingCart: ShoppingCart,
    private val totalCabinetRepository: TotalCabinetRepository): Fragment(R.layout.fragment_settings), AdapterView.OnItemSelectedListener{

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
            findViewById<Button>(R.id.buttonLogout).setOnClickListener {
                encryptedStorage.password = null
                encryptedStorage.userName = null
                NetworkController.logout()
                CoroutineScope(Dispatchers.IO).launch {
                    shoppingCart.clear()
                    totalCabinetRepository.clear()
                }
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        settings.prefUnit = UnitType.Kpl.listVolumeOptions()[p2].first
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }
}