package com.rannasta_suomeen.data_classes

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.rannasta_suomeen.storage.Settings
import java.util.*
import kotlin.math.roundToInt

/**
 * Class to represent a ingredient in general. This is the kotlin equivalent of the rs class Incredient
 */
data class GeneralIngredient(
    val id: Int,
    val type: IngredientType,
    val author_id: Int,
    val name: String,
    @JsonSerialize(using = CategoryJson.CategorySerializer::class)
    @JsonDeserialize(using = CategoryJson.CategoryDeserializer::class)
    val category: Category?,

    val recipe_id: Int?,

    val abv_average: Double,
    val abv_max: Double,
    val abv_min: Double,

    val alko_price_average: Double,
    val alko_price_max: Double,
    val alko_price_min: Double,

    val superalko_price_average: Double,
    val superalko_price_max: Double,
    val superalko_price_min: Double,

    val alko_product_count: Int,
    val superalko_product_count: Int,
    val use_static_filter: Boolean,
    val use_static_filter_c: Boolean,
    val static_filter: Int?,
    val static_filter_c: Int?,
    val unit: UnitType,
){

    fun price(s: Settings): Double{
        return when (s.prefAlko){
            true -> {
                when (alko_product_count > 0) {
                    true -> (alko_price_average + alko_price_min)/2
                    false -> (superalko_price_average + superalko_price_min)/2
                }
            }
            false ->{
                when (superalko_product_count > 0){
                    true -> (superalko_price_average + superalko_price_min)/2
                    false -> (alko_price_average + alko_price_min)/2
                }
            }
        }
    }

    fun isOwned(owned: TreeMap<Int, CabinetMixer>): Boolean{
        return owned.contains(id)
    }

    private fun amountContained(owned: TreeMap<Int, CabinetMixer>): Optional<Optional<Int>>{
        val t = owned[id]?: return Optional.empty<Optional<Int>>()
        return Optional.of(Optional.ofNullable(t.amount))
    }

    fun showAmount(owned: TreeMap<Int, CabinetMixer>, settings: Settings): String{
        val t = amountContained(owned).get()
        val p = when (t.isPresent){
            true -> t.get()
            false -> null
        }
        val c = p?: return "infinite"
        return unit.displayInDesiredUnit(c.toDouble(), settings.prefUnit)
    }
}

fun List<GeneralIngredient>.toTreemap(): TreeMap<Int, GeneralIngredient>{
    return this.associateBy{it.id}.toSortedMap() as TreeMap<Int, GeneralIngredient>
}

enum class IngredientType {
    @JsonProperty("light_alcohol_product")
    LightAlcoholProduct,

    @JsonProperty("strong_alcohol_product")
    StrongAlcoholProduct,

    @JsonProperty("common")
    Common,

    @JsonProperty("mixer")
    Mixer,

    @JsonProperty("grocery")
    Grocery,
}

/**
 * Kotlin equivalent of the rs class IngredientsForDrink
 */
data class IngredientsForDrink(
    @JsonProperty("recipe_id")
    val recipeId: Int,
    @JsonProperty("recipe_parts")
    val recipeParts: Array<RecipePartNoId>
){
    /**
     * Kotlin equivalent of the rs class RecipePartNoId
     */
    data class RecipePartNoId(
        @JsonProperty("ingredient_id")
        val ingredientId: Int,
        val amount: Int,
        val name: String,
        val unit: UnitType,
    ){
        fun toPointer(generalIngredients: List<GeneralIngredient>): IngredientsForDrinkPointer.RecipePartPointer?{
            val ingredient = generalIngredients.find { it.id == ingredientId }
            return ingredient?.let { IngredientsForDrinkPointer.RecipePartPointer(it, amount, name, unit) }
        }
    }
    fun toPointer(generalIngredients: List<GeneralIngredient>): IngredientsForDrinkPointer?{
        val list = recipeParts.map { it.toPointer(generalIngredients) }
        return if (list.contains(null)){
            null
        } else {
            IngredientsForDrinkPointer(recipeId, list.map { it!! }.toTypedArray())
        }
    }
}

data class IngredientsForDrinkPointer(
    val recipeId: Int,
    val recipeParts: Array<RecipePartPointer>
){
    data class RecipePartPointer(
        val ingredient: GeneralIngredient,
        val amount: Int,
        val name: String,
        val unit: UnitType
    ){
        fun price(s: Settings): Double{
            return when (unit) {
                UnitType.Kpl -> amount * ingredient.price(s)
                else -> ingredient.price(s) * unit.convert(amount, UnitType.Cl) / 100
            }

        }
    }
}

enum class UnitType {
    @JsonProperty("cl")
    Cl,

    @JsonProperty("ml")
    Ml,

    @JsonProperty("oz")
    Oz,

    @JsonProperty("kpl")
    Kpl,

    @JsonProperty("tl")
    Tl,

    @JsonProperty("dash")
    Dash,

    @JsonProperty("l")
    L;

    fun convert(amount: Int, newUnit: UnitType): Double {
        val ml = this.convertToMl(amount.toDouble())
        return newUnit.convertFromMl(ml)
    }

    fun convert(amount: Double, newUnit: UnitType): Double {
        val ml = this.convertToMl(amount)
        return newUnit.convertFromMl(ml)
    }

    fun displayInDesiredUnit(amount: Double, desiredUnit: UnitType): String{
        if (this == Kpl) {
            return String.format("%.1f kpl", amount)
        }
        val converted = this.convert(amount, desiredUnit)
        val unit = when (desiredUnit) {
            Cl -> "cl"
            Ml -> "ml"
            Oz -> "oz"
            Kpl -> throw IllegalArgumentException("Unreachable")
            Tl -> "tl"
            L -> "l"
            Dash -> throw IllegalArgumentException("Unreachable")
        }

        // ml does not need to display decimals
        return if (desiredUnit == Ml) {
            String.format("%d $unit", converted.roundToInt())
        } else {
            String.format("%.1f $unit", converted)
        }
    }

    private fun convertToMl(amount: Double): Double{
        return when (this) {
            Cl -> amount * 10
            Ml -> amount
            Oz -> 29.57352956 * amount
            Kpl -> throw IllegalArgumentException("Cant convert Kpl to ml")
            Tl -> 15 * amount
            Dash -> 0.625 * amount
            L -> amount*1000
        }
    }

    private fun convertFromMl(amount: Double): Double{
        return when (this) {
            Cl -> amount / 10
            Ml -> amount
            Oz -> 0.03381402270 * amount
            Kpl -> throw IllegalArgumentException("Cant convert ml to kpl")
            Tl -> 0.06666666666 * amount
            Dash -> 1.6 * amount
            L -> amount/1000
        }
    }

    fun listVolumeOptions(): List<Pair<UnitType, String>>{
        return listOf(Pair(Cl, "cl"), Pair(Ml, "ml"), Pair(Oz, "oz"), Pair(Tl, "tl"), Pair(L,"l"))
    }
}

class IngredientProductFilter(
    @JsonProperty("ingredient_id")
    val ingredientId: Int,
    @JsonProperty("product_ids")
    val productIds: Array<Int>,
){
    fun toPointer(ingredientList: HashMap<Int, GeneralIngredient>,productList: HashMap<Int, Product>): IngredientProductFilterPointer? {
        val ingredient = ingredientList[ingredientId]
        val products = productIds.map { productList[it] }
        if (ingredient != null && !products.contains(null)) {
            return IngredientProductFilterPointer(ingredient, products.map { it!! })
        }
        return null
    }
}

class IngredientProductFilterPointer(
    val ingredient: GeneralIngredient,
    val products: List<Product>,
)