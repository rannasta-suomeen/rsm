package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.rannasta_suomeen.main_fragments.FragmentFactory
import com.rannasta_suomeen.popup_windows.PopupLogin
import com.rannasta_suomeen.storage.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout
    private lateinit var navController : NavController
    private lateinit var encryptedStorage: EncryptedStorage
    private lateinit var imageRepository: ImageRepository
    private lateinit var settings: Settings
    private lateinit var shoppingCart: ShoppingCart
    private lateinit var totalIngredientRepository: IngredientRepository

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val target = when(item.itemId){
            R.id.menuMainProducts -> R.id.fragmentProducts
            R.id.menuMainCharts -> R.id.fragmentCharts
            R.id.menuMainDrinks -> R.id.fragmentDrinks
            R.id.menuMainSettings -> R.id.fragmentSettings
            R.id.menuMainStorage -> R.id.fragmentCabinets
            R.id.menuMainCart -> R.id.fragmentCart
            R.id.menuMainRandomizer -> R.id.fragmentRandomizer
            else -> throw IllegalArgumentException("Attempted to navigate to ${item.itemId} witch is not possible")
        }
        navController.navigate(target)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        settings = Settings(this)
        ingredientRepository = IngredientRepository(applicationContext)
        productRepository = ProductRepository(applicationContext)
        totalDrinkRepository = TotalDrinkRepository(applicationContext)
        totalCabinetRepository = TotalCabinetRepository(applicationContext, settings)
        encryptedStorage = EncryptedStorage(applicationContext)
        imageRepository = ImageRepository(applicationContext)
        shoppingCart = ShoppingCart(applicationContext)
        totalIngredientRepository = IngredientRepository(applicationContext)
        val randomizer = Randomizer(applicationContext)
        supportFragmentManager.fragmentFactory = FragmentFactory(this, imageRepository, settings, totalCabinetRepository, shoppingCart, totalIngredientRepository, randomizer ,encryptedStorage)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.layout_activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbarMain)

        CoroutineScope(Dispatchers.IO).launch {
            totalCabinetRepository.selectedCabinetFlow.collect{
                runOnUiThread {
                    toolbar.title = it?.name?:"No cabinet selected"
                }
            }
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragmentMain) as NavHostFragment
        navController = navHostFragment.findNavController()
        navController.setGraph(R.navigation.nav_main)
        val navView = findViewById<NavigationView>(R.id.navViewMain)

        drawer = findViewById<DrawerLayout>(R.id.drawerLayoutMain)
        val toggle = ActionBarDrawerToggle(this,drawer,toolbar,0,0)

        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navView.bringToFront()
        navView.setNavigationItemSelectedListener(this)
        setupToken(toolbar)
    }

    private fun setupToken(root: View){
        CoroutineScope(Dispatchers.IO).launch {
            val userName = encryptedStorage.userName
            val password = encryptedStorage.password
            if (userName != null && password != null) {
                val res = NetworkController.tryNTimes(5, Pair(userName, password), NetworkController::login)
                if (res.isFailure){
                    val t = res.exceptionOrNull()
                    runOnUiThread {
                        Toast.makeText(applicationContext, (t as NetworkController.Error).message, Toast.LENGTH_SHORT).show()
                    }
                    if (t is NetworkController.Error.CredentialsError){
                        runOnUiThread {
                            login(root)
                        }
                    }
                }
            } else{
                runOnUiThread {
                    login(root)
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun login(root: View){
        root.post {
            PopupLogin(this,encryptedStorage, shoppingCart).show()
        }
    }
}