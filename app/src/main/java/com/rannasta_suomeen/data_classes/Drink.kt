package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

/**
 This is the equivalent of the RS-Struct Recipe
 */

@Suppress("SpellCheckingInspection")
data class DrinkRecipe(
    val id: Int,
    val type: DrinkType,

    val author_id: Int,
    val name: String,
    val info: String,

    val recipe_id: Int,

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
data class DrinkTotal(val drink: DrinkRecipe, val ingredients: List<DrinkIngredient>)

/**
 * Class to represent an ingredient for a single drink
 * @param amount
 * @param ingredient
 */
data class DrinkIngredient(val amount: Int, val ingredient: GeneralIngredient)

/**
 * Class to represent a ingredient in general
 */
data class GeneralIngredient(
    val type: IngredientType,
    val author_id: Int,
    val name: String,

    val category: Category?,

    /** Volume of the drink in ml*/
    val total_volume : Int,
    val standard_servings: Double,
    val price_per_serving: Double,

    val abv_average: Double,
    val abv_max: Double,
    val abv_min: Double,
)

enum class IngredientType{
    LightAlcoholProduct,
    StrongAlcoholProduct,
    Common,
    Mixer,
    Grocery,
}

fun sortDrinkPreview(list: List<DrinkRecipe>, type: DrinkRecipe.SortTypes, asc: Boolean): List<DrinkRecipe>{
    var sortedAsc = when (type){
        DrinkRecipe.SortTypes.Name -> list.sortedBy { it.name }
        DrinkRecipe.SortTypes.Type -> list.sortedBy { it.type }
        DrinkRecipe.SortTypes.Volume -> list.sortedBy { it.total_volume }
        DrinkRecipe.SortTypes.Price -> list.sortedBy { it.price() }
        DrinkRecipe.SortTypes.Fsd -> list.sortedBy { it.standard_servings }
        DrinkRecipe.SortTypes.Pps -> list.sortedBy { it.pricePerServing() }
        DrinkRecipe.SortTypes.Abv -> list.sortedBy { it.abv_average }
    }

    if (!asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class DrinkType {
    cocktail, shot, punch
}


