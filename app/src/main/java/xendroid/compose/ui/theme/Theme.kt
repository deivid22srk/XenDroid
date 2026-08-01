package xendroid.compose.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * XenDroid's Material You 3 theme.
 *
 * [dynamicColor] (default on) uses the wallpaper-derived dynamic scheme on
 * Android 12+; older devices and providers without dynamic color fall back to
 * the consistent Xbox-green brand schemes ([LightColorScheme]/[DarkColorScheme]).
 * Typography and shapes always come from the shared M3 tokens.
 */
@Composable
fun xendroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = XdTypography,
        shapes = XdShapes,
        content = content,
    )
}
