package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.FilterMap
import com.rannasta_suomeen.adapters.IngredientFilterAdapter
import com.rannasta_suomeen.addSimpleOnTextChangeLister
import com.rannasta_suomeen.storage.IngredientRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupFilterIngredients(activity: Activity, private val ingredientRepository: IngredientRepository, initial: FilterMap, private val callback: (FilterMap) -> Unit):PopupRsm(activity, R.layout.popup_ingredient_filter, null) {

    val adapter = IngredientFilterAdapter(initial.clone() as FilterMap)

    override fun bind(view: View) {
        with(view){
            val searchText = findViewById<EditText>(R.id.editTextSearchIngredient)
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewIngredientFilter)
            val buttonCancel = findViewById<Button>(R.id.buttonIngredientFilterCancel)
            val buttonOk = findViewById<Button>(R.id.buttonIngredientFilterOk)
            val buttonIncludeAll = findViewById<RadioButton>(R.id.radioButtonIncludeAll)
            val buttonNeutralAll = findViewById<RadioButton>(R.id.radioButtonNeutralAll)
            val buttonExcludeAll = findViewById<RadioButton>(R.id.radioButtonExcludeAll)

            buttonIncludeAll.setOnClickListener {
                adapter.includeAll()
            }

            buttonNeutralAll.setOnClickListener {
                adapter.neutralAll()
            }

            buttonExcludeAll.setOnClickListener {
                adapter.excludeAll()
            }

            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            buttonCancel.setOnClickListener {
                window.dismiss()
            }
            buttonOk.setOnClickListener {
                callback(adapter.getStatus())
                window.dismiss()
            }
            searchText.addSimpleOnTextChangeLister {
                adapter.submitNewSearch(it)
            }
            CoroutineScope(Dispatchers.IO).launch {
                ingredientRepository.dataFlow.collect{
                    adapter.submitItems(it)
                }
            }
        }
    }
}