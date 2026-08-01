package xendroid.compose.ui.about

import android.graphics.BitmapFactory
import android.webkit.WebView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import xendroid.compose.Emulator
import xendroid.compose.ui.components.LeadingIconContainer
import xendroid.compose.ui.components.XdNavigationBar
import xendroid.compose.ui.components.XdTopBar

private const val TITLE = "Xendroid — Xbox 360 emulation on Android."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }
    // simple_device_info() is a JNI instance method; Emulator.get may be null before
    // load_library() on delay-load devices. Guard it.
    val deviceInfo = remember {
        runCatching { Emulator.get?.simple_device_info() }.getOrNull()
            ?: "Device info unavailable (emulator not loaded yet)."
    }
    val logo = remember {
        runCatching {
            context.assets.open("XenDroid_foreground.png").use {
                BitmapFactory.decodeStream(it).asImageBitmap()
            }
        }.getOrNull()
    }

    Scaffold(
        topBar = {
            XdTopBar(
                title = "About",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
            )
        },
        bottomBar = {
            XdNavigationBar(currentRoute = currentRoute, onNavigate = onNavigate)
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                logo?.let {
                    Image(
                        bitmap = it,
                        contentDescription = "XenDroid logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(128.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("XenDroid", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(28.dp))

            // Credits card.
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LeadingIconContainer(
                            icon = Icons.Outlined.Code,
                            contentDescription = null,
                            size = 40,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Credits", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        TITLE,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Device info card.
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LeadingIconContainer(
                            icon = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            size = 40,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Device", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            deviceInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            FilledTonalButton(
                onClick = { showLicenses = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open-source licenses")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            confirmButton = { TextButton(onClick = { showLicenses = false }) { Text("OK") } },
            title = { Text("Licenses") },
            text = {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply { loadUrl("file:///android_asset/licenses.html") }
                    },
                    modifier = Modifier.fillMaxWidth().height(400.dp),
                )
            },
        )
    }
}
