package com.rannasta_suomeen.main_fragments.cabinet_fragments

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.MixerAdapter
import com.rannasta_suomeen.addSimpleOnTextChangeLister

class CabinetMixersFragment(private val adapter: MixerAdapter): Fragment(R.layout.view_cabinet_mixers) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(view){
            val recycler = findViewById<RecyclerView>(R.id.recyclerViewMixers)
            recycler.adapter = adapter
            recycler.layoutManager = LinearLayoutManager(this.context)
            recycler.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
            val searchBox = findViewById<EditText>(R.id.editTextMixerSearch)
            searchBox.addSimpleOnTextChangeLister {
                adapter.submitNewSearch(it)
            }
        }
        super.onViewCreated(view, savedInstanceState)
    }
}