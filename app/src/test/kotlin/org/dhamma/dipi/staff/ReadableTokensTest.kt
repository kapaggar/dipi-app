package org.dhamma.dipi.staff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.dhamma.dipi.staff.ui.theme.DarkDipi
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.lightDipi
import org.dhamma.dipi.staff.ui.theme.readableTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableTokensTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val fg = foreground.compositeOver(background)
        val a = fg.luminance().toDouble()
        val b = background.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    @Test
    fun lightRolesMeetContrastOnPaletteSurfaces() {
        for (skin in DeskSkin.entries) {
            val palette = IndustryPalette.of(skin)
            val tokens = readableTokens(palette, dark = false)
            val light = lightDipi(palette)
            assertTrue("$skin primary on bg", contrastRatio(tokens.primary, light.background) >= 4.5)
            assertTrue("$skin secondary on bg", contrastRatio(tokens.secondary, light.background) >= 4.5)
            assertTrue("$skin caption on selected seat", contrastRatio(tokens.caption, palette.accent100) >= 4.5)
            assertTrue("$skin caption on card", contrastRatio(tokens.caption, light.field) >= 4.5)
            assertTrue("$skin caption on bg", contrastRatio(tokens.caption, light.background) >= 4.5)
            assertTrue("$skin body on field", contrastRatio(tokens.bodyOnCard, light.field) >= 4.5)
            assertTrue("$skin recorded on fill", contrastRatio(tokens.recordedText, tokens.recordedFill) >= 4.5)
            assertTrue("$skin blank on fill", contrastRatio(tokens.blankText, tokens.blankFill) >= 4.5)
            assertEquals(palette.text, tokens.primary)
            assertEquals(palette.neutral800, tokens.secondary)
            assertEquals(palette.neutral700, tokens.caption)
        }
    }

    @Test
    fun darkRolesUseSteelNightOnEverySkin() {
        for (skin in DeskSkin.entries) {
            val tokens = readableTokens(IndustryPalette.of(skin), dark = true)
            assertEquals(DarkDipi.foreground, tokens.primary)
            assertEquals(DarkDipi.muted, tokens.secondary)
            assertEquals(DarkDipi.foreground, tokens.bodyOnCard)
            assertTrue("$skin dark caption on selected seat", contrastRatio(tokens.caption, DarkDipi.tint) >= 4.5)
            assertTrue("$skin dark primary", contrastRatio(tokens.primary, DarkDipi.background) >= 4.5)
            assertTrue("$skin dark secondary", contrastRatio(tokens.secondary, DarkDipi.background) >= 4.5)
            assertTrue("$skin dark body", contrastRatio(tokens.bodyOnCard, DarkDipi.field) >= 4.5)
            assertTrue("$skin dark recorded", contrastRatio(tokens.recordedText, tokens.recordedFill) >= 4.5)
            assertTrue("$skin dark blank", contrastRatio(tokens.blankText, tokens.blankFill) >= 4.5)
        }
    }
}
