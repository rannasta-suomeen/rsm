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
    val superalko_product_count: Int,
    val use_static_filter: Boolean,
    val static_filter: Int?
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
}

enum class IngredientType{
    light_alcohol_product,
    strong_alcohol_product,
    common,
    mixer,
    grocery,
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
    ){
        fun price(s: Settings): Double{
            return when (unit){
                UnitType.kpl -> amount*ingredient.price(s)
                else -> ingredient.price(s)*unit.convert(amount,UnitType.cl)/100
            }

        }
    }
}

enum class UnitType{
    cl, ml, oz, kpl;

    fun convert(amount: Int, newUnit: UnitType): Double{
        val ml = this.convertToMl(amount.toDouble())
        return newUnit.convertFromMl(ml)
    }

    fun convert(amount: Double, newUnit: UnitType): Double{
        val ml = this.convertToMl(amount)
        return newUnit.convertFromMl(ml)
    }

    fun displayInDesiredUnit(amount: Int, desiredUnit: UnitType): String{
        if (this == kpl) {
            return "$amount kpl"
        }
        val converted = this.convert(amount, desiredUnit)
        val unit = when (desiredUnit){
            cl -> "cl"
            ml -> "ml"
            oz -> "oz"
            kpl -> throw IllegalArgumentException("Unreachable")
        }
        return String.format("%.1f $unit", converted)
    }

    private fun convertToMl(amount: Double): Double{
        return when (this){
            cl -> amount*10
            ml -> amount
            oz -> 29.57352956*amount
            kpl -> throw IllegalArgumentException("Cant convert Kpl to ml")
        }
    }

    private fun convertFromMl(amount: Double): Double{
        return when (this){
            cl -> amount / 10
            ml -> amount
            oz -> 0.03381402270*amount
            kpl -> throw IllegalArgumentException("Cant convert ml to kpl")
        }
    }
}

class IngredientProductFilter(
    val ingredient_id: Int,
    val product_ids: Array<Int>,
){
    fun toPointer(ingredientList: HashMap<Int, GeneralIngredient>,productList: HashMap<Int, Product>): IngredientProductFilterPointer?{
        val ingredient = ingredientList[ingredient_id]
        val products = product_ids.map{ productList[it] }
        if (ingredient != null && !products.contains(null)){
            return IngredientProductFilterPointer(ingredient, products.map { it!! })
        }
        return null
    }
}

class IngredientProductFilterPointer(
    val ingredient: GeneralIngredient,
    val products: List<Product>,
)