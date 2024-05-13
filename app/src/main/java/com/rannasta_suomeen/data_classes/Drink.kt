package com.rannasta_suomeen.data_classes

data class DrinkPreview(
    val name: String,
    val type: DrinkType,
    val volume : Int,
    val abv: Double,
    val price: Double,
){
    fun fsd(): Double{
        return volume*abv/100 / FSD_ETHANOL
    }
    fun aer(): Double{
        return volume*abv/100/price
    }

    companion object {
        const val FSD_ETHANOL = 1.6
    }

    enum class SortTypes{
        Name, Type, Volume, Price, Fsd, Aer, Abv
    }
}

fun SortDrinkPreview(list: List<DrinkPreview>, type: DrinkPreview.SortTypes, Asc: Boolean): List<DrinkPreview>{
    var sortedAsc = when (type){
        DrinkPreview.SortTypes.Name -> list.sortedBy { it.name }
        DrinkPreview.SortTypes.Type -> list.sortedBy { it.type }
        DrinkPreview.SortTypes.Volume -> list.sortedBy { it.volume }
        DrinkPreview.SortTypes.Price -> list.sortedBy { it.price }
        DrinkPreview.SortTypes.Fsd -> list.sortedBy { it.fsd() }
        DrinkPreview.SortTypes.Aer -> list.sortedBy { it.aer() }
        DrinkPreview.SortTypes.Abv -> list.sortedBy { it.abv }
    }

    if (!Asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class DrinkType {
    Cocktail, Shot, Punch
}


