package xendroid.compose.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Hardware
import androidx.compose.material.icons.outlined.ImportantDevices
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputComponent
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import xendroid.compose.settings.SettingValue
import xendroid.compose.settings.SettingsCategory
import xendroid.compose.settings.SettingsViewModel
import xendroid.compose.ui.components.NavListRow
import xendroid.compose.ui.components.XdNavigationBar
import xendroid.compose.ui.components.XdTopBar
import xendroid.compose.ui.components.rememberXdScrollBehavior
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** Category title -> leading icon for the settings index. */
internal fun categoryIcon(title: String): ImageVector = when (title) {
    "Vulkan" -> Icons.Outlined.ImportantDevices
    "Video" -> Icons.Outlined.Movie
    "UI" -> Icons.Outlined.Palette
    "Storage" -> Icons.Outlined.Storage
    "Kernel" -> Icons.Outlined.Memory
    "HID" -> Icons.Outlined.Gamepad
    "Memory" -> Icons.Outlined.DeveloperBoard
    "XConfig" -> Icons.Outlined.Tune
    "Display" -> Icons.Outlined.Monitor
    "GPU" -> Icons.Outlined.Hardware
    "CPU" -> Icons.Outlined.SettingsInputComponent
    "Logging" -> Icons.Outlined.BugReport
    "Content" -> Icons.Outlined.Folder
    "General" -> Icons.Outlined.Settings
    "APU" -> Icons.Outlined.GraphicEq
    else -> Icons.Outlined.Tune
}

/**
 * Two-level settings: an INDEX of sections (the 124-entry schema is too long for one list), and
 * a per-section DETAIL with that section's rows. Navigation between the two is internal state so
 * the single SettingsViewModel (and its config handle / flush lifecycle) is shared; the system
 * back button goes detail->index->exit via [BackHandler].
 */
@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    onBack: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val values by vm.values.collectAsStateWithLifecycle()

    // Durable flush on pause; re-open on resume. Dispose flush = backstop.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> vm.flush()
                Lifecycle.Event.ON_RESUME -> vm.onResume()
                else -> {}
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs); vm.flush() }
    }

    var selected by remember { mutableStateOf<SettingsCategory?>(null) }
    val section = selected
    if (section == null) {
        SettingsIndex(
            categories = vm.categories,
            modifiedCountOf = { cat -> cat.settings.count { values[it.key]?.modified == true } },
            onOpen = { selected = it },
            onBack = { vm.flush(); onBack() },
            currentRoute = currentRoute,
            onNavigate = onNavigate,
        )
    } else {
        BackHandler { selected = null }
        SettingsCategoryDetail(
            category = section,
            values = values,
            vm = vm,
            onBack = { selected = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsIndex(
    categories: List<SettingsCategory>,
    modifiedCountOf: (SettingsCategory) -> Int,
    onOpen: (SettingsCategory) -> Unit,
    onBack: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val scrollBehavior = rememberXdScrollBehavior()
    Scaffold(
        topBar = {
            XdTopBar(
                title = "Settings",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            XdNavigationBar(currentRoute = currentRoute, onNavigate = onNavigate)
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(categories, key = { it.title }) { cat ->
                val modified = modifiedCountOf(cat)
                NavListRow(
                    icon = categoryIcon(cat.title),
                    title = cat.title,
                    supporting = buildString {
                        append("${cat.settings.size} settings")
                        if (modified > 0) append("  ·  $modified changed")
                    },
                    onClick = { onOpen(cat) },
                )
                HorizontalDivider(
                    Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryDetail(
    category: SettingsCategory,
    values: Map<String, SettingValue>,
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            XdTopBar(
                title = category.title,
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back to sections",
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(category.settings, key = { it.key }) { setting ->
                val sv = values[setting.key]
                SettingRow(vm, setting, modified = sv?.modified == true, raw = sv?.raw)
                HorizontalDivider(
                    Modifier.padding(start = 16.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}
