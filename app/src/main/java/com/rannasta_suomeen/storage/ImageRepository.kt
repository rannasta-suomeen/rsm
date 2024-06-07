package com.rannasta_suomeen.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.rannasta_suomeen.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class ImageRepository(private val c: Context) {

    private val imageMap: HashMap<String, ByteArray> = hashMapOf()

    private suspend fun loadFromInternetUnsafe(url: String): Bitmap{
        val stream = withContext(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.IO) {
                    URL(url).openConnection()
                }.getInputStream()
            }
        }
        val b = stream.readBytes()
        withContext(Dispatchers.IO) {
            stream.close()
        }
        imageMap[url] = b
        return BitmapFactory.decodeByteArray(b,0,b.size)
    }

    private suspend fun loadFromInternet(url: String): Bitmap{
        for (i in 0..5){
            try {
                return loadFromInternetUnsafe(url)
            } catch (e: IOException){
                continue
            }
        }
        val drawable = AppCompatResources.getDrawable(c, R.drawable.ic_baseline_wine_bar_24)
        return drawable!!.toBitmap()
    }

    suspend fun getImage(url: String): Bitmap{
        return imageMap[url].run { this?.let { BitmapFactory.decodeByteArray(this,0, it.size) } } ?:loadFromInternet(url)
    }
}