package com.rannasta_suomeen.data_classes

import com.rannasta_suomeen.storage.Settings

class RandomizerItem(
    val drinkTotal: DrinkTotal,
    val hidden: Boolean,
    val multiplier: Double = 1.0,
) {
    fun requiredIngredients(): List<IngredientsForDrinkPointer.RecipePartPointer>{
        return drinkTotal.ingredients.recipeParts.map {it.multipliedBy(multiplier)}
    }
}

class RandomizerList(
    val items: List<RandomizerItem>
){
    fun price(settings: Settings):Double {
        return items.fold(0.0){acc, x ->
            acc + x.drinkTotal.drink.price(settings) * x.multiplier
        }
    }

    fun volume():Double{
        return items.fold(0.0){acc, x ->
            acc + x.drinkTotal.drink.volume * x.multiplier
        }
    }

    fun fsd():Double{
        return items.fold(0.0){acc, x->
            acc + x.drinkTotal.drink.standardServings * x.multiplier
        }
    }
    fun aer(settings: Settings):Double{
        return price(settings)/fsd()
    }
    fun requiredIngredients(): List<IngredientsForDrinkPointer.RecipePartPointer>{
        val map: HashMap<Int, Pair<IngredientsForDrinkPointer.RecipePartPointer, Double>> = hashMapOf()
        items.forEach { randomizerItem ->
            val ings = randomizerItem.requiredIngredients()
            ings.forEach {
                try {
                    val old = map.getOrDefault(it.ingredient.id,Pair(it,0.0))
                    val newAmount = old.second + it.unit.convert(it.amount*randomizerItem.multiplier,old.first.unit)
                    map[it.ingredient.id] = Pair(it,newAmount)
                    // This catch is there in case someone has inserted nonsensical drinks into the database
                } catch (_ : IllegalArgumentException){}

            }
        }
        return map.map {
            val ingredient = it.value.first
            val amount = it.value.second
            IngredientsForDrinkPointer.RecipePartPointer(ingredient.ingredient, amount.toInt(), ingredient.name,ingredient.unit)
        }
    }
    fun requiredAlcoholic(): List<IngredientsForDrinkPointer.RecipePartPointer>{
        return requiredIngredients().filter { it.ingredient.type == IngredientType.LightAlcoholProduct || it.ingredient.type == IngredientType.StrongAlcoholProduct }
    }
    fun requiredMixers(): List<IngredientsForDrinkPointer.RecipePartPointer>{
        return requiredIngredients().filter { it.ingredient.type != IngredientType.LightAlcoholProduct && it.ingredient.type != IngredientType.StrongAlcoholProduct }
    }
}