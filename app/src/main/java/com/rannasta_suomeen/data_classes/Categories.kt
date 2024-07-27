package com.rannasta_suomeen.data_classes

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

enum class Category{
    strong_alcohol, wine, liquer, beer, cider, long_drink_cocktail, drink_mix;

    fun toId(): Int{
        return when (this){
            strong_alcohol -> 2
            wine -> 3
            liquer -> 4
            beer -> 5
            cider -> 6
            long_drink_cocktail -> 7
            drink_mix -> 8
        }
    }
}

object CategoryJson{

    private val CATEGORY_FIELD_NAME = "category"

    object CategorySerializer : JsonSerializer<Category>(){
        override fun serialize(
            value: Category,
            gen: JsonGenerator,
            serializers: SerializerProvider?
        ) {
            with(gen){
                writeNumberField(CATEGORY_FIELD_NAME, value.toId())
            }
        }
    }

    object CategoryDeserializer : JsonDeserializer<Category?>(){
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Category? {
            val node = p.readValueAsTree<JsonNode>()
            val t = node.get(CATEGORY_FIELD_NAME)
            if (t == null){
                return null
            } else {
                val res = t.asInt()
                return from_id(res)
            }
        }
    }
}

fun from_id(id: Int): Category{
    return when (id){
        2 -> Category.strong_alcohol
        3 -> Category.wine
        4 -> Category.liquer
        5 -> Category.beer
        6 -> Category.cider
        7 -> Category.long_drink_cocktail
        8 -> Category.drink_mix
        else -> throw IllegalArgumentException("Cannot build category from $id")
    }
}

enum class Subcategory(val category: Category){

    Vodka(Category.strong_alcohol),
    whiskey(Category.strong_alcohol),
    rum(Category.strong_alcohol),
    gin(Category.strong_alcohol),
    tequila(Category.strong_alcohol),
    cognac(Category.strong_alcohol),
    brandy(Category.strong_alcohol),
    bitter(Category.strong_alcohol),
    other_strong_alcohol(Category.strong_alcohol),

    red_wine(Category.wine),
    white_wine(Category.wine),
    rose_wine(Category.wine),
    sparkling_wine(Category.wine),
    champagne(Category.wine),
    strong_wine(Category.wine),
    vermouth(Category.wine),
    mulled_wine(Category.wine),

    liquer(Category.liquer),
    ground_liquer(Category.liquer),
    berry_fruit_liqueur(Category.liquer),
    cocktail_liqueur(Category.liquer),
    cream_liquer(Category.liquer),

    lager(Category.beer),
    ale(Category.beer),
    wheat(Category.beer),
    dark(Category.beer),
    other(Category.beer),
    cocktail(Category.long_drink_cocktail),

    cider(Category.cider),
    natural_cider(Category.cider),

    long_drink(Category.long_drink_cocktail);

    override fun toString(): String {
        val t = when (this){
            other -> "Other beer"
            dark -> "Dark beer"
            else -> super.toString().replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
        }
        return t
    }
}

fun from(id: Int): Subcategory{
    return when(id){
        1 -> Subcategory.tequila
        2 -> Subcategory.other_strong_alcohol
        3 -> Subcategory.bitter
        4 -> Subcategory.brandy
        5 -> Subcategory.cognac
        6 -> Subcategory.gin
        7 -> Subcategory.rum
        8 -> Subcategory.whiskey
        9 -> Subcategory.Vodka
        10 -> Subcategory.mulled_wine
        11 -> Subcategory.vermouth
        12 -> Subcategory.strong_wine
        13 -> Subcategory.champagne
        14 -> Subcategory.sparkling_wine
        15 -> Subcategory.white_wine
        16 -> Subcategory.red_wine
        17 -> Subcategory.rose_wine
        18 -> Subcategory.berry_fruit_liqueur
        19 -> Subcategory.cream_liquer
        20 -> Subcategory.cocktail_liqueur
        21 -> Subcategory.ground_liquer
        22 -> Subcategory.liquer
        23 -> Subcategory.other
        24 -> Subcategory.dark
        25 -> Subcategory.wheat
        26 -> Subcategory.ale
        27 -> Subcategory.lager
        28 -> Subcategory.natural_cider
        29 -> Subcategory.cider
        30 -> Subcategory.cocktail
        31 -> Subcategory.long_drink
        else -> throw IllegalArgumentException("Cant build category from $id")
    }
}
