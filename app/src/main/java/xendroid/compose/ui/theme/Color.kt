package xendroid.compose.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// XenDroid Material You 3 color system.
//
// Two static schemes built from the Xbox-green brand seed (#107C10) used as a
// consistent fallback whenever Dynamic Color is unavailable (Android < 12 or a
// device without a dynamic color provider). On Android 12+ the app prefers the
// wallpaper-derived dynamic scheme (see Theme.kt); these tokens keep the brand
// alive on every other device.
//
// Neutral surfaces / containers follow the Material Theme Builder tonal ramp so
// elevation, chips, cards and dialogs all read correctly in both modes.
// ---------------------------------------------------------------------------

/** Fallback light scheme, seeded with Xbox green (#107C10). */
val LightColorScheme = lightColorScheme(
    primary = Color(0xFF107C10),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA6F398),
    onPrimaryContainer = Color(0xFF002201),
    inversePrimary = Color(0xFF8FD97B),

    secondary = Color(0xFF52634F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E8CF),
    onSecondaryContainer = Color(0xFF101F0F),

    tertiary = Color(0xFF38656A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCEBF0),
    onTertiaryContainer = Color(0xFF002023),

    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = Color(0xFFFDFDF6),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFFDFDF6),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5D8),
    onSurfaceVariant = Color(0xFF424940),
    surfaceTint = Color(0xFF107C10),
    inverseSurface = Color(0xFF2F312D),
    inverseOnSurface = Color(0xFFF1F1EA),
    scrim = Color(0xFF000000),
    outline = Color(0xFF727970),
    outlineVariant = Color(0xFFC2C9BC),

    surfaceBright = Color(0xFFFDFDF6),
    surfaceDim = Color(0xFFDDDED6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7F0),
    surfaceContainer = Color(0xFFF1F1EA),
    surfaceContainerHigh = Color(0xFFEBECE4),
    surfaceContainerHighest = Color(0xFFE5E6DF),
)

/** Fallback dark scheme, seeded with Xbox green (#107C10). */
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FD97B),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF0B5D12),
    onPrimaryContainer = Color(0xFFA6F398),
    inversePrimary = Color(0xFF107C10),

    secondary = Color(0xFFB9CCB3),
    onSecondary = Color(0xFF253423),
    secondaryContainer = Color(0xFF3B4B38),
    onSecondaryContainer = Color(0xFFD5E8CF),

    tertiary = Color(0xFFA0CFD4),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D52),
    onTertiaryContainer = Color(0xFFBCEBF0),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DB),
    surface = Color(0xFF1A1C19),
    onSurface = Color(0xFFE2E3DB),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BC),
    surfaceTint = Color(0xFF8FD97B),
    inverseSurface = Color(0xFFE2E3DB),
    inverseOnSurface = Color(0xFF2F312D),
    scrim = Color(0xFF000000),
    outline = Color(0xFF8C9386),
    outlineVariant = Color(0xFF424940),

    surfaceBright = Color(0xFF3F423D),
    surfaceDim = Color(0xFF1A1C19),
    surfaceContainerLowest = Color(0xFF151713),
    surfaceContainerLow = Color(0xFF1A1C19),
    surfaceContainer = Color(0xFF1E201D),
    surfaceContainerHigh = Color(0xFF292B27),
    surfaceContainerHighest = Color(0xFF343632),
)
