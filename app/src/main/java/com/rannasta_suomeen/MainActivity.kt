package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var navController : NavController

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val target = when(item.itemId){
            R.id.menuMainProducts -> TODO()
            R.id.menuMainCharts -> TODO()
            R.id.menuMainDrinks -> R.id.fragmentDrinks
            R.id.menuMainSettings -> TODO()
            R.id.menuMainStorage -> TODO()
            else -> throw IllegalArgumentException("Attempted to navigate to ${item.itemId} witch is not possible")
        }
        navController.navigate(target)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)

        val navHostFragment=supportFragmentManager.findFragmentById(R.id.navHostFragmentMain) as NavHostFragment
        navController = navHostFragment.findNavController()
        val navView = findViewById<NavigationView>(R.id.navViewMain)
        val toolbar = findViewById<Toolbar>(R.id.toolbarMain)

        toolbar.title = resources.getString(R.string.app_name)

        drawer = findViewById<DrawerLayout>(R.id.drawerLayoutMain)
        val toggle = ActionBarDrawerToggle(this,drawer,toolbar,0,0)

        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navView.bringToFront()

        navView.setNavigationItemSelectedListener(this)
        val nc = NetworkController

        CoroutineScope(Dispatchers.IO).launch {
            nc.login("Jere", "Hasu")
        }
    }

}