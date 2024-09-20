package com.rannasta_suomeen.data_classes

data class ShoppingCartItem(
    val product: Product,
    var amount: Double,
){
    fun price(): Double{
        return product.price * amount
    }
    fun volume(desiredUnit: UnitType):Double{
        return UnitType.Cl.convert(this.product.volumeCl()*this.amount,desiredUnit)
    }
}
