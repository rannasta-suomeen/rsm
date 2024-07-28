package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

/** Kotlin equivalent of the RSM product */
data class Product(
    val id: Int,
    val name: String,
    val href: String,
    val price: Double,
    val img: String,
    /// Volume of the product in L
    val volume: Double,
    val category_id: Int,
    val subcategory_id: Int,
    val abv: Double,
    val aer: Double,
    val unit_price: Double,
    val checksum: String,
    val retailer: Retailer
){
    enum class SortTypes{
        Name, Price, Volume, Abv, Pps, UnitPrice, Fsd
    }

    fun pps(): Double{
        return price / fsd()
    }

    fun fsd(): Double{
        return volume * abv / FSD_CL
    }

    fun volumeCl(): Double{
        return (volume * 100)
    }

    fun volumeDesired(settings: Settings): String{
        return UnitType.cl.displayInDesiredUnit(volumeCl(), settings.prefUnit)
    }

    companion object{
        const val FSD_CL = 1.6f
    }
}

fun sort(list: List<Product>, type: Product.SortTypes, asc: Boolean): List<Product>{
    var sortedAsc = when(type){
        Product.SortTypes.Name -> list.sortedBy {it.name}
        Product.SortTypes.Price -> list.sortedBy { it.price }
        Product.SortTypes.Volume -> list.sortedBy { it.volume }
        Product.SortTypes.Abv -> list.sortedBy {it.abv}
        Product.SortTypes.Pps -> list.sortedBy { it.pps() }
        Product.SortTypes.UnitPrice -> list.sortedBy { it.unit_price }
        Product.SortTypes.Fsd -> list.sortedBy { it.fsd() }
    }
    if (!asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class Retailer {
    alko, superalko
}
