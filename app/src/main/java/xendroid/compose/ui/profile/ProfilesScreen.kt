package xendroid.compose.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import xendroid.compose.core.Gamertag
import xendroid.compose.core.ProfilePaths
import xendroid.compose.settings.ListOption
import xendroid.compose.settings.Setting
import xendroid.compose.settings.SettingsSchema
import xendroid.compose.ui.components.EmptyState
import xendroid.compose.ui.components.LeadingIconContainer
import xendroid.compose.ui.components.XdTopBar
import xendroid.compose.ui.profile.ProfileManagerViewModel.ListState
import xendroid.compose.ui.profile.ProfileManagerViewModel.OpState
import xendroid.compose.ui.profile.ProfileManagerViewModel.ProfileEntry

// Top-level vals, so a hard cast on a key whose toml section moved would throw
// during class init, before anything can catch it.
private fun listChoice(key: String) =
    SettingsSchema.byKey[key] as? Setting.ListChoice

private val LANGUAGE_OPTIONS = listChoice("Console|user_language")?.options.orEmpty()
private val COUNTRY_OPTIONS = listChoice("Console|user_country")?.options.orEmpty()
private val DEFAULT_LANGUAGE =
    listChoice("Console|user_language")?.default?.toIntOrNull() ?: 1     // en
private val DEFAULT_COUNTRY =
    listChoice("Console|user_country")?.default?.toIntOrNull() ?: 103    // United States

private sealed interface Editing {
    data object None : Editing
    data object Create : Editing
    data class Rename(val entry: ProfileEntry) : Editing
}

