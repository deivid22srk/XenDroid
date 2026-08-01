package xendroid.compose.ui.patches

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xendroid.compose.patches.GamePatchesViewModel
import xendroid.compose.patches.PatchEntry
import xendroid.compose.patches.PatchFile
import xendroid.compose.ui.components.EmptyState
import xendroid.compose.ui.components.XdTopBar

/**
 * Lists the bundled patches for one game, grouped by file. Each `[[patch]]` is a row with a
 * Switch that toggles its on-disk `is_enabled` (effective on the next launch).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePatchesScreen(
    vm: GamePatchesViewModel,
    gameName: String,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            XdTopBar(
                title = if (gameName.isNotBlank()) "Patches · $gameName" else "Patches",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val s = state) {
                GamePatchesViewModel.UiState.Loading -> CircularProgressIndicator()
                GamePatchesViewModel.UiState.Empty ->
                    EmptyState(
                        icon = Icons.Outlined.Rule,
                        title = "No patches yet",
                        message = "No bundled patches for this game.",
                    )
                is GamePatchesViewModel.UiState.Error ->
                    EmptyState(
                        icon = Icons.Outlined.Info,
                        title = "Couldn't load patches",
                        message = s.message,
                    )
                is GamePatchesViewModel.UiState.Loaded ->
                    PatchList(files = s.files, onToggle = vm::toggle)
            }
        }
    }
}

@Composable
private fun PatchList(
    files: List<PatchFile>,
    onToggle: (PatchFile, PatchEntry, Boolean) -> Unit,
) {
    val showHeaders = files.size > 1
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Patches apply on the next launch of this game, and only if they match your game's version.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        for (file in files) {
            if (showHeaders) {
                item(key = "hdr:${file.fileName}") {
                    Text(
                        file.variantLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
            }
            items(file.entries, key = { "${file.fileName}#${it.index}" }) { entry ->
                PatchRow(
                    entry = entry,
                    onCheckedChange = { checked -> onToggle(file, entry, checked) },
                )
            }
            item(key = "div:${file.fileName}") { HorizontalDivider() }
        }
    }
}

@Composable
private fun PatchRow(entry: PatchEntry, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(entry.name) },
        supportingContent = {
            val sub = listOfNotNull(
                entry.desc?.takeIf { it.isNotBlank() },
                entry.author?.takeIf { it.isNotBlank() }?.let { "by $it" },
            ).joinToString("\n")
            if (sub.isNotBlank()) Text(sub)
        },
        trailingContent = {
            Switch(
                checked = entry.isEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
        },
    )
}
