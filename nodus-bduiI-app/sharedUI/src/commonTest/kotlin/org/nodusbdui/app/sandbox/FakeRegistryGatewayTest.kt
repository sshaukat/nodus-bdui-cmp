package org.nodusbdui.app.sandbox

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeRegistryGatewayTest {
    private val gateway = FakeRegistryGateway()

    @Test
    fun returnsProjectsAndPayload() = runTest {
        val projects = gateway.listProjects()
        assertTrue(projects.isNotEmpty())

        val contracts = gateway.listContracts("demo")
        val versions = gateway.listVersions("demo", "main-contract")
        val screens = gateway.listScreens("demo", "main-contract", "v0-2")
        val payload = gateway.loadScreenPayload(
            gateway.buildContext("demo", "main-contract", "v0-2", "home"),
        )

        assertEquals("demo", projects.first().id)
        assertTrue(contracts.any { it.id == "main-contract" })
        assertTrue(versions.any { it.id == "v0-2" })
        assertTrue(screens.any { it.id == "home" })
        assertTrue(payload.rawJson.contains("\"type\": \"column\""))
    }
}
