package org.nodusbdui.app.sandbox

import dev.nodus.bdui.runtime.api.BduiSchemaPayload
import dev.nodus.bdui.runtime.model.BduiRegistryContext

class FakeRegistryGateway : RegistryGateway {
    private val projects = listOf(
        RegistryItem("demo", "Demo Project"),
        RegistryItem("retail", "Retail Sandbox"),
    )

    private val contracts = mapOf(
        "demo" to listOf(
            RegistryItem("main-contract", "Main Contract"),
            RegistryItem("profile-contract", "Profile Contract"),
        ),
        "retail" to listOf(
            RegistryItem("checkout-contract", "Checkout Contract"),
        ),
    )

    private val versions = mapOf(
        "demo:main-contract" to listOf(
            RegistryItem("v0-1", "v0-1"),
            RegistryItem("v0-2", "v0-2"),
        ),
        "demo:profile-contract" to listOf(
            RegistryItem("v1-preview", "v1-preview"),
        ),
        "retail:checkout-contract" to listOf(
            RegistryItem("2026-03", "2026-03"),
        ),
    )

    private val screens = mapOf(
        "demo:main-contract:v0-2" to listOf(
            ScreenDescriptor("home", "Home form"),
            ScreenDescriptor("links", "Links form"),
        ),
        "demo:profile-contract:v1-preview" to listOf(
            ScreenDescriptor("profile", "Profile form"),
        ),
        "retail:checkout-contract:2026-03" to listOf(
            ScreenDescriptor("checkout", "Checkout form"),
        ),
    )

    private val payloads = mapOf(
        "demo:main-contract:v0-2:home" to """
            {
              "schemaVersion": "v0_2",
              "type": "column",
              "id": "root",
              "layout": {
                "padding": { "top": 8, "right": 8, "bottom": 8, "left": 8 }
              },
              "children": [
                { "type": "text", "id": "title", "value": "Добро пожаловать в Nodus CMP" },
                { "type": "input", "id": "email", "placeholder": "Введите email" },
                { "type": "input", "id": "name", "placeholder": "Введите имя" },
                { "type": "button", "id": "submit", "title": "Сохранить", "action": { "type": "log", "value": "submit pressed" } },
                { "type": "button", "id": "back", "title": "Назад", "action": { "type": "navigate", "route": "back" } }
              ]
            }
        """.trimIndent(),
        "demo:main-contract:v0-2:links" to """
            {
              "schemaVersion": "v0_2",
              "type": "column",
              "id": "links_root",
              "children": [
                { "type": "text", "id": "links_title", "value": "Полезные ссылки" },
                { "type": "button", "id": "docs", "title": "Открыть Nodus", "action": { "type": "open_url", "url": "https://github.com/sshaukat/nodus-bdui-cmp" } },
                { "type": "button", "id": "close", "title": "Вернуться", "action": { "type": "navigate", "route": "back" } }
              ]
            }
        """.trimIndent(),
        "demo:profile-contract:v1-preview:profile" to """
            {
              "schemaVersion": "v0_2",
              "type": "column",
              "id": "profile_root",
              "children": [
                { "type": "text", "id": "profile_title", "value": "Профиль пользователя" },
                { "type": "input", "id": "phone", "placeholder": "Телефон" },
                { "type": "input", "id": "city", "placeholder": "Город" },
                { "type": "button", "id": "save_profile", "title": "Сохранить профиль", "action": { "type": "log", "value": "profile saved" } }
              ]
            }
        """.trimIndent(),
        "retail:checkout-contract:2026-03:checkout" to """
            {
              "schemaVersion": "v0_2",
              "type": "column",
              "id": "checkout_root",
              "children": [
                { "type": "text", "id": "checkout_title", "value": "Checkout sandbox" },
                { "type": "box", "id": "checkout_box", "children": [
                    { "type": "text", "id": "checkout_note", "value": "Проверьте адрес доставки" }
                ]},
                { "type": "input", "id": "address", "placeholder": "Адрес доставки" },
                { "type": "button", "id": "continue", "title": "Продолжить", "action": { "type": "log", "value": "continue checkout" } }
              ]
            }
        """.trimIndent(),
    )

    override suspend fun listProjects(): List<RegistryItem> = projects

    override suspend fun listContracts(projectId: String): List<RegistryItem> = contracts[projectId].orEmpty()

    override suspend fun listVersions(projectId: String, contractId: String): List<RegistryItem> {
        return versions["$projectId:$contractId"].orEmpty()
    }

    override suspend fun listScreens(projectId: String, contractId: String, versionId: String): List<ScreenDescriptor> {
        return screens["$projectId:$contractId:$versionId"].orEmpty()
    }

    override suspend fun loadScreenPayload(context: BduiRegistryContext): BduiSchemaPayload {
        val key = "${context.projectId}:${context.contractId}:${context.versionId}:${context.screenId}"
        val raw = payloads[key] ?: error("No payload configured for $key")
        return BduiSchemaPayload(
            screenId = context.screenId,
            schemaVersion = "v0_2",
            rawJson = raw,
            sourceMetadata = mapOf("gateway" to "fake"),
        )
    }
}
