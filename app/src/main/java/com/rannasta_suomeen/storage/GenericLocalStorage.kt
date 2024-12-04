package com.rannasta_suomeen.storage

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

abstract class GenericLocalStorage<T>(context: Context, filename: String,type: Class<Array<T>>) {
    private val items: MutableList<T> = mutableListOf()
    private val file: File =  File(context.filesDir, filename)
    private val jackson = jacksonObjectMapper()
    private val rwLock = ReentrantReadWriteLock()
    private val scope = CoroutineScope(Dispatchers.IO)
    val dataFlow = MutableSharedFlow<List<T>>(1)

    init {
        jackson.findAndRegisterModules()
        try{
            rwLock.writeLock().lock()
            val t: Array<T> = jackson.readValue(file.readText(), type)
            Log.d("Storage", "Loaded ${t.size}")
            items.addAll(t)
            scope.launch {
                rwLock.readLock().lock()
                dataFlow.emit(items)
                rwLock.readLock().unlock()
            }
        } catch (_: FileNotFoundException){}
        catch (e: MissingKotlinParameterException){
            Log.d("Storage","Failed json parse")
        }
        finally {
            rwLock.writeLock().unlock()
        }
    }

    suspend fun clear(){
        rwLock.writeLock().withLock {
            items.clear()
            file.delete()
        }
        dataFlow.emit(listOf())
    }

    fun addItem(x: T){
        scope.launch {
            rwLock.writeLock().withLock {
                items.add(x)
                file.writeText(jackson.writeValueAsString(items))
            }
            rwLock.readLock().lock()
            val t = items
            rwLock.readLock().unlock()
            dataFlow.emit(t)
        }
    }

    fun removeItem(x: T){
        scope.launch {
            rwLock.writeLock().withLock {
                items.removeIf {
                    it == x
                }
                file.writeText(jackson.writeValueAsString(items))
            }
            rwLock.readLock().lock()
            val t = items
            rwLock.readLock().unlock()
            dataFlow.emit(t)
        }
    }

    fun getItems(): List<T>{
        rwLock.readLock().lock()
        val t = items.toList()
        rwLock.readLock().unlock()
        return t
    }

    fun removeItemAt(index: Int){
        scope.launch {
            rwLock.writeLock().lock()
            items.removeAt(index)
            dataFlow.emit(items)
            file.writeText(jackson.writeValueAsString(items))
            rwLock.writeLock().unlock()
        }
    }
    fun modifyItem(item: T,function: (T) -> Unit){
        scope.launch {
            rwLock.writeLock().lock()

            items.find { it == item }?.let { function(it)}
            val i = items
            rwLock.writeLock().unlock()
            dataFlow.emit(i)
            file.writeText(jackson.writeValueAsString(items))
        }
    }
}