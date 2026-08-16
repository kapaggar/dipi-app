package org.dhamma.dipi.staff.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

/**
 * Industry design-system tokens, mirrored from
 * `version-3/project/_ds/industry-…/styles.css` `:root` plus the version-3
 * skin blocks (`.sk-steel` … `.sk-still`).
 *
 * Usage discipline (non-negotiable): the accent is the only colour and it
 * means one thing — live, occupied, or selected. accent100 for selected fills
 * and hovers, accent for active bars / ticks / solid primaries, accent600 for
 * pressed, accent700–800 for text on tinted fills and numerals. Everything
 * else is a hairline drawing on the neutral ramp.
 *
 * Since version-3 the whole block re-colours per skin: tokens are backed by
 * snapshot state, so composables reading them recompose when [apply] switches
 * the skin. Steel (the wireframe as drawn) is the default.
 */
object Industry {
    private val skinState = mutableStateOf(DeskSkin.Steel)
    private val paletteState = mutableStateOf(IndustryPalette.Steel)

    /** The active skin. */
    val skin: DeskSkin get() = skinState.value

    /** The active skin's full token block. */
    val palette: IndustryPalette get() = paletteState.value

    /** Switch every token to [skin]'s block. Status colours stay put — they carry meaning, not mood. */
    fun apply(skin: DeskSkin) {
        skinState.value = skin
        paletteState.value = IndustryPalette.of(skin)
    }

    val bg: Color get() = palette.bg
    val surface: Color get() = palette.surface
    val text: Color get() = palette.text

    val neutral100: Color get() = palette.neutral100
    val neutral200: Color get() = palette.neutral200
    val neutral300: Color get() = palette.neutral300
    val neutral400: Color get() = palette.neutral400
    val neutral500: Color get() = palette.neutral500
    val neutral600: Color get() = palette.neutral600
    val neutral700: Color get() = palette.neutral700
    val neutral800: Color get() = palette.neutral800
    val neutral900: Color get() = palette.neutral900

    val accent: Color get() = palette.accent
    val accent100: Color get() = palette.accent100
    val accent200: Color get() = palette.accent200
    val accent300: Color get() = palette.accent300
    val accent400: Color get() = palette.accent400
    val accent500: Color get() = palette.accent500
    val accent600: Color get() = palette.accent600
    val accent700: Color get() = palette.accent700
    val accent800: Color get() = palette.accent800
    val accent900: Color get() = palette.accent900

    /** Backdrop behind the check-in dialog: rgba(29,31,32,.42). Fixed across skins. */
    val scrim = Color(0x6B1D1F20)
}
