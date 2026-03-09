package dev.nodus.parser.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dev.nodus.parser.model.BduiAction
import dev.nodus.parser.model.BduiNode
import dev.nodus.parser.model.BduiParseDiagnostic
import dev.nodus.parser.model.BduiRegistryContext
import dev.nodus.parser.render.BduiScreen

data class BduiSchemaPayload(
    val screenId: String,
    val schemaVersion: String? = null,
    val rawJson: String,
    val sourceMetadata: Map<String, String> = emptyMap(),
)

data class BduiParseResult(
    val node: BduiNode?,
    val decodeErrors: List<BduiParseDiagnostic> = emptyList(),
    val validationErrors: List<BduiParseDiagnostic> = emptyList(),
    val warnings: List<BduiParseDiagnostic> = emptyList(),
    val appliedSchemaVersion: String? = null,
) {
    val isSuccess: Boolean
        get() = node != null && decodeErrors.isEmpty() && validationErrors.isEmpty()
}

data class BduiActionMeta(
    val sourceId: String? = null,
    val routeHint: String? = null,
    val payload: Map<String, String> = emptyMap(),
)

fun interface BduiActionDispatcher {
    fun dispatch(action: BduiAction, meta: BduiActionMeta)

    companion object {
        val NoOp = BduiActionDispatcher { _, _ -> }
    }
}

fun interface BduiSchemaSource {
    suspend fun load(context: BduiRegistryContext): BduiSchemaPayload
}

interface BduiRuntimeEngine {
    fun parse(rawJson: String, preferredSchemaVersion: String? = null): BduiParseResult

    fun createSession(
        context: BduiRegistryContext,
        parseResult: BduiParseResult,
        dispatcher: BduiActionDispatcher = BduiActionDispatcher.NoOp,
    ): BduiRenderSession
}

object BduiRuntimeMetadata {
    const val moduleName: String = "nodus-parser"
    const val version: String = "0.1.0"
}

class BduiRenderSession(
    val context: BduiRegistryContext,
    val parseResult: BduiParseResult,
    private val dispatcher: BduiActionDispatcher = BduiActionDispatcher.NoOp,
) {
    val fieldValues: MutableState<Map<String, String>> = mutableStateOf(emptyMap())
    val actionLog: MutableState<List<String>> = mutableStateOf(emptyList())

    fun updateField(id: String, value: String, action: BduiAction? = null) {
        fieldValues.value = fieldValues.value + (id to value)
        if (action != null) {
            dispatch(action, BduiActionMeta(sourceId = id, payload = mapOf("value" to value)))
        }
    }

    fun dispatch(action: BduiAction, meta: BduiActionMeta = BduiActionMeta()) {
        val line = buildString {
            append(action.type)
            meta.sourceId?.let {
                append(" from ")
                append(it)
            }
            meta.routeHint?.let {
                append(" -> ")
                append(it)
            }
        }
        actionLog.value = listOf(line) + actionLog.value
        dispatcher.dispatch(action, meta)
    }
}

@Composable
fun RenderBduiScreen(session: BduiRenderSession) {
    BduiScreen(session = session)
}
