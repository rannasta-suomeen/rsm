package com.rannasta_suomeen.popup_windows

import android.app.Activity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.R
import com.rannasta_suomeen.storage.EncryptedStorage
import com.rannasta_suomeen.storage.ShoppingCart
import com.rannasta_suomeen.totalCabinetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupLogin(activity: Activity, private val encryptedStorage: EncryptedStorage, private val shoppingCart: ShoppingCart): PopupRsm(activity, R.layout.popup_login, null) {
    override fun bind(view: View) {

        val usernameText = view.findViewById<EditText>(R.id.editTextTextLoginUsername)
        val passwordText = view.findViewById<EditText>(R.id.editTextLoginPassword)
        val rememberSwitch = view.findViewById<SwitchCompat>(R.id.switchLoginRemember)
        val buttonLogin = view.findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = view.findViewById<Button>(R.id.buttonRegister)

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
                        CoroutineScope(Dispatchers.Main).launch { window.dismiss() }
                    } else{
                        when(val e = res.exceptionOrNull()){
                            is NetworkController.Error.CredentialsError -> CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, "Username or password is wrong", Toast.LENGTH_LONG).show() }
                            else -> CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, "Error ${e!!.message}", Toast.LENGTH_LONG).show()}
                        }
                    }
                } else{
                    CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, "You must give username and password", Toast.LENGTH_LONG).show()}
                }
            }
        }

        buttonRegister.setOnClickListener {
            encryptedStorage.password = null
            encryptedStorage.userName = null
            NetworkController.logout()
            CoroutineScope(Dispatchers.IO).launch {
                shoppingCart.clear()
                totalCabinetRepository.clear()
            }
            val save = rememberSwitch.isChecked
            val username = usernameText.text.toString()
            val password = passwordText.text.toString()
            if (password != "" && username != ""){
                CoroutineScope(Dispatchers.IO).launch {
                    val res = NetworkController.tryNTimes(5, Pair(username, password), NetworkController::register)
                    if (res.isSuccess){
                        encryptedStorage.password = password
                        if (!save) encryptedStorage.password = null
                        else encryptedStorage.password = password
                        CoroutineScope(Dispatchers.Main).launch { window.dismiss() }
                    } else{
                        fun makeToast(s: String){
                            CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, s, Toast.LENGTH_SHORT).show() }
                        }
                        when(val e = res.exceptionOrNull()){
                            is NetworkController.Error.ConflictError -> {
                                makeToast("Username already taken")
                            }
                            is NetworkController.Error.NetworkError -> {
                                makeToast("No network")
                            }
                            else -> {
                                makeToast(e?.message?:"")
                            }
                        }
                    }
                }
            } else{
                CoroutineScope(Dispatchers.Main).launch { Toast.makeText(activity, "You must give username and password", Toast.LENGTH_LONG).show()}
            }
        }

    }
}