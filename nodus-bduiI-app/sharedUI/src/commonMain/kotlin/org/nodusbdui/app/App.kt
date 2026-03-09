package org.nodusbdui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.nodus.bdui.runtime.api.BduiActionDispatcher
import dev.nodus.bdui.runtime.api.BduiActionMeta
import dev.nodus.bdui.runtime.api.BduiRenderSession
import dev.nodus.bdui.runtime.api.BduiRuntimeMetadata
import dev.nodus.bdui.runtime.model.BduiNavigateAction
import dev.nodus.bdui.runtime.model.BduiOpenUrlAction
import dev.nodus.bdui.runtime.parser.DefaultBduiRuntimeEngine
import dev.nodus.bdui.runtime.render.BduiScreen
import kotlinx.coroutines.launch
import nodus_bdui_app.sharedui.generated.resources.Res
import nodus_bdui_app.sharedui.generated.resources.ic_dark_mode
import nodus_bdui_app.sharedui.generated.resources.ic_light_mode
import org.jetbrains.compose.resources.vectorResource
import org.nodusbdui.app.sandbox.FakeRegistryGateway
import org.nodusbdui.app.sandbox.RegistryGateway
import org.nodusbdui.app.sandbox.RegistryItem
import org.nodusbdui.app.sandbox.ScreenDescriptor
import org.nodusbdui.app.theme.AppTheme
import org.nodusbdui.app.theme.LocalThemeIsDark

private sealed interface SandboxRoute {
    data object Selector : SandboxRoute
    data class Render(val session: BduiRenderSession, val title: String) : SandboxRoute
}

