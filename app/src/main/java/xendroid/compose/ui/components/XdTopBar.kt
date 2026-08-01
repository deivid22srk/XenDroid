package xendroid.compose.ui.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/** Returns the standard M3 scroll behavior for collapsing app bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberXdScrollBehavior(): TopAppBarScrollBehavior =
    TopAppBarDefaults.enterAlwaysScrollBehavior()

/**
 * Consistent top app bar used across every destination. Applies the M3
 * enter-always scroll behavior when [scrollBehavior] is provided so the bar
 * collapses with the official M3 motion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XdTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = rememberXdScrollBehavior(),
) {
    TopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigationClick != null && navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = navigationContentDescription)
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

/** Center-aligned variant for detail screens that prefer a centered title. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XdCenterTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigationClick: (() -> Unit)? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    CenterAlignedTopAppBar(
        title = {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier,
        navigationIcon = {
            if (onNavigationClick != null && navigationIcon != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(navigationIcon, contentDescription = navigationContentDescription)
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
