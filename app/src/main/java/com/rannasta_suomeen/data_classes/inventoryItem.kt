package com.rannasta_suomeen.data_classes

class InventoryItem (
    val product_id: Int,
    val amount: Double?,
    val unitType: UnitType
){
    fun toPointer(productMap: HashMap<Int, Product>): InventoryItemPointer?{
        val product = productMap[product_id]
        return product?.let { InventoryItemPointer(it, amount, unitType) }
    }
}

class InventoryItemPointer(
    val product: Product,
    val amount: Double?,
    val unitType: UnitType,
)