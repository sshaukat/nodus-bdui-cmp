package dev.nodus.bdui.runtime.model

enum class BduiDiagnosticSeverity {
    Warning,
    Error,
}

data class BduiParseDiagnostic(
    val severity: BduiDiagnosticSeverity,
    val path: String,
    val message: String,
)

data class BduiRegistryContext(
    val projectId: String,
    val contractId: String,
    val versionId: String,
    val screenId: String,
)

data class BduiSpacing(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f,
)

data class BduiLayout(
    val padding: BduiSpacing = BduiSpacing(),
    val margin: BduiSpacing = BduiSpacing(),
    val width: Float? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val alignment: String? = null,
    val justify: String? = null,
    val distribution: String? = null,
    val alignItems: String? = null,
    val crossAlign: String? = null,
)

sealed interface BduiNode {
    val id: String?
    val visible: Boolean
    val enabled: Boolean
    val layout: BduiLayout?
}

data class BduiColumnNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val children: List<BduiNode> = emptyList(),
) : BduiNode

data class BduiRowNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val children: List<BduiNode> = emptyList(),
) : BduiNode

data class BduiBoxNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val children: List<BduiNode> = emptyList(),
) : BduiNode

data class BduiTextNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val value: String,
) : BduiNode

data class BduiButtonNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val title: String,
    val action: BduiAction? = null,
) : BduiNode

data class BduiInputNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val placeholder: String? = null,
    val value: String? = null,
    val onChange: BduiAction? = null,
) : BduiNode

data class BduiSpacerNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
) : BduiNode

data class BduiUnsupportedNode(
    override val id: String? = null,
    override val visible: Boolean = true,
    override val enabled: Boolean = true,
    override val layout: BduiLayout? = null,
    val sourceType: String,
) : BduiNode
