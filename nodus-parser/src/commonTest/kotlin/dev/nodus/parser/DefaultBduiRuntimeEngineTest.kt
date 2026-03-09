package dev.nodus.parser

import dev.nodus.parser.parser.DefaultBduiRuntimeEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultBduiRuntimeEngineTest {
    private val engine = DefaultBduiRuntimeEngine()

    @Test
    fun parsesSimpleSchema() {
        val rawJson = """
            {
              "schemaVersion": "v0_2",
              "type": "column",
              "id": "root",
              "children": [
                { "type": "text", "id": "title", "value": "Hello" },
                { "type": "input", "id": "email", "placeholder": "Email" }
              ]
            }
        """.trimIndent()

        val result = engine.parse(rawJson)

        assertTrue(result.isSuccess)
        assertEquals("v0_2", result.appliedSchemaVersion)
    }

    @Test
    fun reportsDuplicateIds() {
        val rawJson = """
            {
              "type": "column",
              "id": "root",
              "children": [
                { "type": "text", "id": "dup", "value": "One" },
                { "type": "text", "id": "dup", "value": "Two" }
              ]
            }
        """.trimIndent()

        val result = engine.parse(rawJson)

        assertTrue(result.validationErrors.isNotEmpty())
        assertTrue(result.validationErrors.any { it.message.contains("Duplicate node id") })
    }
}
