package com.rannasta_suomeen

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class NetworkControllerTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getDrinks() = runTest {
            val res = NetworkController.getDrinks(Unit)
            assertEquals(res.isSuccess, true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getCabinets() = runTest {
            val res = NetworkController.getCabinets(Unit)
            assertEquals(res.isSuccess, true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getCabinetsTotal() = runTest {
        val res = NetworkController.getCabinets(Unit)
            assertEquals(res.isSuccess, true)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getProductIngredientFilter() = runTest{
            val res = NetworkController.getProductIngredientFilter(Unit)
            assertEquals(res.isSuccess, true)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getProducts() = runTest{
            val res = NetworkController.getProducts(Unit)
            assertEquals(res.isSuccess, true)
            assert(res.getOrThrow().size >= 12000)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getDrinkRecipes() = runTest{
            val res = NetworkController.getDrinkRecipes(Unit)
            assertEquals(res.isSuccess, true)
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test

    fun getIngredients() = runTest { 
            val res = NetworkController.getIngredients(Unit)
            assertEquals(res.isSuccess, true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun getImage() = runTest {
        val res =
            NetworkController.getImage("https://viinarannasta.ee/images/media/2024/04/thumbnail1713338952zJ7Xs17210.webp")
        assertEquals(res.isSuccess, true)
    }
}