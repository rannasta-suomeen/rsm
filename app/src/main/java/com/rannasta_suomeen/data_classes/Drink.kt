package com.rannasta_suomeen.data_classes

import com.fasterxml.jackson.annotation.JsonProperty
import com.rannasta_suomeen.storage.Settings
import java.util.*

/**
 This is the equivalent of the RS-Struct Recipe
 */

data class DrinkInfo(
    val id: Int,
    val type: DrinkType,
    @JsonProperty("recipe_id")
    val recipeId: Int,

    @JsonProperty("author_id")
    val authorId: Int,
    val name: String,
    val info: String,

    @JsonProperty("tag_list")
    val tagList: List<String>,

    @JsonProperty("alko_price_per_serving")
    val alkoPps: Double,
    @JsonProperty("superalko_price_per_serving")
    val superalkoPps: Double,

    @JsonProperty("alko_aer")
    val alkoAer: Double,
    @JsonProperty("superalko_aer")
    val superalkoAer: Double,

    /// Volume of the drink in ml
    @JsonProperty("total_volume")
    val volume: Double,
    @JsonProperty("standard_servings")
    val standardServings: Double,

    @JsonProperty("abv_average")
    val abvAvg: Double,
    @JsonProperty("abv_max")
    val abvMax: Double,
    @JsonProperty("abv_min")
    val abvMin: Double,

    @JsonProperty("available_alko")
    val availableAlko: Boolean,
    @JsonProperty("alko_price_max")
    val alkoPriceMax: Double,
    @JsonProperty("alko_price_min")
    val alkoPriceMin: Double,
    @JsonProperty("alko_price_average")
    val alkoPriceAverage: Double,

    @JsonProperty("available_superalko")
    val availableSuperalko: Boolean,
    @JsonProperty("superalko_price_max")
    val superalkoPriceMax: Double,
    @JsonProperty("superalko_price_min")
    val superalkoPriceMin: Double,
    @JsonProperty("superalko_price_average")
    val superalkoPriceAverage: Double,

    @JsonProperty("incredient_count")
    val ingredientCount: Int,
    @JsonProperty("favorite_count")
    val favoriteCount: Int

){

    // TODO: Make this show abv in your cabinet
    fun abv():Double{
        return abvAvg
    }
    enum class SortTypes{
        Name, Type, Volume, Price, Fsd, Pps, Abv
    }

    fun displayTagList(): String{
        return tagList.fold("") { r, t ->
            if (t != "") {
                return@fold "$r#$t\n"
            }
            ""
        }.trim()
    }

    fun price(settings: Settings): Double{
        return when (settings.prefAlko){
            true -> {
                when (availableAlko) {
                    true -> alkoPriceAverage
                    false -> superalkoPriceAverage
                }
            }
            false -> {
                when (availableSuperalko) {
                    true -> superalkoPriceAverage
                    false -> alkoPriceAverage
                }
            }
        }
    }

    fun pricePerServing(settings: Settings): Double{
        return price(settings) / standardServings
    }

}

/**
 * Class to represent a drink with all of its information
 */
data class DrinkTotal(val drink: DrinkInfo, val ingredients: IngredientsForDrinkPointer){

    fun amountOfMissingIngredientsAlcoholic(owned: TreeMap<Int,GeneralIngredient>): Int{
        return missingIngredients(owned).count { it.type == IngredientType.StrongAlcoholProduct || it.type == IngredientType.LightAlcoholProduct }
    }

    fun amountOfMissingIngredientsNonAlcoholic(owned: TreeMap<Int,GeneralIngredient>): Int{
        return missingIngredients(owned).count { it.type != IngredientType.StrongAlcoholProduct && it.type != IngredientType.LightAlcoholProduct }
    }

    private fun missingIngredients(owned: TreeMap<Int,GeneralIngredient>): List<GeneralIngredient>{
        return ingredients.recipeParts.map { it.ingredient }.filter { !owned.containsKey(it.id) }
    }

    fun canMakeAlcoholic(owned: TreeMap<Int,GeneralIngredient>): Boolean{
        return amountOfMissingIngredientsAlcoholic(owned) == 0
    }
    fun canMakeNonAlcoholic(owned: TreeMap<Int,GeneralIngredient>): Boolean{
        return amountOfMissingIngredientsNonAlcoholic(owned) == 0
    }
    fun canMakeStrict(owned: TreeMap<Int,GeneralIngredient>):Boolean{
        return canMakeAlcoholic(owned) && canMakeNonAlcoholic(owned)
    }

    fun isMissing(owned: TreeMap<Int,GeneralIngredient>, ingredient: GeneralIngredient): Boolean{
        return missingIngredients(owned).contains(ingredient)
    }

    fun contains(ingredient: GeneralIngredient): Boolean{
        return ingredients.recipeParts.map { it.ingredient }.contains(ingredient)
    }

    fun isMissingButHasAlcoholic(owned: TreeMap<Int,GeneralIngredient>, ingredient: GeneralIngredient): Boolean{
        return missingIngredients(owned).contains(ingredient) && canMakeAlcoholic(owned)
    }

    fun isMissingOnly(owned: TreeMap<Int,GeneralIngredient>, ingredient: GeneralIngredient): Boolean{
        val missing = missingIngredients(owned)
        return missing.size == 1 && missing.contains(ingredient)
    }

    fun isMissingOnlyOrHas(owned: TreeMap<Int,GeneralIngredient>, ingredient: GeneralIngredient): Boolean{
        val missing = missingIngredients(owned)
        return (missing.size == 1 && missing.contains(ingredient)) || (contains(ingredient) && missing.isEmpty())
    }

    fun isMissingOrOwnsAndHasAlcoholic(owned: TreeMap<Int,GeneralIngredient>, ingredient: GeneralIngredient): Boolean{
        return contains(ingredient) && canMakeAlcoholic(owned)
    }
}

fun sortDrinkPreview(list: List<DrinkTotal>, type: DrinkInfo.SortTypes, asc: Boolean, settings: Settings): List<DrinkTotal>{
    var sortedAsc = when (type){
        DrinkInfo.SortTypes.Name -> list.sortedBy { it.drink.name }
        DrinkInfo.SortTypes.Type -> list.sortedBy { it.drink.type }
        DrinkInfo.SortTypes.Volume -> list.sortedBy { it.drink.volume }
        DrinkInfo.SortTypes.Price -> list.sortedBy { it.drink.price(settings) }
        DrinkInfo.SortTypes.Fsd -> list.sortedBy { it.drink.standardServings }
        DrinkInfo.SortTypes.Pps -> list.sortedBy { it.drink.pricePerServing(settings) }
        DrinkInfo.SortTypes.Abv -> list.sortedBy { it.drink.abvAvg }
    }

    if (!asc){sortedAsc = sortedAsc.reversed()}
    return sortedAsc
}

enum class DrinkType {
    @JsonProperty("cocktail")
    Cocktail,

    @JsonProperty("shot")
    Shot,

    @JsonProperty("punch")
    Punch,

    @JsonProperty("generated")
    Generated
}


