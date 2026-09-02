package org.dhamma.dipi.staff

import androidx.compose.ui.graphics.Color
import org.dhamma.dipi.staff.ui.theme.DarkDipi
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.lightDipi
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The v4 "Steel night" ramp (`docs/DESIGN.md`, Design tokens) and the
 * fixed severity pair (spec R6): dark mode is one night ladder for every skin,
 * and `hard` never follows the skin.
 */
class DarkTokensTest {

    @Test
    fun darkGroundAndTextAreTheNightRamp() {
        assertEquals(Color(0xFF14171A), DarkDipi.background)
        assertEquals(Color(0xFFE4E6E9), DarkDipi.foreground)
        assertEquals(Color(0xFF9BA1A8), DarkDipi.muted)
        assertEquals(Color(0xFF1D2D3D), DarkDipi.tint)
        assertEquals(Color(0xFF5980A6), DarkDipi.accent)
    }

    @Test
    fun darkSurfacesClimbTheNightLadderInLightThemeRoleOrder() {
        // field (card ground) < hover < hairline < hairlineStrong, the same
        // ordering the light builder reads off neutral100..neutral400.
        assertEquals(Color(0xFF1A1E22), DarkDipi.field)
        assertEquals(Color(0xFF22272C), DarkDipi.hover)
        assertEquals(Color(0xFF2E3339), DarkDipi.hairline)
        assertEquals(Color(0xFF3A4046), DarkDipi.hairlineStrong)
    }

    @Test
    fun severityIsAFixedLightDarkPairAcrossEverySkin() {
        assertEquals(Color(0xFFE0796F), DarkDipi.hard)
        assertEquals(Color(0xFFA33A34), lightDipi(IndustryPalette.Steel).hard)
        assertEquals(Color(0xFFA33A34), lightDipi(IndustryPalette.of(DeskSkin.Blossom)).hard)
    }
}
