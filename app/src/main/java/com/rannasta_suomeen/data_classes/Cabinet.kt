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

sealed class OwnedAmount(protected val owner: ProductOwner?){
    abstract fun show(settings: Settings): String
}

class None : OwnedAmount(null) {
    override fun show(settings: Settings): String {
        return "Not owned"
    }
}

class Infinite(owner: ProductOwner) : OwnedAmount(owner) {
    override fun show(settings: Settings): String {
        return "Infinite" + owner?.show()
    }
}
class Some(val x: Int, owner: ProductOwner): OwnedAmount(owner){
    override fun show(settings: Settings): String {
        return UnitType.ml.displayInDesiredUnit(x.toDouble(),settings.prefUnit) + owner?.show()
    }
}

sealed class ProductOwner(protected val cabinetMember: CabinetMember){
    abstract fun show(): String
    class Owned(cabinetMember: CabinetMember): ProductOwner(cabinetMember){
        override fun show():String {
            return ""
        }
    }
    class Borrowed(cabinetMember: CabinetMember): ProductOwner(cabinetMember){
        override fun show(): String {
            return " *" + cabinetMember.userName
        }
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

    fun containedAmount(x: Product): OwnedAmount{
        val res = products.filter {
            it.product == x
        }
        val prefered = res.find { it.ownerId == getOwnUserId() }?:res.sortedBy { it.amountMl }.reversed().getOrNull(0)
        val owner = prefered?.let {ownedBy(it)}
        return if (prefered == null ){
            None()
        }else{
            when(prefered.amountMl){
                null -> Infinite(owner!!)
                else -> Some(prefered.amountMl!!,owner!!)
            }
        }
    }
    private fun ownedBy(x: CabinetProduct):ProductOwner?{
        val member = members.find {x.ownerId == it.userId}
        member?.let {
            return when(it.userId == getOwnUserId()){
                true -> ProductOwner.Owned(it)
                false -> ProductOwner.Borrowed(it)
            }
        }
        return null
    }

    /**
     * @throws IllegalArgumentException when you are not in said cabinet
     */
    fun getOwnUserId(): Int{
        return members.find { it.userName == NetworkController.username }?.userId
            ?: throw IllegalArgumentException("A cabinet you hold a reference to does not have yourself as a member")
    }
}