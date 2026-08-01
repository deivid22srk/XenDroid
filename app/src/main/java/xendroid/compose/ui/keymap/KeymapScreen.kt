package xendroid.compose.ui.keymap

import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xendroid.compose.ui.components.XdTopBar

/**
 * Key-mapping screen: one row per game button. Tapping a row opens the capture dialog;
 * the Clear action restores that button's unbound state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeymapScreen(vm: KeymapViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var capturing by remember { mutableStateOf<KeymapRow?>(null) }

    Scaffold(
        topBar = {
            XdTopBar(
                title = "Key Mapping",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                actions = {
                    TextButton(onClick = { vm.onResetDefaults() }) { Text("Reset") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.rows, key = { it.button.index }) { row ->
                ListItem(
                    headlineContent = { Text(row.button.label) },
                    supportingContent = { Text(keyLabel(row.boundKey)) },
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AssistChip(
                                onClick = { capturing = row },
                                label = { Text(keyLabel(row.boundKey)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Keyboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                            )
                            TextButton(onClick = { vm.onClear(row.button.index) }) { Text("Clear") }
                        }
                    },
                    modifier = Modifier.clickable { capturing = row },
                )
                HorizontalDivider()
            }
        }
    }

    capturing?.let { row ->
        KeyCaptureDialog(
            label = row.button.label,
            onKey = { code -> vm.onKeyCaptured(row.button.index, code); capturing = null },
            onDismiss = { capturing = null },
        )
    }
}

@Composable
private fun KeyCaptureDialog(label: String, onKey: (Int) -> Unit, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BackHandler(enabled = true, onBack = onDismiss)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Press a key for $label") },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .focusable()
                    .onKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown) {
                            onKey(ev.nativeKeyEvent.keyCode); true
                        } else false
                    }
            ) { Text("Waiting for a controller/keyboard button…") }
        },
    )
}

/** Human-readable name for a bound Android keycode (0 = unbound). */
private fun keyLabel(code: Int): String =
    if (code == 0) "(unbound)"
    else AndroidKeyEvent.keyCodeToString(code).removePrefix("KEYCODE_")
