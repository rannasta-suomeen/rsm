package com.rannasta_suomeen.main_fragments.cabinet_fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.rannasta_suomeen.adapters.CabinetProductAdapter
import com.rannasta_suomeen.adapters.MixerAdapter

class CabinetFragmentFactory(
    private val cabinetProductAdapter: CabinetProductAdapter,
    private val cabinetMixerAdapter: MixerAdapter): FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            CabinetProductsFragment::class.java.name -> CabinetProductsFragment(cabinetProductAdapter)
            CabinetMixersFragment::class.java.name -> CabinetMixersFragment(cabinetMixerAdapter)
            else -> super.instantiate(classLoader, className)
        }
    }
}