@Composable
fun ProfilesScreen(
    vm: ProfileManagerViewModel,
    onBack: () -> Unit,
) {
    val listState by vm.listState.collectAsStateWithLifecycle()
    val opState by vm.opState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing>(Editing.None) }
    var deleteTarget by remember { mutableStateOf<ProfileEntry?>(null) }

    Scaffold(
        topBar = {
            XdTopBar(
                title = "Profiles",
                onNavigationClick = onBack,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                navigationContentDescription = "Back",
                actions = {
                    IconButton(onClick = { editing = Editing.Create }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create profile")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when (val s = listState) {
                ListState.Loading -> CircularProgressIndicator()
                is ListState.Error -> Text(
                    s.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
                is ListState.Loaded ->
                    if (s.profiles.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.Person,
                            title = "No profiles yet",
                            message = "Tap + to create your first profile.",
                            actionLabel = "Create profile",
                            onAction = { editing = Editing.Create },
                            tonal = true,
                        )
                    } else {
                        ProfileList(
                            profiles = s.profiles,
                            onSelect = vm::setActive,
                            onRename = { editing = Editing.Rename(it) },
                            onDelete = { deleteTarget = it },
                        )
                    }
            }
        }
    }

    when (val e = editing) {
        Editing.None -> {}
        Editing.Create -> ProfileForm(
            initial = null,
            onDismiss = { editing = Editing.None },
            onSubmit = { tag, lang, country, avatar ->
                editing = Editing.None
                vm.create(tag, lang, country, avatar)
            },
        )
        is Editing.Rename -> ProfileForm(
            initial = e.entry,
            onDismiss = { editing = Editing.None },
            onSubmit = { tag, lang, country, avatar ->
                editing = Editing.None
                vm.rename(e.entry.xuid, tag, lang, country, avatar)
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete profile?") },
            text = { Text("Delete “${target.gamertag}”? This removes its files permanently.") },
            confirmButton = {
                TextButton(onClick = { deleteTarget = null; vm.delete(target.xuid) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    when (val s = opState) {
        is OpState.Busy -> AlertDialog(
            onDismissRequest = {},
            title = { Text(s.message) },
            text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        is OpState.Done -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Done") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text("OK") } },
        )
        is OpState.Failed -> AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text("Couldn't complete") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = vm::dismiss) { Text("OK") } },
        )
        OpState.Idle -> {}
    }
}

@Composable
private fun ProfileList(
    profiles: List<ProfileEntry>,
    onSelect: (String) -> Unit,
    onRename: (ProfileEntry) -> Unit,
    onDelete: (ProfileEntry) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(profiles, key = { it.xuid }) { p ->
            ListItem(
                leadingContent = { ProfileAvatar(p) },
                headlineContent = {
                    Text(
                        p.gamertag.ifBlank { p.xuid },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = if (p.isActive) ({ Text("Active") }) else null,
                trailingContent = { RowMenu(p, onRename, onDelete) },
                modifier = Modifier.clickable { onSelect(p.xuid) },
            )
            HorizontalDivider(Modifier.padding(start = 72.dp))
        }
    }
}

/** Circular 40dp profile tile (avatar or tonal placeholder) with the active badge. */
@Composable
private fun ProfileAvatar(p: ProfileEntry) {
    Box {
        if (p.hasAvatar) {
            val model = remember(p.xuid) { ProfilePaths.tile64Path(p.xuid) }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(model).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            LeadingIconContainer(
                icon = Icons.Filled.Person,
                contentDescription = null,
                size = 40,
            )
        }
        if (p.isActive) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun RowMenu(
    p: ProfileEntry,
    onRename: (ProfileEntry) -> Unit,
    onDelete: (ProfileEntry) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = { open = false; onRename(p) },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = { open = false; onDelete(p) },
        )
    }
}

/** Create/edit profile dialog: avatar picker, gamertag, and language/region dropdowns. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileForm(
    initial: ProfileEntry?,
    onDismiss: () -> Unit,
    onSubmit: (gamertag: String, language: Int, country: Int, avatar: Uri?) -> Unit,
) {
    var gamertag by rememberSaveable { mutableStateOf(initial?.gamertag ?: "") }
    var language by rememberSaveable { mutableIntStateOf(initial?.language ?: DEFAULT_LANGUAGE) }
    var country by rememberSaveable { mutableIntStateOf(initial?.country ?: DEFAULT_COUNTRY) }
    var avatar by remember { mutableStateOf<Uri?>(null) }

    val pickAvatar = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) avatar = uri }

    val valid = Gamertag.isValid(gamertag)
    val showError = gamertag.isNotEmpty() && !valid

    val currentAvatar = avatar
        ?: initial?.takeIf { it.hasAvatar }?.let { Uri.fromFile(ProfilePaths.tile64Path(it.xuid)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create profile" else "Edit profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AvatarPicker(
                    avatar = currentAvatar,
                    onPick = {
                        pickAvatar.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                OutlinedTextField(
                    value = gamertag,
                    onValueChange = { if (it.length <= 15) gamertag = it },
                    label = { Text("Gamertag") },
                    singleLine = true,
                    isError = showError,
                    supportingText = if (showError) {
                        {
                            Text(
                                "1-15 letters/digits; must start with a letter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                ChoiceField("Language", LANGUAGE_OPTIONS, language) { language = it }
                ChoiceField("Region", COUNTRY_OPTIONS, country) { country = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSubmit(gamertag, language, country, avatar) },
            ) { Text(if (initial == null) "Create" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * 72dp tonal circle that launches the avatar picker, with a camera affordance
 * badge. Shows the picked (or existing) avatar image once one is present.
 */
@Composable
private fun AvatarPicker(
    avatar: Uri?,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Pick avatar",
                    onClick = onPick,
                ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (avatar != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(avatar).build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier.size(28.dp).align(Alignment.BottomEnd),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                Icons.Outlined.AddAPhoto,
                contentDescription = "Pick avatar",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

/**
 * Read-only exposed dropdown for a list-style setting (language/region). Keeps
 * the exact option source and [onSelect] semantics; replaces the old radio-list
 * dialog with an M3 [OutlinedTextField]-anchored menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(
    label: String,
    options: List<ListOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.firstOrNull { it.value.toIntOrNull() == selected }?.label
        ?: selected.toString()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = currentLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 320.dp),
        ) {
            options.forEach { opt ->
                val value = opt.value.toIntOrNull()
                DropdownMenuItem(
                    text = { Text(opt.label) },
                    leadingIcon = if (value == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        value?.let(onSelect)
                        expanded = false
                    },
                )
            }
        }
    }
}
