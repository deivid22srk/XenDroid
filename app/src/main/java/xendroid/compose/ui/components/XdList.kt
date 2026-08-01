package xendroid.compose.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A tonal rounded container for leading list icons (M3 "icon in container"). */
@Composable
fun LeadingIconContainer(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
    size: Int = 40,
) {
    Surface(
        modifier = modifier.size(size.dp),
        shape = RoundedCornerShape((size / 2).dp),
        color = container,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size((size * 0.55).dp),
            )
        }
    }
}

/**
 * A settings-style list row: leading tonal icon, title + supporting text, and a
 * trailing chevron. The whole row is a 48dp+ touch target and clickable.
 */
@Composable
fun NavListRow(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LeadingIconContainer(icon = icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!supporting.isNullOrEmpty()) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

/** M3 list-section header. */
@Composable
fun ListSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
