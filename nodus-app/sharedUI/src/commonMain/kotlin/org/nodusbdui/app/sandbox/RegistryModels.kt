package org.nodusbdui.app.sandbox

import dev.nodus.bdui.runtime.api.BduiSchemaPayload
import dev.nodus.bdui.runtime.model.BduiRegistryContext

data class RegistryItem(
    val id: String,
    val label: String,
)

data class ScreenDescriptor(
    val id: String,
    val label: String,
    val schemaVersion: String = "v0_2",
)

interface RegistryGateway {
    suspend fun listProjects(): List<RegistryItem>
    suspend fun listContracts(projectId: String): List<RegistryItem>
    suspend fun listVersions(projectId: String, contractId: String): List<RegistryItem>
    suspend fun listScreens(projectId: String, contractId: String, versionId: String): List<ScreenDescriptor>
    suspend fun loadScreenPayload(context: BduiRegistryContext): BduiSchemaPayload

    fun buildContext(projectId: String, contractId: String, versionId: String, screenId: String): BduiRegistryContext {
        return BduiRegistryContext(
            projectId = projectId,
            contractId = contractId,
            versionId = versionId,
            screenId = screenId,
        )
    }
}
