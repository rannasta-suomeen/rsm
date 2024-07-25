package com.rannasta_suomeen.data_classes

import android.util.Log
import com.rannasta_suomeen.storage.Settings

data class CabinetCompact(
    val id: Int,
    val name: String,
){
    fun toStorable(products: List<CabinetProductCompact>): CabinetStorable{
        return CabinetStorable(id, name, products.toMutableList())
    }
}

data class CabinetProductCompact(
    val product_id: Int,
    var amount_ml: Int?,
    var usable: Boolean,
){
    fun toCabinetProduct(productMap: HashMap<Int, Product>): CabinetProduct?{
        return productMap[product_id]?.let { CabinetProduct(it,amount_ml, usable) }
    }
}

data class CabinetProduct(
    val product: Product,
    var amount_ml: Int?,
    var usable: Boolean,
)

data class CabinetStorable(
    val id:Int,
    val name: String,
    var products: MutableList<CabinetProductCompact>
){
    fun toCabinet(productMap: HashMap<Int, Product>): Cabinet?{
        val products = products.map{ it.toCabinetProduct(productMap) }
        return if (products.contains(null)) null
        else Cabinet(id, name, products.mapNotNull { it })
    }
}

sealed class OwnedAmount{
    abstract fun show(settings: Settings): String
}
class None: OwnedAmount(){
    override fun show(settings: Settings): String {
        return "Not owned"
    }
}
class Infinite: OwnedAmount(){
    override fun show(settings: Settings): String {
        return "Infinite"
    }
}
class Some(val x: Int): OwnedAmount(){
    override fun show(settings: Settings): String {
        Log.d("Owned", "Amount_ml is: $x")
        return UnitType.ml.displayInDesiredUnit(x.toDouble(),settings.prefUnit)
    }
}


data class Cabinet(
    val id: Int,
    val name: String,
    val products: List<CabinetProduct>,
){
    fun owned(x: Product): OwnedAmount{
        val res = products.find {
            it.product == x
        }
        return if (res == null ){
            None()
        }else{
            when(res.amount_ml){
                null -> Infinite()
                else -> Some(res.amount_ml!!)
            }
        }
    }
    fun isOwned(x: Product): Boolean{
        return products.any { it.product == x }
    }
}