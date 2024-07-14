package com.rannasta_suomeen.main_fragments

import android.app.Activity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.rannasta_suomeen.storage.ImageRepository
import com.rannasta_suomeen.storage.Settings
import com.rannasta_suomeen.storage.TotalCabinetRepository

class FragmentFactory(private val activity: Activity, private val imageRepository: ImageRepository, private val settings: Settings, private val totalCabinetRepository: TotalCabinetRepository): FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when(className){
            CabinetFragment::class.java.name -> CabinetFragment(activity,imageRepository, settings, totalCabinetRepository)
            DrinksFragment::class.java.name -> DrinksFragment(activity, settings, totalCabinetRepository)
            ProductsFragment::class.java.name -> ProductsFragment(activity, imageRepository, settings, totalCabinetRepository)
            else -> super.instantiate(classLoader, className)
        }
    }
}