@Preview
@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) = AppTheme(onThemeChanged) {
    val gateway: RegistryGateway = remember { FakeRegistryGateway() }
    val runtimeEngine = remember { DefaultBduiRuntimeEngine() }
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var route by remember { mutableStateOf<SandboxRoute>(SandboxRoute.Selector) }

    var projects by remember { mutableStateOf<List<RegistryItem>>(emptyList()) }
    var contracts by remember { mutableStateOf<List<RegistryItem>>(emptyList()) }
    var versions by remember { mutableStateOf<List<RegistryItem>>(emptyList()) }
    var screens by remember { mutableStateOf<List<ScreenDescriptor>>(emptyList()) }

    var selectedProject by remember { mutableStateOf<RegistryItem?>(null) }
    var selectedContract by remember { mutableStateOf<RegistryItem?>(null) }
    var selectedVersion by remember { mutableStateOf<RegistryItem?>(null) }
    var selectedScreen by remember { mutableStateOf<ScreenDescriptor?>(null) }

    var isLoadingHierarchy by remember { mutableStateOf(false) }
    var isBuilding by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loadProjects = remember(gateway) {
        suspend {
            isLoadingHierarchy = true
            errorMessage = null
            try {
                projects = gateway.listProjects()
            } catch (error: Throwable) {
                errorMessage = error.message ?: "Failed to load projects"
            } finally {
                isLoadingHierarchy = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadProjects()
    }

    val actionDispatcher = remember(uriHandler) {
        BduiActionDispatcher { action, meta ->
            when (action) {
                is BduiOpenUrlAction -> uriHandler.openUri(action.url)
                is BduiNavigateAction -> if (action.route == "back") {
                    route = SandboxRoute.Selector
                }

                else -> Unit
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val current = route) {
            SandboxRoute.Selector -> {
                SelectorScreen(
                    projects = projects,
                    contracts = contracts,
                    versions = versions,
                    screens = screens,
                    selectedProject = selectedProject,
                    selectedContract = selectedContract,
                    selectedVersion = selectedVersion,
                    selectedScreen = selectedScreen,
                    isLoadingHierarchy = isLoadingHierarchy,
                    isBuilding = isBuilding,
                    errorMessage = errorMessage,
                    onReload = {
                        scope.launch { loadProjects() }
                    },
                    onProjectSelected = { item ->
                        selectedProject = item
                        selectedContract = null
                        selectedVersion = null
                        selectedScreen = null
                        contracts = emptyList()
                        versions = emptyList()
                        screens = emptyList()
                        scope.launch {
                            isLoadingHierarchy = true
                            errorMessage = null
                            try {
                                contracts = gateway.listContracts(item.id)
                            } catch (error: Throwable) {
                                errorMessage = error.message ?: "Failed to load contracts"
                            } finally {
                                isLoadingHierarchy = false
                            }
                        }
                    },
                    onContractSelected = { item ->
                        val project = selectedProject ?: return@SelectorScreen
                        selectedContract = item
                        selectedVersion = null
                        selectedScreen = null
                        versions = emptyList()
                        screens = emptyList()
                        scope.launch {
                            isLoadingHierarchy = true
                            errorMessage = null
                            try {
                                versions = gateway.listVersions(project.id, item.id)
                            } catch (error: Throwable) {
                                errorMessage = error.message ?: "Failed to load versions"
                            } finally {
                                isLoadingHierarchy = false
                            }
                        }
                    },
                    onVersionSelected = { item ->
                        val project = selectedProject ?: return@SelectorScreen
                        val contract = selectedContract ?: return@SelectorScreen
                        selectedVersion = item
                        selectedScreen = null
                        screens = emptyList()
                        scope.launch {
                            isLoadingHierarchy = true
                            errorMessage = null
                            try {
                                screens = gateway.listScreens(project.id, contract.id, item.id)
                            } catch (error: Throwable) {
                                errorMessage = error.message ?: "Failed to load screens"
                            } finally {
                                isLoadingHierarchy = false
                            }
                        }
                    },
                    onScreenSelected = { selectedScreen = it },
                    onBuild = {
                        val project = selectedProject ?: return@SelectorScreen
                        val contract = selectedContract ?: return@SelectorScreen
                        val version = selectedVersion ?: return@SelectorScreen
                        val screen = selectedScreen ?: return@SelectorScreen
                        scope.launch {
                            isBuilding = true
                            errorMessage = null
                            try {
                                val context = gateway.buildContext(project.id, contract.id, version.id, screen.id)
                                val payload = gateway.loadScreenPayload(context)
                                val parseResult = runtimeEngine.parse(payload.rawJson, payload.schemaVersion)
                                val session = runtimeEngine.createSession(context, parseResult, actionDispatcher)
                                route = SandboxRoute.Render(
                                    session = session,
                                    title = "${project.label} / ${contract.label} / ${version.label} / ${screen.label}",
                                )
                            } catch (error: Throwable) {
                                errorMessage = error.message ?: "Failed to build screen"
                            } finally {
                                isBuilding = false
                            }
                        }
                    },
                )
            }

            is SandboxRoute.Render -> {
                RenderScreen(
                    route = current,
                    onBack = { route = SandboxRoute.Selector },
                )
            }
        }
    }
}

@Composable
private fun SelectorScreen(
    projects: List<RegistryItem>,
    contracts: List<RegistryItem>,
    versions: List<RegistryItem>,
    screens: List<ScreenDescriptor>,
    selectedProject: RegistryItem?,
    selectedContract: RegistryItem?,
    selectedVersion: RegistryItem?,
    selectedScreen: ScreenDescriptor?,
    isLoadingHierarchy: Boolean,
    isBuilding: Boolean,
    errorMessage: String?,
    onReload: () -> Unit,
    onProjectSelected: (RegistryItem) -> Unit,
    onContractSelected: (RegistryItem) -> Unit,
    onVersionSelected: (RegistryItem) -> Unit,
    onScreenSelected: (ScreenDescriptor) -> Unit,
    onBuild: () -> Unit,
) {
    var isDark by LocalThemeIsDark.current
    val icon = if (isDark) Res.drawable.ic_light_mode else Res.drawable.ic_dark_mode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nodus BDUI CMP Sandbox", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Runtime module: ${BduiRuntimeMetadata.moduleName} ${BduiRuntimeMetadata.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ElevatedButton(onClick = { isDark = !isDark }) {
                Icon(vectorResource(icon), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isDark) "Light" else "Dark")
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Выбор контекста", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Выберите проект, контракт, версию и экран, затем нажмите \"Построить\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SelectorField(
            title = "Проект",
            placeholder = "Выберите проект",
            items = projects,
            selectedLabel = selectedProject?.label,
            enabled = projects.isNotEmpty(),
            onSelect = onProjectSelected,
        )

        SelectorField(
            title = "Контракт",
            placeholder = if (selectedProject == null) "Сначала выберите проект" else "Выберите контракт",
            items = contracts,
            selectedLabel = selectedContract?.label,
            enabled = selectedProject != null && contracts.isNotEmpty(),
            onSelect = onContractSelected,
        )

        SelectorField(
            title = "Версия",
            placeholder = if (selectedContract == null) "Сначала выберите контракт" else "Выберите версию",
            items = versions,
            selectedLabel = selectedVersion?.label,
            enabled = selectedContract != null && versions.isNotEmpty(),
            onSelect = onVersionSelected,
        )

        ScreenSelectorField(
            title = "Экран",
            placeholder = if (selectedVersion == null) "Сначала выберите версию" else "Выберите экран",
            items = screens,
            selectedLabel = selectedScreen?.label,
            enabled = selectedVersion != null && screens.isNotEmpty(),
            onSelect = onScreenSelected,
        )

        if (isLoadingHierarchy || isBuilding) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(if (isBuilding) "Построение экрана..." else "Загрузка данных...")
            }
        }

        errorMessage?.let {
            ErrorCard(message = it, onReload = onReload)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                enabled = selectedProject != null && selectedContract != null && selectedVersion != null && selectedScreen != null && !isBuilding,
                onClick = onBuild,
            ) {
                Text("Построить")
            }

            TextButton(onClick = onReload) {
                Text("Обновить")
            }
        }
    }
}

