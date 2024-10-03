package com.rannasta_suomeen.main_fragments.cabinet_fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.CabinetProductAdapter
import com.rannasta_suomeen.adapters.CabinetProductAdapterItemTouchHelper
import com.rannasta_suomeen.addSimpleOnTextChangeLister

class CabinetProductsFragment(
    private val adapter: CabinetProductAdapter
    ): Fragment(R.layout.view_recycler_search) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSearch)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        val helper = CabinetProductAdapterItemTouchHelper(adapter, MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.GREEN),requireContext())
        helper.attachToRecyclerView(recyclerView)
        view.findViewById<EditText>(R.id.editTextSearch).addSimpleOnTextChangeLister {
            adapter.submitNewSearch(it)
        }
        super.onViewCreated(view, savedInstanceState)
    }
}