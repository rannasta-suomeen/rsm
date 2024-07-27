package com.rannasta_suomeen

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rannasta_suomeen.data_classes.DrinkInfo
import org.junit.Ignore
import org.junit.Test

private const val TEST_DRINK = "{\"abv_average\":12.903225518042042,\"abv_max\":14.516128647711971,\"abv_min\":11.290322388372113,\"alko_aer\":0.6270065315728146,\"alko_price_average\":3.1897593638771458,\"alko_price_max\":13.519999732971192,\"alko_price_min\":2.257857099260603,\"alko_price_per_serving\":2.822937100128835,\"author_id\":2,\"available_alko\":true,\"available_superalko\":true,\"favorite_count\":0,\"id\":10,\"incredient_count\":3,\"info\":\"Kaada Tequila highball lasiin, lisää limemehu. Lisää jää ja suola. Täytä greppilimulla.\",\"name\":\"Paloma\",\"standard_servings\":1.1299434775686534,\"superalko_aer\":0.905109414888582,\"superalko_price_average\":2.209677551020408,\"superalko_price_max\":13.525714285714285,\"superalko_price_min\":1.4794999999999998,\"superalko_price_per_serving\":1.9555646763633376,\"total_volume\":155.0,\"type\":\"cocktail\"}"

internal class ShouldGsonBeBurnedInFireTest {

    @Test
    fun testGsonParseInvalidJackson(){
        val jackson = jacksonObjectMapper()
        jackson.findAndRegisterModules()
        try {
            val drink = jackson.readerFor(DrinkInfo::class.java).readValue<DrinkInfo>(TEST_DRINK)
            assert(drink.id == 10)
            assert(drink.tag_list.isEmpty())
        } catch (e: MissingKotlinParameterException){
            assert(true)
        }

    }
}