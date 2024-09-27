package com.rannasta_suomeen.main_fragments.cabinet_fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.rannasta_suomeen.R
import com.rannasta_suomeen.adapters.CabinetProductAdapter
import com.rannasta_suomeen.adapters.CabinetProductAdapterItemTouchHelper

class CabinetProductsFragment(
    private val adapter: CabinetProductAdapter
    ): Fragment(R.layout.view_cabinet_products) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCabinetProducts)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        val helper = CabinetProductAdapterItemTouchHelper(adapter, MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorTertiary, Color.GREEN),requireContext())
        helper.attachToRecyclerView(recyclerView)
        super.onViewCreated(view, savedInstanceState)
    }
}