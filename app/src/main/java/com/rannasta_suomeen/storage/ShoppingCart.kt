package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rannasta_suomeen.data_classes.ShoppingCartItem
import com.rannasta_suomeen.data_classes.UnitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantReadWriteLock

const val SHOPPING_CART_FILE = "shoppingCart"
class ShoppingCart(context: Context) {
    private val items: MutableList<ShoppingCartItem> = mutableListOf()
    private val file: File =  File(context.filesDir, SHOPPING_CART_FILE)
    private val jackson = jacksonObjectMapper()
    private val rwLock = ReentrantReadWriteLock()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        try{
            rwLock.readLock().lock()
            val t: Array<ShoppingCartItem> = jackson.readValue(file.readText(),Array<ShoppingCartItem>::class.java)
            Log.d("Storage", "Loaded ${t.size}")
            items.addAll(t)
        } catch (_: FileNotFoundException){}
        catch (e: MissingKotlinParameterException){
            Log.d("Storage","Failed json parse")
        }
        finally {
            rwLock.readLock().unlock()
        }
    }

    fun addItem(x: ShoppingCartItem){
        scope.launch {
        rwLock.writeLock().lock()
        val item = items.find { it.product == x.product }
        if (item != null){
            item.amount = item.amount.plus(x.amount)
        } else {
            items.add(x)
        }
            file.writeText(jackson.writeValueAsString(items))
            rwLock.writeLock().unlock()
        }
    }

    fun removeItem(x: ShoppingCartItem){
        scope.launch {
        rwLock.writeLock().lock()
        items.removeIf {
            it == x
        }
            file.writeText(jackson.writeValueAsString(items))
            rwLock.writeLock().unlock()
        }
    }

    fun getItems(): List<ShoppingCartItem>{
        rwLock.readLock().lock()
        val t = items.toList()
        rwLock.readLock().unlock()
        return t
    }

    fun totalPrice(): Double{
        return getItems().sumOf { it.price() }
    }

    fun totalVolume(desiredUnit: UnitType): Double{
        return getItems().sumOf { it.volume(desiredUnit)}
    }

    fun amountOfShots():Double{
        return getItems().sumOf { it.amountOfShots() }
    }

    fun amountOfItems():Int{
        return getItems().sumOf { it.amount }
    }

    fun pps():Double{
        return totalPrice()/amountOfShots()
    }

    fun ppl():Double{
        return totalPrice()/totalVolume(UnitType.L)
    }

    fun removeItemAt(index: Int){
        scope.launch {
        rwLock.writeLock().lock()
        items.removeAt(index)
            file.writeText(jackson.writeValueAsString(items))
            rwLock.writeLock().unlock()
        }
    }
}