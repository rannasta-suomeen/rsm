package com.rannasta_suomeen.data_classes

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

enum class Category {
    @JsonProperty("strong_alcohol")
    StrongAlcohol,

    @JsonProperty("wine")
    Wine,

    @JsonProperty("liquer")
    Liquor,

    @JsonProperty("beer")
    Beer,

    @JsonProperty("cider")
    Cider,

    @JsonProperty("long_drink_cocktail")
    LongDrinkCocktail,

    @JsonProperty("drink_mix")
    DrinkMix;

    fun toId(): Int {
        return when (this) {
            StrongAlcohol -> 2
            Wine -> 3
            Liquor -> 4
            Beer -> 5
            Cider -> 6
            LongDrinkCocktail -> 7
            DrinkMix -> 8
        }
    }
}

object CategoryJson{

    private const val CATEGORY_FIELD_NAME = "category"

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
    return when (id) {
        2 -> Category.StrongAlcohol
        3 -> Category.Wine
        4 -> Category.Liquor
        5 -> Category.Beer
        6 -> Category.Cider
        7 -> Category.LongDrinkCocktail
        8 -> Category.DrinkMix
        else -> throw IllegalArgumentException("Cannot build category from $id")
    }
}

enum class Subcategory(val category: Category) {

    Vodka(Category.StrongAlcohol),

    @JsonProperty("Whiskey")
    Whiskey(Category.StrongAlcohol),

    @JsonProperty("rum")
    Rum(Category.StrongAlcohol),

    @JsonProperty("gin")
    Gin(Category.StrongAlcohol),

    @JsonProperty("tequila")
    Tequila(Category.StrongAlcohol),

    @JsonProperty("cognac")
    Cognac(Category.StrongAlcohol),

    @JsonProperty("brandy")
    Brandy(Category.StrongAlcohol),

    @JsonProperty("bitter")
    Bitter(Category.StrongAlcohol),

    @JsonProperty("other_strong_alcohol")
    OtherStrongAlcohol(Category.StrongAlcohol),

    @JsonProperty("red_wine")
    RedWine(Category.Wine),

    @JsonProperty("white_wine")
    WhiteWine(Category.Wine),

    @JsonProperty("rose_wine")
    RoseWine(Category.Wine),

    @JsonProperty("sparkling_wine")
    SparklingWine(Category.Wine),

    @JsonProperty("champagne")
    Champagne(Category.Wine),

    @JsonProperty("strong_wine")
    StrongWine(Category.Wine),

    @JsonProperty("vermouth")
    Vermouth(Category.Wine),

    @JsonProperty("mulled_wine")
    MulledWine(Category.Wine),

    @JsonProperty("liquer")
    Liquor(Category.Liquor),

    @JsonProperty("ground_liquer")
    GroundLiquor(Category.Liquor),

    @JsonProperty("berry_fruit_liqueur")
    BerryFruitLiqueur(Category.Liquor),

    @JsonProperty("cocktail_liqueur")
    CocktailLiqueur(Category.Liquor),

    @JsonProperty("cream_liquer")
    CreamLiquor(Category.Liquor),

    @JsonProperty("lager")
    Lager(Category.Beer),

    @JsonProperty("ale")
    Ale(Category.Beer),

    @JsonProperty("wheat")
    Wheat(Category.Beer),

    @JsonProperty("dark")
    Dark(Category.Beer),

    @JsonProperty("other")
    Other(Category.Beer),

    @JsonProperty("cocktail")
    Cocktail(Category.LongDrinkCocktail),

    @JsonProperty("cider")
    Cider(Category.Cider),

    @JsonProperty("natural_cider")
    NaturalCider(Category.Cider),

    @JsonProperty("long_drink")
    LongDrink(Category.LongDrinkCocktail);


    override fun toString(): String {
        val t = when (this) {
            Other -> "Other beer"
            Dark -> "Dark beer"
            else -> super.toString().replaceFirstChar { it.uppercaseChar() }.replace("_", " ")
        }
        return t
    }
}

fun from(id: Int): Subcategory{
    return when (id) {
        1 -> Subcategory.Tequila
        2 -> Subcategory.OtherStrongAlcohol
        3 -> Subcategory.Bitter
        4 -> Subcategory.Brandy
        5 -> Subcategory.Cognac
        6 -> Subcategory.Gin
        7 -> Subcategory.Rum
        8 -> Subcategory.Whiskey
        9 -> Subcategory.Vodka
        10 -> Subcategory.MulledWine
        11 -> Subcategory.Vermouth
        12 -> Subcategory.StrongWine
        13 -> Subcategory.Champagne
        14 -> Subcategory.SparklingWine
        15 -> Subcategory.WhiteWine
        16 -> Subcategory.RedWine
        17 -> Subcategory.RoseWine
        18 -> Subcategory.BerryFruitLiqueur
        19 -> Subcategory.CreamLiquor
        20 -> Subcategory.CocktailLiqueur
        21 -> Subcategory.GroundLiquor
        22 -> Subcategory.Liquor
        23 -> Subcategory.Other
        24 -> Subcategory.Dark
        25 -> Subcategory.Wheat
        26 -> Subcategory.Ale
        27 -> Subcategory.Lager
        28 -> Subcategory.NaturalCider
        29 -> Subcategory.Cider
        30 -> Subcategory.Cocktail
        31 -> Subcategory.LongDrink

        else -> throw IllegalArgumentException("Cant build category from $id")
    }
}
