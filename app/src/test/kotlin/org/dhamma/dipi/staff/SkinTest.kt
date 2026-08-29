package org.dhamma.dipi.staff

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.dhamma.dipi.staff.model.StatusTone
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.chipGradientColors
import org.dhamma.dipi.staff.ui.theme.lightDipi
import org.dhamma.dipi.staff.ui.theme.markColorFilter
import org.dhamma.dipi.staff.ui.theme.oklch
import org.dhamma.dipi.staff.ui.theme.statusColors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkinTest {

    @After
    fun resetSkin() {
        Industry.apply(DeskSkin.Steel)
    }

    // ── OKLCH → sRGB converter, against independently computed references ──

    private fun assertHex(expected: Long, actual: Color) {
        val e = Color(expected).toArgb()
        val a = actual.toArgb()
        // Allow ±1/255 per channel for float rounding.
        for (shift in intArrayOf(16, 8, 0)) {
            val ec = (e shr shift) and 0xFF
            val ac = (a shr shift) and 0xFF
            assertTrue(
                "channel@$shift expected $ec got $ac (expected ${"%08x".format(e)} got ${"%08x".format(a)})",
                kotlin.math.abs(ec - ac) <= 1,
            )
        }
    }

    @Test
    fun oklchConvertsAchromaticAnchors() {
        assertHex(0xFFFFFFFF, oklch(1.0, 0.0, 0.0))
        assertHex(0xFF000000, oklch(0.0, 0.0, 0.0))
        assertHex(0xFF636363, oklch(0.5, 0.0, 0.0))
    }

    @Test
    fun oklchConvertsEachSkinAccent() {
        // oklch(56% c h) for each non-steel skin, references computed offline.
        assertHex(0xFF6B7587, oklch(0.56, 0.03, 262.0)) // paper
        assertHex(0xFF9F5C7A, oklch(0.56, 0.095, 352.0)) // blossom
        assertHex(0xFF548160, oklch(0.56, 0.07, 152.0)) // pond
        assertHex(0xFF6170AC, oklch(0.56, 0.095, 272.0)) // still
    }

    @Test
    fun oklchConvertsBackgroundsAndTints() {
        assertHex(0xFFF5F6FA, oklch(0.974, 0.03 * 0.16, 262.0)) // paper bg
        assertHex(0xFFF1F9F3, oklch(0.974, 0.07 * 0.16, 152.0)) // pond bg
        assertHex(0xFFFFEEF7, oklch(0.97, 0.095 * 0.3, 352.0)) // blossom accent-100
    }

    // ── Palette ladders ──

    @Test
    fun steelKeepsTheHandPickedHexes() {
        val p = IndustryPalette.of(DeskSkin.Steel)
        assertEquals(Color(0xFF5980A6), p.accent)
        assertEquals(Color(0xFFEEF6FF), p.accent100)
        assertEquals(Color(0xFF2C455D), p.accent800)
        assertEquals(Color(0xFFF2F2F3), p.bg)
        assertEquals(Color(0xFF1D1F20), p.text)
        assertEquals(Color(0xFF7A7A7D), p.neutral600)
        assertEquals(Color(0xFF2B2B2D), p.neutral900)
    }

    @Test
    fun nonSteelPalettesFollowTheOklchLadder() {
        for (skin in DeskSkin.entries.filter { it != DeskSkin.Steel }) {
            val p = IndustryPalette.of(skin)
            assertEquals(oklch(0.56, skin.chroma, skin.hue), p.accent)
            assertEquals(p.accent, p.accent500)
            assertEquals(oklch(0.97, skin.chroma * 0.3, skin.hue), p.accent100)
            assertEquals(oklch(0.26, skin.chroma * 0.8, skin.hue), p.accent900)
            assertEquals(oklch(0.974, skin.chroma * 0.16, skin.hue), p.bg)
            assertEquals(oklch(0.952, skin.chroma * 0.14, skin.hue), p.surface)
            assertEquals(oklch(0.23, skin.chroma * 0.25, skin.hue), p.text)
            assertEquals(oklch(0.976, skin.chroma * 0.12, skin.hue), p.neutral100)
            assertEquals(oklch(0.55, skin.chroma * 0.14, skin.hue), p.neutral600)
            assertEquals(oklch(0.26, skin.chroma * 0.14, skin.hue), p.neutral900)
        }
    }

    @Test
    fun skinKeysRoundTripAndDefaultToSteel() {
        for (skin in DeskSkin.entries) assertEquals(skin, DeskSkin.fromKey(skin.key))
        assertEquals(DeskSkin.Steel, DeskSkin.fromKey(null))
        assertEquals(DeskSkin.Steel, DeskSkin.fromKey("mahogany"))
    }

    @Test
    fun chipGradientsMatchTheDesign() {
        assertEquals(
            listOf(Color(0xFFB5D9FD), Color(0xFF5980A6), Color(0xFF2F4A66)),
            DeskSkin.Steel.chipGradientColors(),
        )
        val pond = DeskSkin.Pond.chipGradientColors()
        assertEquals(oklch(0.88, 0.07 * 0.75, 152.0), pond[0])
        assertEquals(oklch(0.56, 0.07, 152.0), pond[1])
        assertEquals(oklch(0.30, 0.07 * 0.8, 152.0), pond[2])
    }

    @Test
    fun watermarkFiltersFollowTheSkin() {
        // Blossom takes the mark unfiltered; every other skin tints it.
        assertNull(DeskSkin.Blossom.markColorFilter())
        for (skin in DeskSkin.entries.filter { it != DeskSkin.Blossom }) {
            assertTrue(skin.markColorFilter() != null)
        }
    }

    // ── Industry switching + theme remaps ──

    @Test
    fun applyingASkinRecoloursIndustryAndStatusReceived() {
        Industry.apply(DeskSkin.Blossom)
        val p = IndustryPalette.of(DeskSkin.Blossom)
        assertEquals(p.accent, Industry.accent)
        assertEquals(p.bg, Industry.bg)
        // Received/Reconfirmation light bg follows the skin's accent-100…
        assertEquals(p.accent100, statusColors(StatusTone.Received, dark = false).first)
        // …while its fg and dark mode stay steel, and other tones stay fixed.
        assertEquals(Color(0xFF2C455D), statusColors(StatusTone.Received, dark = false).second)
        assertEquals(Color(0xFF22384C), statusColors(StatusTone.Received, dark = true).first)
        assertEquals(Color(0xFFDFEAE1), statusColors(StatusTone.Confirmed, dark = false).first)
        assertEquals(Color(0xFFF0E3E3), statusColors(StatusTone.Cancelled, dark = false).first)
    }

    @Test
    fun lightThemeReadsThePaletteDarkStaysSteel() {
        val pond = IndustryPalette.of(DeskSkin.Pond)
        val light = lightDipi(pond)
        assertEquals(pond.bg, light.background)
        assertEquals(pond.text, light.foreground)
        assertEquals(pond.neutral600, light.muted)
        assertEquals(pond.neutral100, light.field)
        assertEquals(pond.accent100, light.tint)
        assertEquals(pond.accent, light.accent)
        assertEquals(pond.accent700, light.accentPressed)
        // Severities that carry meaning stay fixed.
        assertEquals(Color(0xFFA33A34), light.hard)
        assertEquals(Color(0xFF6A5A38), light.safety)
    }
}
