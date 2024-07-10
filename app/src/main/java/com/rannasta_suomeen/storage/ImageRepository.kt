package com.rannasta_suomeen.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class ImageRepository(private val c: Context) {

    private val imageMap: HashMap<String, ByteArray> = hashMapOf()
    val updateFlow = MutableSharedFlow<String>(0)

    private suspend fun loadFromInternet(url: String): Bitmap{
        val res = NetworkController.tryNTimes(5,url,NetworkController::getImage)
        return when (res.isSuccess){
            true -> {
                val b = res.getOrThrow()
                imageMap[url] = b
                updateFlow.emit(url)
                BitmapFactory.decodeByteArray(b,0,b.size)
            }
            false -> {
                Log.d("Networking", "Error ${res.exceptionOrNull()} occured")
                val drawable = AppCompatResources.getDrawable(c, R.drawable.ic_baseline_wine_bar_24)
                drawable!!.toBitmap()
            }
        }

    }

    fun getFromMemoryOrMiss(url: String): Bitmap?{
        val res = imageMap[url].run { this?.let { BitmapFactory.decodeByteArray(this,0, it.size) }}
        if (res == null){
            CoroutineScope(Dispatchers.IO).launch {
                loadFromInternet(url)
            }
        }
        return res
    }

    suspend fun getImage(url: String): Bitmap{
        return imageMap[url].run { this?.let { BitmapFactory.decodeByteArray(this,0, it.size) } } ?:loadFromInternet(url)
    }
}