package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.adapters.NameableItem

data class ShoppingCartItem(
    val product: Product,
    var amount: Int,
){
    fun price(): Double{
        return product.price * amount
    }
    fun volume(desiredUnit: UnitType):Double{
        return UnitType.Cl.convert(this.product.volumeCl()*this.amount,desiredUnit)
    }
    fun amountOfShots():Double{
        return volume(UnitType.Cl)*product.abv/160
    }
}

data class ShoppingCartMixer(
    val mixer: GeneralIngredient,
    /** Amout of mixer in the unit the [GeneralIngredient] uses
     *
     */
    var amount: Int?,
): NameableItem(){
    override val name: String = mixer.name
    fun price(): Double{
        return mixer.alko_price_average * (mixer.unit.convert(amount?:0, UnitType.L))
    }
}
