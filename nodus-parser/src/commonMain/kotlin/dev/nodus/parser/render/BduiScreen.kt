package dev.nodus.parser.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nodus.parser.api.BduiActionMeta
import dev.nodus.parser.api.BduiRenderSession
import dev.nodus.parser.model.BduiAction
import dev.nodus.parser.model.BduiBoxNode
import dev.nodus.parser.model.BduiButtonNode
import dev.nodus.parser.model.BduiColumnNode
import dev.nodus.parser.model.BduiInputNode
import dev.nodus.parser.model.BduiLayout
import dev.nodus.parser.model.BduiLogAction
import dev.nodus.parser.model.BduiNavigateAction
import dev.nodus.parser.model.BduiNode
import dev.nodus.parser.model.BduiOpenUrlAction
import dev.nodus.parser.model.BduiRowNode
import dev.nodus.parser.model.BduiSpacerNode
import dev.nodus.parser.model.BduiTextNode
import dev.nodus.parser.model.BduiUnsupportedNode

@Composable
fun BduiScreen(
    session: BduiRenderSession,
    modifier: Modifier = Modifier,
) {
    val node = session.parseResult.node
    if (node == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("No renderable node")
        }
        return
    }

    RenderNode(
        node = node,
        session = session,
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun RenderNode(
    node: BduiNode,
    session: BduiRenderSession,
    modifier: Modifier = Modifier,
) {
    if (!node.visible) {
        return
    }

    when (node) {
        is BduiColumnNode -> Column(
            modifier = modifier.then(layoutModifier(node.layout)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            node.children.forEach { child ->
                RenderNode(child, session)
            }
        }

        is BduiRowNode -> Row(
            modifier = modifier.then(layoutModifier(node.layout)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            node.children.forEach { child ->
                RenderNode(child, session)
            }
        }

        is BduiBoxNode -> Box(
            modifier = modifier
                .then(layoutModifier(node.layout))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.CenterStart,
        ) {
            node.children.forEach { child ->
                RenderNode(child, session)
            }
        }

        is BduiTextNode -> BasicText(
            text = node.value,
            modifier = modifier.then(layoutModifier(node.layout)),
        )

        is BduiButtonNode -> Button(
            onClick = {
                node.action?.let { action ->
                    session.dispatch(action, toMeta(node.id, action))
                }
            },
            enabled = node.enabled,
            modifier = modifier.then(layoutModifier(node.layout)),
        ) {
            BasicText(node.title)
        }

        is BduiInputNode -> {
            val currentValue by session.fieldValues
            OutlinedTextField(
                value = currentValue[node.id].orEmpty().ifBlank { node.value.orEmpty() },
                onValueChange = { value ->
                    val id = node.id ?: return@OutlinedTextField
                    session.updateField(id, value, node.onChange)
                },
                enabled = node.enabled,
                placeholder = {
                    node.placeholder?.let { BasicText(it) }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .then(layoutModifier(node.layout)),
            )
        }

        is BduiSpacerNode -> Spacer(
            modifier = modifier.then(layoutModifier(node.layout)).size(8.dp),
        )

        is BduiUnsupportedNode -> Box(
            modifier = modifier
                .fillMaxWidth()
                .then(layoutModifier(node.layout))
                .background(Color(0xFFFFF3CD))
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicText(
                text = "Unsupported node: ${node.sourceType}",
            )
        }
    }
}

private fun layoutModifier(layout: BduiLayout?): Modifier {
    if (layout == null) {
        return Modifier
    }

    var modifier: Modifier = Modifier
    modifier = modifier.padding(
        start = (layout.margin.left + layout.padding.left).dp,
        top = (layout.margin.top + layout.padding.top).dp,
        end = (layout.margin.right + layout.padding.right).dp,
        bottom = (layout.margin.bottom + layout.padding.bottom).dp,
    )
    layout.width?.let { modifier = modifier.width(it.dp) }
    return modifier
}

private fun toMeta(sourceId: String?, action: BduiAction): BduiActionMeta = when (action) {
    is BduiNavigateAction -> BduiActionMeta(sourceId = sourceId, routeHint = action.route)
    is BduiOpenUrlAction -> BduiActionMeta(sourceId = sourceId, routeHint = action.url)
    is BduiLogAction -> BduiActionMeta(sourceId = sourceId, payload = mapOf("value" to action.value))
    else -> BduiActionMeta(sourceId = sourceId)
}
