package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

/**
 * Class to represent a ingredient in general. This is the kotlin equivalent of the rs class Incredient
 */
data class GeneralIngredient(
    val id: Int,
    val type: IngredientType,
    val author_id: Int,
    val name: String,
    val category: Category?,

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
    val superalko_product_count: Int
){
    val standard_servings = abv_average

    fun price(): Double{
        return when (Settings.prefAlko){
            true -> {
                when (alko_product_count > 0) {
                    true -> alko_price_average
                    false -> superalko_price_average
                }
            }
            false ->{
                when (superalko_product_count > 0){
                    true -> superalko_price_average
                    false -> alko_price_average
                }
            }
        }
    }
}

enum class IngredientType{
    LightAlcoholProduct,
    StrongAlcoholProduct,
    Common,
    Mixer,
    Grocery,
}

/**
 * Kotlin equivalent of the rs class IngredientsForDrink
 */
data class IngredientsForDrink(
    val recipe_id: Int,
    val recipe_parts: Array<RecipePartNoId>
){
    /**
     * Kotlin equivalent of the rs class RecipePartNoId
     */
    data class RecipePartNoId(
        val ingredient_id: Int,
        val amount: Int,
        val name: String,
        val unit: UnitType,
    ){
        fun toPointer(generalIngredients: List<GeneralIngredient>): IngredientsForDrinkPointer.RecipePartPointer?{
            val ingredient = generalIngredients.find { it.id == ingredient_id }
            return ingredient?.let { IngredientsForDrinkPointer.RecipePartPointer(it, amount, name, unit) }
        }
    }
    fun toPointer(generalIngredients: List<GeneralIngredient>): IngredientsForDrinkPointer?{
        val list = recipe_parts.map { it.toPointer(generalIngredients) }
        return if (list.contains(null)){
            null
        } else {
            IngredientsForDrinkPointer(recipe_id, list.map { it!! }.toTypedArray())
        }
    }
}

data class IngredientsForDrinkPointer(
    val recipe_id: Int,
    val recipeParts: Array<RecipePartPointer>
){
    data class RecipePartPointer(
        val ingredient: GeneralIngredient,
        val amount: Int,
        val name: String,
        val unit: UnitType
    )
}

enum class UnitType{
    cl, ml, oz, kpl
}