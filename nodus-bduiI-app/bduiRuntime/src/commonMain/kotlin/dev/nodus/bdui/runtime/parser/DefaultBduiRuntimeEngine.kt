package dev.nodus.bdui.runtime.parser

import dev.nodus.bdui.runtime.api.BduiActionDispatcher
import dev.nodus.bdui.runtime.api.BduiParseResult
import dev.nodus.bdui.runtime.api.BduiRenderSession
import dev.nodus.bdui.runtime.api.BduiRuntimeEngine
import dev.nodus.bdui.runtime.model.BduiAction
import dev.nodus.bdui.runtime.model.BduiBoxNode
import dev.nodus.bdui.runtime.model.BduiButtonNode
import dev.nodus.bdui.runtime.model.BduiColumnNode
import dev.nodus.bdui.runtime.model.BduiDiagnosticSeverity
import dev.nodus.bdui.runtime.model.BduiInputNode
import dev.nodus.bdui.runtime.model.BduiLayout
import dev.nodus.bdui.runtime.model.BduiLogAction
import dev.nodus.bdui.runtime.model.BduiNavigateAction
import dev.nodus.bdui.runtime.model.BduiNode
import dev.nodus.bdui.runtime.model.BduiOpenUrlAction
import dev.nodus.bdui.runtime.model.BduiParseDiagnostic
import dev.nodus.bdui.runtime.model.BduiRegistryContext
import dev.nodus.bdui.runtime.model.BduiRowNode
import dev.nodus.bdui.runtime.model.BduiSpacing
import dev.nodus.bdui.runtime.model.BduiSpacerNode
import dev.nodus.bdui.runtime.model.BduiTextNode
import dev.nodus.bdui.runtime.model.BduiUnknownAction
import dev.nodus.bdui.runtime.model.BduiUnsupportedNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive

class DefaultBduiRuntimeEngine : BduiRuntimeEngine {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override fun parse(rawJson: String, preferredSchemaVersion: String?): BduiParseResult {
        val decodeErrors = mutableListOf<BduiParseDiagnostic>()
        val validationErrors = mutableListOf<BduiParseDiagnostic>()

        val element = try {
            json.parseToJsonElement(rawJson)
        } catch (error: Throwable) {
            return BduiParseResult(
                node = null,
                decodeErrors = listOf(
                    BduiParseDiagnostic(
                        severity = BduiDiagnosticSeverity.Error,
                        path = "$",
                        message = "Invalid JSON: ${error.message}",
                    )
                ),
                appliedSchemaVersion = preferredSchemaVersion,
            )
        }

        val node = parseNode(element, "$", decodeErrors)
        if (node != null) {
            validationErrors += validateNode(node)
        }

        return BduiParseResult(
            node = node,
            decodeErrors = decodeErrors,
            validationErrors = validationErrors,
            appliedSchemaVersion = preferredSchemaVersion ?: inferSchemaVersion(element),
        )
    }

    override fun createSession(
        context: BduiRegistryContext,
        parseResult: BduiParseResult,
        dispatcher: BduiActionDispatcher,
    ): BduiRenderSession = BduiRenderSession(context, parseResult, dispatcher)

    private fun inferSchemaVersion(element: JsonElement): String? {
        val root = element as? JsonObject ?: return null
        return root["schemaVersion"]?.jsonPrimitive?.contentOrNull
    }

    private fun parseNode(
        element: JsonElement,
        path: String,
        errors: MutableList<BduiParseDiagnostic>,
    ): BduiNode? {
        if (element !is JsonObject) {
            errors += error(path, "Node must be an object")
            return null
        }

        val type = element.string("type")?.lowercase()
        if (type.isNullOrBlank()) {
            errors += error(path, "Node field 'type' is required")
            return null
        }

        val id = element.string("id")
        val visible = element.boolean("visible") ?: true
        val enabled = element.boolean("enabled") ?: true
        val layout = parseLayout(element["layout"])

        return when (type) {
            "column" -> BduiColumnNode(id, visible, enabled, layout, parseChildren(element["children"], "$path.children", errors))
            "row" -> BduiRowNode(id, visible, enabled, layout, parseChildren(element["children"], "$path.children", errors))
            "box" -> BduiBoxNode(id, visible, enabled, layout, parseChildren(element["children"], "$path.children", errors))
            "text" -> {
                val value = element.string("value")
                if (value.isNullOrBlank()) {
                    errors += error(path, "Text node field 'value' is required")
                    null
                } else {
                    BduiTextNode(id, visible, enabled, layout, value)
                }
            }

            "button" -> {
                val title = element.string("title")
                if (title.isNullOrBlank()) {
                    errors += error(path, "Button node field 'title' is required")
                    null
                } else {
                    BduiButtonNode(id, visible, enabled, layout, title, parseAction(element["action"], "$path.action", errors))
                }
            }

            "input" -> BduiInputNode(
                id = id,
                visible = visible,
                enabled = enabled,
                layout = layout,
                placeholder = element.string("placeholder"),
                value = element.string("value"),
                onChange = parseAction(element["onChange"], "$path.onChange", errors),
            )

            "spacer" -> BduiSpacerNode(id, visible, enabled, layout)
            else -> BduiUnsupportedNode(id, visible, enabled, layout, type)
        }
    }

