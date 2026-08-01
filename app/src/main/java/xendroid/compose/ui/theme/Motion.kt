package xendroid.compose.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

// ---------------------------------------------------------------------------
// Material 3 motion system.
//
// Official M3 easing curves and duration tokens (M3 "Motion" spec). Every
// animation in the app uses these tokens instead of ad-hoc tweens so motion
// feels consistent, deliberate and platform-native. Curves are the documented
// M3 cubic-bezier values; durations are the M3 duration tokens expressed as
// milliseconds with semantic names.
// ---------------------------------------------------------------------------

object MotionTokens {

    // -- Easing curves (M3 motion spec) --------------------------------
    /** Emphasized — large-scale enter/exit + shared-axis transitions. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Emphasized decelerated — entering motion (accelerates out). */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Emphasized accelerated — exiting motion (accelerates in). */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard — small component state changes (toast, switch, ripple). */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Standard decelerated — entering motion for small components. */
    val StandardDecelerate: Easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** Standard accelerated — exiting motion for small components. */
    val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    // -- Duration tokens (M3 motion spec, in ms) -----------------------
    const val DurationShort1 = 50
    const val DurationShort2 = 100
    const val DurationShort3 = 150
    const val DurationShort4 = 200

    const val DurationMedium1 = 250
    const val DurationMedium2 = 300
    const val DurationMedium3 = 350
    const val DurationMedium4 = 400

    const val DurationLong1 = 450
    const val DurationLong2 = 500
    const val DurationLong3 = 550
    const val DurationLong4 = 600

    const val DurationExtraLong1 = 700
    const val DurationExtraLong2 = 800
    const val DurationExtraLong3 = 900
    const val DurationExtraLong4 = 1000
}
