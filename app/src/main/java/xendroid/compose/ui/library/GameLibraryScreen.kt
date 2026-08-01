package xendroid.compose.ui.library

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shortcut
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xendroid.compose.core.AllFilesAccess
import xendroid.compose.core.EmuProcessLink
import xendroid.compose.data.Game
import xendroid.compose.data.GameFormat
import xendroid.compose.ui.components.EmptyState
import xendroid.compose.ui.components.LeadingIconContainer
import xendroid.compose.ui.components.XdNavigationBar
import xendroid.compose.ui.components.XdTopBar
import xendroid.compose.ui.components.rememberXdScrollBehavior
import xendroid.compose.ui.compress.GameCompressViewModel
import xendroid.compose.ui.compress.GameCompressViewModel.CompressState
import xendroid.compose.ui.userdata.openUserData
import xendroid.compose.updater.CooldownDialog
import xendroid.compose.updater.getRemainingCooldown
import xendroid.compose.updater.LatestVersionDialog
import xendroid.compose.updater.UpdateDialog
import xendroid.compose.updater.UpdateResult
import xendroid.compose.updater.checkForUpdates
import xendroid.compose.updater.shouldCheckForUpdates
import xendroid.compose.updater.saveLastCheck

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(
    viewModel: GameLibraryViewModel,
    onOpenSettings: () -> Unit,
    onOpenKeymap: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenTouchControls: () -> Unit,
    onOpenPerGameSettings: (titleId: String, gameName: String, format: GameFormat, launchUri: String) -> Unit,
    onOpenGamePatches: (titleId: String, gameName: String) -> Unit,
    onOpenContentManager: (titleId: String, gameName: String) -> Unit,
    onOpenInstallContent: () -> Unit,
    compressVm: GameCompressViewModel,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }

    // The long-press menu target (per-game settings, optionally shortcut).
    var pendingGame by remember { mutableStateOf<Game?>(null) }
    // Long-press "Compress to .zar" (ISO only): the game awaiting the confirm dialog.
    var compressConfirmFor by remember { mutableStateOf<Game?>(null) }
    val compressState by compressVm.state.collectAsStateWithLifecycle()
    val titleIdState by viewModel.titleIdState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    // Real-path (All Files Access) mode: hosts the built-in File browser when shown.
    // Re-checked on ON_START so a grant made in Settings is observed on return.
    var showBrowser by remember { mutableStateOf(false) }
    var allFilesGranted by remember { mutableStateOf(AllFilesAccess.isGranted()) }
    // Start real-path mode: if not yet granted, send the user to Settings; once granted,
    // open the in-app folder browser. (Grant returns no result -> observed on ON_START.)
    val startRealPathMode: () -> Unit = {
        if (AllFilesAccess.isGranted()) showBrowser = true
        else AllFilesAccess.requestAccess(context)
    }

    // Re-scan when the app returns to the foreground (picks up games added while it was
    // backgrounded). The ViewModel's init does the first cold-start load, so the first
    // ON_START is skipped to avoid doubling it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var firstStart = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                // Re-check the All Files Access grant (no result payload): a grant made in
                // Settings while we were backgrounded is observed here on return.
                allFilesGranted = AllFilesAccess.isGranted()
                if (firstStart) firstStart = false else viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Real-path mode: the built-in File folder browser takes over the screen while open.
    // Choosing a folder persists it (switching to real-path mode) + rescans, then closes.
    if (showBrowser) {
        FolderBrowserScreen(
            onFolderChosen = { path ->
                showBrowser = false
                viewModel.onRealPathFolderPicked(path)
            },
            onCancel = { showBrowser = false },
        )
        return
    }

    // Collapsing top-bar motion; driven by the content's nested scroll.
    val scrollBehavior = rememberXdScrollBehavior()

    Scaffold(
        topBar = {
            XdTopBar(
                title = "Library",
                scrollBehavior = scrollBehavior,
                actions = {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        // Set game folder via All Files Access (real-path). Only offered where
                        // All Files Access exists (API 30+); on API 29 the empty state explains why.
                        if (AllFilesAccess.isSupported) {
                            DropdownMenuItem(
                                text = { Text("Set game folder") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Folder, contentDescription = null)
                                },
                                onClick = { menuOpen = false; startRealPathMode() },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Install content") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenInstallContent() },
                        )
                        DropdownMenuItem(
                            text = { Text("Profiles") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenProfiles() },
                        )
                        DropdownMenuItem(
                            text = { Text("Key mapping") },
                            leadingIcon = { Icon(Icons.Filled.Gamepad, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenKeymap() },
                        )
                        DropdownMenuItem(
                            text = { Text("Touch controls") },
                            leadingIcon = { Icon(Icons.Filled.Games, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenTouchControls() },
                        )
                        DropdownMenuItem(
                            text = { Text("Open user data") },
                            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                openUserData(context)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = { menuOpen = false; onOpenAbout() },
                        )
                        DropdownMenuItem(
                            text = { Text("Check for updates") },
                            leadingIcon = { Icon(Icons.Filled.Update, contentDescription = null) },
                            onClick = {
                                menuOpen = false

                                checkForUpdatesClicked(
                                    context = context,
                                    scope = scope,
                                    onResult = { updateResult = it }
                                )
                            }
                        )
                    }
                },
            )
        },
        bottomBar = {
            XdNavigationBar(currentRoute = currentRoute, onNavigate = onNavigate)
        },
        floatingActionButton = {
            // The primary "add games" action. Only meaningful when the library is showing.
            if (state is LibraryUiState.Loaded && AllFilesAccess.isSupported) {
                ExtendedFloatingActionButton(
                    onClick = startRealPathMode,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(if (allFilesGranted) "Add games" else "Set folder") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // The folder is set via All Files Access (real-path). The primary empty-state
                // button starts that flow; its label reflects whether the grant is already held.
                val setFolderLabel =
                    if (allFilesGranted) "Set game folder" else "Grant All Files Access"
                when (val s = state) {
                    LibraryUiState.NoVulkan ->
                        NoVulkanDialog(onQuit = { (context as? Activity)?.finish() })
                    LibraryUiState.Loading -> CircularProgressIndicator()
                    // All Files Access is API 30+; on API 29 there is no games path at all.
                    LibraryUiState.NoFolder ->
                        if (AllFilesAccess.isSupported)
                            EmptyState(
                                icon = Icons.Filled.Folder,
                                title = "No game folder set",
                                message = "Pick the folder that holds your Xbox 360 games to get started.",
                                actionLabel = setFolderLabel,
                                onAction = startRealPathMode,
                                tonal = true,
                            )
                        else
                            EmptyState(
                                icon = Icons.Filled.Folder,
                                title = "Android 11+ required",
                                message = "Setting a game folder requires Android 11 or newer.",
                                actionLabel = "OK",
                                onAction = {},
                            )
                    LibraryUiState.PermissionLost ->
                        EmptyState(
                            icon = Icons.Filled.Folder,
                            title = "Folder access lost",
                            message = "Re-grant All Files Access to keep scanning this folder.",
                            actionLabel = setFolderLabel,
                            onAction = startRealPathMode,
                            tonal = true,
                        )
                    is LibraryUiState.Error ->
                        EmptyState(
                            icon = Icons.Filled.Folder,
                            title = "Couldn't load the library",
                            message = s.message,
                            actionLabel = "Retry",
                            onAction = { viewModel.refresh() },
                            tonal = true,
                        )
                    is LibraryUiState.Loaded ->
                        if (s.games.isEmpty())
                            EmptyState(
                                icon = Icons.Filled.VideogameAsset,
                                title = "No games in this folder",
                                message = "Add a folder with Xbox 360 games, or pick a different one.",
                                actionLabel = "Choose another folder",
                                onAction = startRealPathMode,
                                tonal = true,
                            )
                        else GameGrid(
                            games = s.games,
                            viewModel = viewModel,
                            onLaunch = { game ->
                                runCatching {
                                    // Reap any stale/orphaned :emu first (single-shot core), then
                                    // attach the main-process liveness token so the new :emu dies
                                    // with us. Token is added here, NOT in buildLaunchIntent, since
                                    // that Intent is reused for pinned shortcuts (no Binders on disk).
                                    EmuProcessLink.killStaleEmu(context)
                                    val intent = viewModel.buildLaunchIntent(game)
                                    EmuProcessLink.attachMainAliveToken(intent)
                                    context.startActivity(intent)
                                }
                            },
                            // Long-press opens the per-game menu (independent of canLaunchGames,
                            // which only gates the shortcut affordance inside the dialog).
                            onLongPress = { pendingGame = it },
                        )
                }
            }
        }
    }

    when (val result = updateResult) {
        is UpdateResult.Available -> {
            UpdateDialog(
                release = result.release,
                onDismiss = { updateResult = null }
            )
        }

        is UpdateResult.Latest -> {
            LatestVersionDialog(
                commitHash = result.commitHash,
                onDismiss = { updateResult = null }
            )
        }

        is UpdateResult.Cooldown -> {
            CooldownDialog(
                remainingMillis = result.remainingMillis,
                onDismiss = { updateResult = null }
            )
        }

        null -> {}
    }

    pendingGame?.let { game ->
        val dismiss = { pendingGame = null; viewModel.clearTitleIdRequest() }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = dismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                // Sheet header: game icon, name; the title-id status line shows ONLY
                // while resolving or on error (no static subtitle otherwise).
                val statusContent: (@Composable () -> Unit)? = when (val st = titleIdState) {
                    is TitleIdState.Loading -> ({
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Reading title id…")
                        }
                    })
                    is TitleIdState.Error -> ({ Text(st.message) })
                    else -> null
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(game = game, viewModel = viewModel, size = 56)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            game.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (game.isMultiDisc) {
                            Text(
                                "Disc ${game.discNumber} of ${game.discCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (game.titleId != null || game.mediaId != null || statusContent != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                game.titleId?.let { Text("Title ID: $it") }
                                game.mediaId?.let { Text("Media ID: $it") }
                                statusContent?.invoke()
                            }
                        }
                    }
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                // Per-game settings — disabled (and visually dimmed) while the title id resolves.
                val perGameEnabled = titleIdState !is TitleIdState.Loading
                SheetItem(
                    icon = Icons.Filled.Settings,
                    label = "Per-game settings",
                    enabled = perGameEnabled,
                ) {
                    viewModel.requestPerGameSettings(game)
                }

                // Game patches — bundled xenia patches for this title (all formats; title id is
                // read boot-free, including ZAR via ExtractZarMetadata).
                SheetItem(
                    icon = Icons.Filled.Games,
                    label = "Game patches",
                    enabled = perGameEnabled,
                ) {
                    viewModel.requestGamePatches(game)
                }

                // Manage content — install, list and remove DLC + title updates for this game
                // (title id resolved boot-free, same as patches above).
                SheetItem(
                    icon = Icons.Filled.VideogameAsset,
                    label = "Manage content",
                    enabled = perGameEnabled,
                ) {
                    viewModel.requestContentManager(game)
                }

                // ISO-only: pack this disc into a smaller .zar.
                if (game.format == GameFormat.ISO) {
                    SheetItem(icon = Icons.Filled.ContentCut, label = "Compress to .zar") {
                        compressConfirmFor = game
                        pendingGame = null
                        viewModel.clearTitleIdRequest()
                    }
                }

                // Shortcut affordance when a launchable host exists.
                if (viewModel.canLaunchGames && viewModel.isPinShortcutSupported) {
                    SheetItem(icon = Icons.Filled.Shortcut, label = "Create shortcut") {
                        viewModel.createShortcut(game)
                        dismiss()
                    }
                }
            }
        }
    }

    // Once resolved, navigate to the editor or the patches screen, then reset dialog + request.
    LaunchedEffect(titleIdState) {
        (titleIdState as? TitleIdState.Resolved)?.let { r ->
            when (r.action) {
                GameAction.PER_GAME_SETTINGS ->
                    onOpenPerGameSettings(r.titleId, r.game.name, r.game.format, r.game.launchUri)
                GameAction.GAME_PATCHES ->
                    onOpenGamePatches(r.titleId, r.game.name)
                GameAction.MANAGE_CONTENT ->
                    onOpenContentManager(r.titleId, r.game.name)
            }
            pendingGame = null
            viewModel.clearTitleIdRequest()
        }
    }

    // ISO -> .zar compress, launched straight from the long-press popup. Confirm, then
    // GameCompressViewModel runs the safe compress+verify+replace; refresh on Done so the
    // .iso entry turns into the new .zar in the grid.
    compressConfirmFor?.let { game ->
        AlertDialog(
            onDismissRequest = { compressConfirmFor = null },
            title = { Text("Compress to .zar?") },
            text = {
                Text(
                    "This packs the disc into a smaller .zar. The original .iso is deleted " +
                        "only after the .zar is created and verified. The game stays in your library.")
            },
            confirmButton = {
                TextButton(onClick = {
                    compressConfirmFor = null
                    compressVm.compress(game.launchUri)
                }) { Text("Compress") }
            },
            dismissButton = {
                TextButton(onClick = { compressConfirmFor = null }) { Text("Cancel") }
            },
        )
    }

    when (val s = compressState) {
        is CompressState.Busy -> AlertDialog(
            onDismissRequest = {},   // not cancelable while running
            title = { Text(s.message) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (s.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${(s.progress * 100).toInt()}%  ·  this may take a while.")
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("This may take a while.")
                    }
                }
            },
            confirmButton = {},
        )
        is CompressState.Done -> AlertDialog(
            onDismissRequest = { compressVm.dismiss(); viewModel.refresh() },
            title = { Text("Done") },
            text = { Text(s.message) },
            confirmButton = {
                TextButton(onClick = { compressVm.dismiss(); viewModel.refresh() }) { Text("OK") }
            },
        )
        is CompressState.Failed -> AlertDialog(
            onDismissRequest = compressVm::dismiss,
            title = { Text("Failed") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = compressVm::dismiss) { Text("OK") } },
        )
        else -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val itemColors = if (enabled) {
        ListItemDefaults.colors()
    } else {
        ListItemDefaults.colors(
            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
    ListItem(
        headlineContent = {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        },
        leadingContent = {
            LeadingIconContainer(
                icon = icon,
                contentDescription = null,
                size = 40,
                container = if (enabled) MaterialTheme.colorScheme.secondaryContainer
                           else MaterialTheme.colorScheme.surfaceContainerHighest,
                tint = if (enabled) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = itemColors,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
            ),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameGrid(
    games: List<Game>,
    viewModel: GameLibraryViewModel,
    onLaunch: (Game) -> Unit,
    onLongPress: (Game) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(games, key = { it.stableId }) { game ->
            GameCell(game, viewModel, onLaunch, onLongPress)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameCell(
    game: Game,
    viewModel: GameLibraryViewModel,
    onLaunch: (Game) -> Unit,
    onLongPress: (Game) -> Unit,
) {
    // Resolve the icon model once per cell (the File.exists() stat must not run on
    // every recomposition while scrolling).
    Card(
        modifier = Modifier
            .padding(4.dp)
            .combinedClickable(onClick = { onLaunch(game) }, onLongClick = { onLongPress(game) }),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            GameIcon(game = game, viewModel = viewModel, size = 0, fillWidth = true)
            Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    game.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // A set shares one title, so the tiles would otherwise be identical.
                if (game.isMultiDisc) {
                    Text(
                        "Disc ${game.discNumber} of ${game.discCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Game cover/icon. When [size] > 0 renders a fixed square (used in the sheet
 * header); when [fillWidth] is set it stretches across the card with a
 * square-ish aspect ratio.
 */
@Composable
private fun GameIcon(
    game: Game,
    viewModel: GameLibraryViewModel,
    size: Int = 0,
    fillWidth: Boolean = false,
) {
    val context = LocalContext.current
    val iconModel = remember(game.stableId) { viewModel.iconFileOrFallback(game) }
    val baseModifier = if (fillWidth) {
        Modifier.fillMaxWidth().aspectRatio(1f)
    } else {
        Modifier.size(size.dp)
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(iconModel)   // File or app_icon res id
            .build(),
        contentDescription = game.name,
        contentScale = ContentScale.Crop,
        modifier = baseModifier,
    )
}

@Composable
private fun NoVulkanDialog(onQuit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onQuit,
        confirmButton = { TextButton(onClick = onQuit) { Text("Quit") } },
        title = { Text("Unsupported device") },
        text = { Text("This device has no Vulkan GPU; the emulator cannot run.") },
    )
}

fun checkForUpdatesClicked(
    context: Context,
    scope: CoroutineScope,
    onResult: (UpdateResult) -> Unit
) {
    scope.launch {
        if (!shouldCheckForUpdates(context)) {
            Log.d("Updater", "Skipping update check")
            onResult(UpdateResult.Cooldown(getRemainingCooldown(context)))
            return@launch
        }

        try {
            val result = checkForUpdates()
            saveLastCheck(context)
            onResult(result)
        } catch (e: Exception) {
            Log.e("Updater", "Failed to check updates", e)
        }
    }
}
