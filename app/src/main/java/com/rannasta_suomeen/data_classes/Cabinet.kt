package com.rannasta_suomeen.data_classes

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.rannasta_suomeen.NetworkController
import com.rannasta_suomeen.storage.Settings

data class CabinetCompact(
    val id: Int,
    @JsonProperty("owner_id")
    val ownerId: Int,
    val name: String,
    val members: List<CabinetMember>,
    val products: MutableList<CabinetProductCompact>,
    @JsonProperty("access_key")
    val accessKey: String?,
){
    fun toCabinet(productMap: HashMap<Int, Product>): Cabinet?{
        val products = products.map{ it.toCabinetProduct(productMap) }
        return if (products.contains(null)) null
        else Cabinet(id, ownerId, name, members, products.mapNotNull { it },accessKey)
    }

    /**
     * @throws IllegalArgumentException when you are not in said cabinet
     */
    @JsonIgnore
    fun getOwnUserId(): Int{
        return members.find { it.userName == NetworkController.username }?.userId
            ?: throw IllegalArgumentException("A cabinet you hold a reference to does not have yourself as a member")
    }
}

@JsonIgnoreProperties(value = ["cabinet_id", "name", "img", "href", "abv"])
data class CabinetProductCompact(
    val id: Int,
    @JsonProperty("product_id")
    val productId: Int,
    @JsonProperty("owner_id")
    val ownerId: Int,
    @JsonProperty("amount_ml")
    var amountMl: Int?,
    var usable: Boolean,
){
    fun toCabinetProduct(productMap: HashMap<Int, Product>): CabinetProduct?{
        return productMap[productId]?.let { CabinetProduct(id,it,productId,ownerId, amountMl, usable) }
    }
}

data class CabinetProduct(
    val id: Int,
    val product: Product,
    private val productId: Int,
    val ownerId: Int,
    var amountMl: Int?,
    var usable: Boolean,
){
    fun toCompact(): CabinetProductCompact{
        return CabinetProductCompact(id, productId, ownerId, amountMl, usable)
    }
    fun estimatedFsd(): Double{
        return when (amountMl == null){
            true -> product.fsd()
            false -> product.fsd()*amountMl!! / product.volumeMl()
        }
    }
    fun estimatedPrice(): Double{
        return when (amountMl == null){
            true -> product.price
            false -> product.price*amountMl!! / product.volumeMl()
        }
    }
}

@JsonIgnoreProperties(value = ["cabinet_id"])
data class CabinetMember(
    @JsonProperty("user_id")
    val userId: Int,
    @JsonProperty("user_username")
    val userName: String,
)

sealed class OwnedAmount{
    abstract fun show(settings: Settings): String
}

object None : OwnedAmount() {
    override fun show(settings: Settings): String {
        return "Not owned"
    }
}

object Infinite : OwnedAmount() {
    override fun show(settings: Settings): String {
        return "Infinite"
    }
}
class Some(val x: Int): OwnedAmount(){
    override fun show(settings: Settings): String {
        return UnitType.ml.displayInDesiredUnit(x.toDouble(),settings.prefUnit)
    }
}


data class Cabinet(
    val id: Int,
    val ownerId: Int,
    val name: String,
    val members: List<CabinetMember>,
    val products: List<CabinetProduct>,
    val accessKey: String?,
){
    fun toCompact(){
        val productsCompact = products.map { it.toCompact() }
        CabinetCompact(id, ownerId, name, members, productsCompact.toMutableList(), accessKey)
    }

    fun owned(x: Product): OwnedAmount{
        val res = products.find {
            it.product == x
        }
        return if (res == null ){
            None
        }else{
            when(res.amountMl){
                null -> Infinite
                else -> Some(res.amountMl!!)
            }
        }
    }
    fun isOwned(x: Product): Boolean{
        return products.any { it.product == x }
    }
    /**
     * @throws IllegalArgumentException when you are not in said cabinet
     */
    fun getOwnUserId(): Int{
        return members.find { it.userName == NetworkController.username }?.userId
            ?: throw IllegalArgumentException("A cabinet you hold a reference to does not have yourself as a member")
    }
}