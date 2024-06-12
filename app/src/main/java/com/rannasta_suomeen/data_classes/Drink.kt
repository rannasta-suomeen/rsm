package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

/**
 This is the equivalent of the RS-Struct Recipe
 */

@Suppress("SpellCheckingInspection")
data class DrinkInfo(
    val id: Int,
    val type: DrinkType,

    val author_id: Int,
    val name: String,
    val info: String,

    val recipe_id: Int,

    val alko_price_per_serving: Double,
    val superalko_price_per_serving: Double,

    val alko_aer: Double,
    val superalko_aer: Double,

    /** Volume of the drink in ml*/
    val total_volume : Int,
    val standard_servings: Double,
    val price_per_serving: Double,

    val abv_average: Double,
    val abv_max: Double,
    val abv_min: Double,

    val available_alko: Boolean,
    val alko_price_max: Double,
    val alko_price_min: Double,
    val alko_price_average: Double,

    val available_superalko: Boolean,
    val superalko_price_max: Double,
    val superalko_price_min: Double,
    val superalko_price_average: Double,

    val incredient_count: Int,
    val favorite_count: Int

){

    enum class SortTypes{
        Name, Type, Volume, Price, Fsd, Pps, Abv
    }

    fun price(): Double{
        return when (Settings.prefAlko){
            true -> {
                when (available_alko) {
                    true -> alko_price_average
                    false -> superalko_price_average
                }
            }
            false ->{
                when (available_superalko){
                    true -> superalko_price_average
                    false -> alko_price_average
                }
            }
        }
    }

    fun pricePerServing(): Double{
        return price()/standard_servings
    }
}

/**
 * Class to represent a drink with all of its information
 */
data class DrinkTotal(val drink: DrinkInfo, val ingredients: IngredientsForDrinkPointer){
    // TODO: Change to hashmaps for faster performace
    fun missingIngredients(inventory: List<InventoryItem>,helper:  HashMap<Int, IngredientProductFilter>): Int{
        return ingredients.recipeParts.fold(0) { acc, x ->
            when (inventory.find { helper[x.ingredient.id]?.product_ids?.contains(it.product_id) == true } != null){
                true -> acc
                false -> acc + 1
            }
        }
    }
}

fun sortDrinkPreview(list: List<DrinkTotal>, type: DrinkInfo.SortTypes, asc: Boolean): List<DrinkTotal>{
    var sortedAsc = when (type){
        DrinkInfo.SortTypes.Name -> list.sortedBy { it.drink.name }
        DrinkInfo.SortTypes.Type -> list.sortedBy { it.drink.type }
        DrinkInfo.SortTypes.Volume -> list.sortedBy { it.drink.total_volume }
        DrinkInfo.SortTypes.Price -> list.sortedBy { it.drink.price() }
        DrinkInfo.SortTypes.Fsd -> list.sortedBy { it.drink.standard_servings }
        DrinkInfo.SortTypes.Pps -> list.sortedBy { it.drink.pricePerServing() }
        DrinkInfo.SortTypes.Abv -> list.sortedBy { it.drink.abv_average }
    }

    if (!asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class DrinkType {
    cocktail, shot, punch
}


