package dev.nodus.parser.model

sealed interface BduiAction {
    val type: String
}

data class BduiLogAction(val value: String) : BduiAction {
    override val type: String = "log"
}

data class BduiOpenUrlAction(val url: String) : BduiAction {
    override val type: String = "open_url"
}

data class BduiNavigateAction(val route: String) : BduiAction {
    override val type: String = "navigate"
}

data class BduiUnknownAction(
    override val type: String,
    val raw: Map<String, String> = emptyMap(),
) : BduiAction
