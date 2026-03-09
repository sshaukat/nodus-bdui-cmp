package org.nodusbdui.app.sandbox

import dev.nodus.bdui.runtime.api.BduiSchemaPayload
import dev.nodus.bdui.runtime.model.BduiRegistryContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class HttpRegistryGateway(
    private val baseUrl: String,
    private val client: HttpClient,
) : RegistryGateway {
    override suspend fun listProjects(): List<RegistryItem> {
        return client.get("$baseUrl/api/projects").body<ListEnvelope<ProjectDto>>().items.map { RegistryItem(it.projectId, it.name) }
    }

    override suspend fun listContracts(projectId: String): List<RegistryItem> {
        return client.get("$baseUrl/api/contracts") {
            parameter("project_id", projectId)
        }.body<ListEnvelope<ContractDto>>().items.map { RegistryItem(it.contractId, it.name) }
    }

    override suspend fun listVersions(projectId: String, contractId: String): List<RegistryItem> {
        return client.get("$baseUrl/api/versions") {
            parameter("project_id", projectId)
            parameter("contract_id", contractId)
        }.body<ListEnvelope<VersionDto>>().items.map { RegistryItem(it.versionId, it.versionId) }
    }

    override suspend fun listScreens(projectId: String, contractId: String, versionId: String): List<ScreenDescriptor> {
        return client.get("$baseUrl/api/screens") {
            parameter("project_id", projectId)
            parameter("contract_id", contractId)
            parameter("version_id", versionId)
        }.body<ListEnvelope<ScreenDto>>().items.map {
            ScreenDescriptor(
                id = it.screenId,
                label = it.name,
                schemaVersion = it.schemaVersion ?: "v0_2",
            )
        }
    }

    override suspend fun loadScreenPayload(context: BduiRegistryContext): BduiSchemaPayload {
        val screens = client.get("$baseUrl/api/screens") {
            parameter("project_id", context.projectId)
            parameter("contract_id", context.contractId)
            parameter("version_id", context.versionId)
        }.body<ListEnvelope<ScreenDto>>().items

        val match = screens.firstOrNull { it.screenId == context.screenId }
            ?: error("Screen ${context.screenId} not found")

        return BduiSchemaPayload(
            screenId = match.screenId,
            schemaVersion = match.schemaVersion ?: "v0_2",
            rawJson = match.contentRaw ?: error("Screen ${match.screenId} has no content_raw"),
            sourceMetadata = mapOf("gateway" to "http", "status" to match.status),
        )
    }
}

@Serializable
private data class ListEnvelope<T>(
    val items: List<T> = emptyList(),
)

@Serializable
private data class ProjectDto(
    @SerialName("project_id") val projectId: String,
    val name: String,
)

@Serializable
private data class ContractDto(
    @SerialName("contract_id") val contractId: String,
    val name: String,
)

@Serializable
private data class VersionDto(
    @SerialName("version_id") val versionId: String,
)

@Serializable
private data class ScreenDto(
    @SerialName("screen_id") val screenId: String,
    val name: String,
    val status: String,
    @SerialName("schema_version") val schemaVersion: String? = null,
    @SerialName("content_raw") val contentRaw: String? = null,
)
