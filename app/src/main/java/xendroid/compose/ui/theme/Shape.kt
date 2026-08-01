package xendroid.compose.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ---------------------------------------------------------------------------
// Material 3 shape system.
//
// Token-per-component-category corner radii following the official M3 shape
// spec. Larger radii on the "extra" end give the UI the soft, expressive feel
// while every component still maps to its documented token (buttons = full,
// cards = medium/large, dialogs = large/extraLarge, FAB = extraLarge...).
// ---------------------------------------------------------------------------

private val XdExtraSmall: CornerBasedShape = RoundedCornerShape(4.dp)
private val XdSmall: CornerBasedShape = RoundedCornerShape(8.dp)
private val XdMedium: CornerBasedShape = RoundedCornerShape(12.dp)
private val XdLarge: CornerBasedShape = RoundedCornerShape(16.dp)
private val XdExtraLarge: CornerBasedShape = RoundedCornerShape(28.dp)

/** M3 shape tokens used by every component in the app. */
val XdShapes = Shapes(
    extraSmall = XdExtraSmall,
    small = XdSmall,
    medium = XdMedium,
    large = XdLarge,
    extraLarge = XdExtraLarge,
)
