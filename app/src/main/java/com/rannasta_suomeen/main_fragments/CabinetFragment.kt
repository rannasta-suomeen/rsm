package com.rannasta_suomeen.main_fragments

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.CabinetProductAdapter
import com.rannasta_suomeen.adapters.MixerAdapter
import com.rannasta_suomeen.data_classes.Cabinet
import com.rannasta_suomeen.data_classes.CabinetMixer
import com.rannasta_suomeen.data_classes.GeneralIngredient
import com.rannasta_suomeen.data_classes.IngredientType
import com.rannasta_suomeen.main_fragments.cabinet_fragments.CabinetFragmentFactory
import com.rannasta_suomeen.popup_windows.PopupCabinetShare
import com.rannasta_suomeen.popup_windows.PopupExportProducts
import com.rannasta_suomeen.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.max

class CabinetFragment(
    private val activity: Activity,
    private val imageRepository: ImageRepository,
    private val settings: Settings,
    private val totalCabinetRepository: TotalCabinetRepository,
    private val shoppingCart: ShoppingCart,
    private val totalDrinkRepository: TotalDrinkRepository,
    private val totalIngredientRepository: IngredientRepository): Fragment(R.layout.fragment_cabinets), AdapterView.OnItemSelectedListener{

    private var cabinetList = listOf<Cabinet>()
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var navController: NavController
    private var selectedCabinet: Cabinet? = null
    private lateinit var adapter: CabinetProductAdapter
    private lateinit var mixerAdapter: MixerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CabinetProductAdapter(activity, totalCabinetRepository, imageRepository, settings, shoppingCart)
        mixerAdapter = MixerAdapter(settings, totalDrinkRepository.totalDrinkList)
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
            val fabShare = findViewById<FloatingActionButton>(R.id.fabSharing)
            val fabMove = findViewById<FloatingActionButton>(R.id.fabMove)
            val tabs = findViewById<TabLayout>(R.id.tabsCabinet)
            childFragmentManager.fragmentFactory = CabinetFragmentFactory(adapter, mixerAdapter)
            val navHostFragment = childFragmentManager.findFragmentById(R.id.recyclerHolder) as NavHostFragment
            navController = navHostFragment.findNavController()
            navController.setGraph(R.navigation.nav_cabinet)
            tabs.addOnTabSelectedListener(TabListener(navController))

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
                            onChangeCabinet()
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
                            onChangeCabinet()
                        }
                    }
                }
                launch {
                    totalIngredientRepository.dataFlow.collect{
                        CoroutineScope(Dispatchers.Main).launch {
                            mixerAdapter.submitItems(it.filter { listOf(IngredientType.Mixer, IngredientType.Grocery, IngredientType.Common).contains(it.type) })
                        }
                    }
                }
                launch {
                    totalDrinkRepository.dataFlow.collect{
                        CoroutineScope(Dispatchers.Main).launch {
                            mixerAdapter.submitNewDrinks(it)
                        }
                    }
                }
                totalCabinetRepository.selectedCabinetFlow.collect{
                    selectedCabinet = it
                    onChangeCabinet()
                }
            }
        }
    }

    private fun onChangeCabinet(){
        CoroutineScope(Dispatchers.Main).launch {
            selectedCabinet?.let {
                adapter.submitItems(it.products.sortedBy { it.product.name })
                mixerAdapter.submitNewOwned(it.mixers.associateBy { it.ingredient.id }.toSortedMap() as TreeMap<Int, CabinetMixer>)
                mixerAdapter.submitNewAlcohol(totalCabinetRepository.productsToIngredients(it.products).associateBy { it.id }.toSortedMap() as TreeMap<Int, GeneralIngredient>)
            }
        }
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        val item = spinnerAdapter.getItem(p2)
        totalCabinetRepository.changeSelectedCabinet(cabinetList.find { it.name == item })
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    private class TabListener(private val navController: NavController): TabLayout.OnTabSelectedListener{
        override fun onTabSelected(tab: TabLayout.Tab?) {
            val target = when (tab?.text){
                "Alcoholic" -> R.id.fragmentCabinetProducts
                "Mixers" -> R.id.fragmentCabinetMixers
                else -> {
                    Log.d("Cabinet", "Selected tab with id ${tab?.id}")
                    R.id.fragmentCabinetProducts
                }
            }
            navController.navigate(target)
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabReselected(tab: TabLayout.Tab?) {}

    }
}