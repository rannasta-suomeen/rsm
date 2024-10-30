package com.rannasta_suomeen.data_classes

class RandomizerItem(
    val drinkTotal: DrinkTotal,
    val hidden: Boolean,
    val multiplier: Double = 1.0,
) {
    fun requiredIngredients(): List<IngredientsForDrinkPointer.RecipePartPointer>{
        return drinkTotal.ingredients.recipeParts.map {it.multipliedBy(multiplier)}
    }
}