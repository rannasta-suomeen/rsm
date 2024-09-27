package com.rannasta_suomeen

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.navigation.NavigationView
import com.rannasta_suomeen.main_fragments.FragmentFactory
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
        supportFragmentManager.fragmentFactory = FragmentFactory(this, imageRepository, settings, totalCabinetRepository, shoppingCart, totalIngredientRepository)
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
        val popUpView = layoutInflater.inflate(R.layout.popup_login, null)
        val popupWindow = PopupWindow(popUpView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        popupWindow.isFocusable = true

        val usernameText = popUpView.findViewById<EditText>(R.id.editTextTextLoginUsername)
        val passwordText = popUpView.findViewById<EditText>(R.id.editTextLoginPassword)
        val rememberSwitch = popUpView.findViewById<SwitchCompat>(R.id.switchLoginRemember)
        val buttonLogin = popUpView.findViewById<Button>(R.id.buttonLogin)

        usernameText.setText(encryptedStorage.userName)
        passwordText.setText(encryptedStorage.password)

        buttonLogin.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                val save = rememberSwitch.isChecked

                val username = usernameText.text.toString()
                val password = passwordText.text.toString()
                encryptedStorage.userName = username
                if (!save) encryptedStorage.password = null
                else encryptedStorage.password = password

                if (password != "" && username != ""){
                    val res = NetworkController.tryNTimes(5, Pair(username, password), NetworkController::login)
                    if (res.isSuccess){
                        runOnUiThread { popupWindow.dismiss() }
                    } else{
                        when(val e = res.exceptionOrNull()){
                            is NetworkController.Error.CredentialsError -> runOnUiThread { Toast.makeText(this@MainActivity.applicationContext, "Username or password is wrong", Toast.LENGTH_LONG).show() }
                            else -> runOnUiThread { Toast.makeText(this@MainActivity.applicationContext, "Error ${e!!.message}", Toast.LENGTH_LONG).show()}
                        }
                    }
                } else{
                    runOnUiThread { Toast.makeText(this@MainActivity.applicationContext, "You must give username and password", Toast.LENGTH_LONG).show()}
                }
            }
        }
        root.post {
            popupWindow.showAtLocation(root, Gravity.TOP,0,0)
        }
    }
}