package org.dhamma.dipi.staff.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic text roles for v7. Roles pick existing ramp steps — they do not
 * rewrite the Industry ladder. Dark always uses Steel night, regardless of
 * the remembered Light skin.
 */
data class ReadableTokens(
    val primary: Color,
    val secondary: Color,
    val caption: Color,
    val bodyOnCard: Color,
    val recordedText: Color,
    val recordedFill: Color,
    val blankText: Color,
    val blankFill: Color,
)

fun readableTokens(palette: IndustryPalette, dark: Boolean): ReadableTokens {
    if (dark) {
        return ReadableTokens(
            primary = DarkDipi.foreground,
            secondary = DarkDipi.muted,
            caption = DarkDipi.muted,
            bodyOnCard = DarkDipi.foreground,
            recordedText = DarkDipi.foreground,
            recordedFill = DarkDipi.hover,
            blankText = DarkDipi.muted,
            blankFill = DarkDipi.field,
        )
    }
    return ReadableTokens(
        primary = palette.text,
        secondary = palette.neutral800,
        caption = palette.neutral700,
        bodyOnCard = palette.text,
        recordedText = palette.neutral800,
        recordedFill = palette.neutral100,
        blankText = palette.neutral700,
        blankFill = palette.neutral200,
    )
}

val LocalReadableTokens = staticCompositionLocalOf {
    readableTokens(IndustryPalette.Steel, dark = false)
}
