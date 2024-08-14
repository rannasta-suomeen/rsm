package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

/**
 This is the equivalent of the RS-Struct Recipe
 */

@Suppress("SpellCheckingInspection")
data class DrinkInfo(
    val id: Int,
    val type: DrinkType,
    val recipe_id: Int,

    val author_id: Int,
    val name: String,
    val info: String,

    val tag_list: List<String>,

    val alko_price_per_serving: Double,
    val superalko_price_per_serving: Double,

    val alko_aer: Double,
    val superalko_aer: Double,

    /** Volume of the drink in ml*/
    val total_volume : Double,
    val standard_servings: Double,

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

    // TODO: Make this show abv in your cabinet
    fun abv():Double{
        return abv_average
    }
    enum class SortTypes{
        Name, Type, Volume, Price, Fsd, Pps, Abv
    }

    fun displayTagList(): String{
        return tag_list.fold(""){r, t ->
            if (t != ""){
                return@fold "$r#$t\n"
            }
            ""
        }.trim()
    }

    fun price(settings: Settings): Double{
        return when (settings.prefAlko){
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

    fun pricePerServing(settings: Settings): Double{
        return price(settings)/standard_servings
    }

}

/**
 * Class to represent a drink with all of its information
 */
data class DrinkTotal(val drink: DrinkInfo, val ingredients: IngredientsForDrinkPointer){

    fun missingIngredientsAlcoholic(owned: List<GeneralIngredient>): Int{
        return ingredients.recipeParts.map{ it.ingredient }.filter { it.type == IngredientType.light_alcohol_product || it.type == IngredientType.strong_alcohol_product }.count {
            !owned.contains(it)
        }
    }

    fun missingIngredientsNonAlcoholic(owned: List<GeneralIngredient>): Int{
        return ingredients.recipeParts.map{ it.ingredient }.filter { it.type != IngredientType.light_alcohol_product && it.type != IngredientType.strong_alcohol_product }.count {
            !owned.contains(it)
        }
    }
}

fun sortDrinkPreview(list: List<DrinkTotal>, type: DrinkInfo.SortTypes, asc: Boolean, settings: Settings): List<DrinkTotal>{
    var sortedAsc = when (type){
        DrinkInfo.SortTypes.Name -> list.sortedBy { it.drink.name }
        DrinkInfo.SortTypes.Type -> list.sortedBy { it.drink.type }
        DrinkInfo.SortTypes.Volume -> list.sortedBy { it.drink.total_volume }
        DrinkInfo.SortTypes.Price -> list.sortedBy { it.drink.price(settings) }
        DrinkInfo.SortTypes.Fsd -> list.sortedBy { it.drink.standard_servings }
        DrinkInfo.SortTypes.Pps -> list.sortedBy { it.drink.pricePerServing(settings) }
        DrinkInfo.SortTypes.Abv -> list.sortedBy { it.drink.abv_average }
    }

    if (!asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class DrinkType {
    cocktail, shot, punch, generated
}


