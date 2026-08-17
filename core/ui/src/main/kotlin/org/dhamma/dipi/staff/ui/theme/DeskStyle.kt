package org.dhamma.dipi.staff.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The sleek pass (owner decision 2026-08-16): real cards instead of the
 * wireframe's hairline-only boxes. One radius per element class and two
 * gentle elevation tiers, defined once here — never as magic numbers at
 * call sites. Everything else in the Industry system (type, single accent,
 * status tones, scarce motion) is unchanged.
 */
object DeskStyle {
    /** Cards, action rows and dialog panels. */
    val cardRadius: Dp = 12.dp

    /** Grid cells and small tiles (room chart, room picker, roll table). */
    val tileRadius: Dp = 10.dp

    /** Chips, buttons, inputs, toggles and segmented controls. */
    val controlRadius: Dp = 8.dp

    val cardShape = RoundedCornerShape(cardRadius)
    val tileShape = RoundedCornerShape(tileRadius)
    val controlShape = RoundedCornerShape(controlRadius)

    /** Status pills and the toggle track are fully rounded. */
    val pillShape = RoundedCornerShape(50)

    /** Gentle lift on interactive cards and tiles. */
    val cardElevation: Dp = 2.dp

    /** The check-in dialog and the snackbar float a little higher. */
    val dialogElevation: Dp = 4.dp

    /** Elevated surface tone — the skin ground lifted toward white, so the hue survives. */
    val cardFill: Color get() = lerp(Industry.bg, Color.White, 0.55f)

    /** The soft card outline: neutral-300 thinned out. */
    val cardBorder: Color get() = Industry.neutral300.copy(alpha = 0.75f)
}

/**
 * The finished-product card: subtle skin-toned fill, rounded corners, a very
 * soft border and gentle elevation. Selected/occupied surfaces pass
 * accent100 fill + accent border instead. Content and ripples clip to the
 * shape, so callers no longer stack their own square backgrounds.
 */
fun Modifier.deskCard(
    shape: Shape = DeskStyle.cardShape,
    fill: Color = DeskStyle.cardFill,
    border: Color = DeskStyle.cardBorder,
    elevation: Dp = DeskStyle.cardElevation,
): Modifier = this
    .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
    .background(fill, shape)
    .border(1.dp, border, shape)
    .clip(shape)