@Composable
private fun RenderScreen(
    route: SandboxRoute.Render,
    onBack: () -> Unit,
) {
    val parseResult = route.session.parseResult
    val actionLog by route.session.actionLog

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Результат построения", style = MaterialTheme.typography.headlineSmall)
                Text(route.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
        }

        if (!parseResult.isSuccess) {
            DiagnosticsCard(route.session)
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(
                    modifier = Modifier.weight(1f).fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        BduiScreen(
                            session = route.session,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Card(
                    modifier = Modifier.width(280.dp).fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Action log", style = MaterialTheme.typography.titleMedium)
                        HorizontalDivider()
                        if (actionLog.isEmpty()) {
                            Text(
                                "Пока действий нет",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            actionLog.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(session: BduiRenderSession) {
    val parseResult = session.parseResult
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Схема не может быть отрисована", style = MaterialTheme.typography.titleMedium)
            listOf(parseResult.decodeErrors, parseResult.validationErrors, parseResult.warnings)
                .flatten()
                .forEach { diagnostic ->
                    Text("${diagnostic.path}: ${diagnostic.message}")
                }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onReload: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Ошибка загрузки", style = MaterialTheme.typography.titleMedium)
            Text(message)
            TextButton(onClick = onReload) {
                Text("Повторить")
            }
        }
    }
}

@Composable
private fun SelectorField(
    title: String,
    placeholder: String,
    items: List<RegistryItem>,
    selectedLabel: String?,
    enabled: Boolean,
    onSelect: (RegistryItem) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(selectedLabel ?: placeholder)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.label) },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenSelectorField(
    title: String,
    placeholder: String,
    items: List<ScreenDescriptor>,
    selectedLabel: String?,
    enabled: Boolean,
    onSelect: (ScreenDescriptor) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Text(selectedLabel ?: placeholder)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.label)
                                Text(item.id, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        onClick = {
                            expanded = false
                            onSelect(item)
                        },
                    )
                }
            }
        }
    }
}