    private fun parseChildren(
        element: JsonElement?,
        path: String,
        errors: MutableList<BduiParseDiagnostic>,
    ): List<BduiNode> {
        if (element == null || element is JsonNull) {
            return emptyList()
        }
        if (element !is JsonArray) {
            errors += error(path, "'children' must be an array")
            return emptyList()
        }
        return element.mapIndexedNotNull { index, child -> parseNode(child, "$path[$index]", errors) }
    }

    private fun parseLayout(element: JsonElement?): BduiLayout? {
        val obj = element as? JsonObject ?: return null
        return BduiLayout(
            padding = parseSpacing(obj["padding"]),
            margin = parseSpacing(obj["margin"]),
            width = obj.float("width"),
            height = obj.float("height"),
            weight = obj.float("weight"),
            alignment = obj.string("alignment"),
            justify = obj.string("justify"),
            distribution = obj.string("distribution"),
            alignItems = obj.string("alignItems"),
            crossAlign = obj.string("crossAlign"),
        )
    }

    private fun parseSpacing(element: JsonElement?): BduiSpacing {
        val obj = element as? JsonObject ?: return BduiSpacing()
        return BduiSpacing(
            top = obj.float("top") ?: 0f,
            right = obj.float("right") ?: 0f,
            bottom = obj.float("bottom") ?: 0f,
            left = obj.float("left") ?: 0f,
        )
    }

    private fun parseAction(
        element: JsonElement?,
        path: String,
        errors: MutableList<BduiParseDiagnostic>,
    ): BduiAction? {
        if (element == null || element is JsonNull) {
            return null
        }
        if (element !is JsonObject) {
            errors += error(path, "Action must be an object")
            return null
        }

        val type = element.string("type")?.lowercase()
        if (type.isNullOrBlank()) {
            errors += error(path, "Action field 'type' is required")
            return null
        }

        return when (type) {
            "log" -> {
                val value = element.string("value")
                if (value.isNullOrBlank()) {
                    errors += error(path, "Log action field 'value' is required")
                    null
                } else {
                    BduiLogAction(value)
                }
            }

            "open_url" -> {
                val url = element.string("url")
                if (url.isNullOrBlank()) {
                    errors += error(path, "open_url action field 'url' is required")
                    null
                } else {
                    BduiOpenUrlAction(url)
                }
            }

            "navigate" -> {
                val route = element.string("route")
                if (route.isNullOrBlank()) {
                    errors += error(path, "navigate action field 'route' is required")
                    null
                } else {
                    BduiNavigateAction(route)
                }
            }

            else -> BduiUnknownAction(type)
        }
    }

    private fun validateNode(node: BduiNode): List<BduiParseDiagnostic> {
        val errors = mutableListOf<BduiParseDiagnostic>()
        val ids = mutableMapOf<String, Int>()

        fun visit(current: BduiNode, path: String) {
            current.id?.let { id ->
                val count = (ids[id] ?: 0) + 1
                ids[id] = count
                if (count > 1) {
                    errors += error(path, "Duplicate node id: $id")
                }
            }

            when (current) {
                is BduiButtonNode -> if (current.title.isBlank()) errors += error(path, "Button title must not be blank")
                is BduiInputNode -> if (current.id.isNullOrBlank()) errors += error(path, "Input node requires non-empty id")
                is BduiTextNode -> if (current.value.isBlank()) errors += error(path, "Text value must not be blank")
                is BduiColumnNode -> current.children.forEachIndexed { index, child -> visit(child, "$path.children[$index]") }
                is BduiRowNode -> current.children.forEachIndexed { index, child -> visit(child, "$path.children[$index]") }
                is BduiBoxNode -> current.children.forEachIndexed { index, child -> visit(child, "$path.children[$index]") }
                else -> Unit
            }
        }

        visit(node, "$")
        return errors
    }

    private fun JsonObject.string(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.boolean(name: String): Boolean? = this[name]?.jsonPrimitive?.booleanOrNull

    private fun JsonObject.float(name: String): Float? = this[name]?.jsonPrimitive?.floatOrNull

    private fun error(path: String, message: String) = BduiParseDiagnostic(
        severity = BduiDiagnosticSeverity.Error,
        path = path,
        message = message,
    )
}
