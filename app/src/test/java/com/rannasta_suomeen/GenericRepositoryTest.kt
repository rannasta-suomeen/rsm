package com.rannasta_suomeen

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class GenericRepositoryTest {

    private val jackson = jacksonObjectMapper()
    init {
        jackson.findAndRegisterModules()
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testJackson() = runTest {
        val op1 = NetworkController.CabinetOperation.ModifyCabinetProductAmount(1,2,20)
        val op2 = NetworkController.CabinetOperation.AddItemToCabinet(1,6,null)
        val list = arrayOf<NetworkController.CabinetOperation>(op1, op2)
        val json = jackson.writerFor(NetworkController.CabinetOperation::class.java).writeValueAsString(op1)
        val new = jackson.readerFor(NetworkController.CabinetOperation::class.java).readValue(json, NetworkController.CabinetOperation::class.java)
        assert(op1.timestamp == new.timestamp)
        assert(new is NetworkController.CabinetOperation.ModifyCabinetProductAmount)
        assert(op1.pid == (new as NetworkController.CabinetOperation.ModifyCabinetProductAmount).pid)
        val newJson = jackson.writerFor(Array<NetworkController.CabinetOperation>::class.java).writeValueAsString(list)
        val newList = jackson.readerForListOf(NetworkController.CabinetOperation::class.java).readValue(newJson,Array<NetworkController.CabinetOperation>::class.java)
        assert(list[0].timestamp == newList[0].timestamp)
    }
}