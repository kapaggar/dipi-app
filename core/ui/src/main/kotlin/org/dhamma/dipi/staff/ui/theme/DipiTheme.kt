package org.dhamma.dipi.staff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.dhamma.dipi.staff.model.StatusTone
import org.dhamma.dipi.staff.ui.R

data class DipiColors(
    val background: Color,
    val foreground: Color,
    val muted: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val hover: Color,
    val field: Color,
    val tint: Color,
    val accent: Color,
    val accentPressed: Color,
    val snack: Color,
    val snackError: Color,
    val hard: Color,
    val safety: Color,
    val soft: Color,
    val flagHard: Color,
    val flagSoft: Color,
    val photoFixed: Color,
    val photoAuto: Color,
    val photoSuggest: Color,
    val photoNone: Color,
)

/**
 * The version-3 light theme reads the active skin's token block
 * (`THEME.light` var-ified in the design): background/text/hairlines follow
 * the neutrals, `muted` is neutral-600, `field` neutral-100, `tint`
 * accent-100. Hard/safety severities and the photo badges stay fixed hexes —
 * they carry meaning, not mood.
 */
fun lightDipi(palette: IndustryPalette): DipiColors = DipiColors(
    background = palette.bg,
    foreground = palette.text,
    muted = palette.neutral600,
    hairline = palette.neutral300,
    hairlineStrong = palette.neutral400,
    hover = palette.neutral200,
    field = palette.neutral100,
    tint = palette.accent100,
    accent = palette.accent,
    accentPressed = palette.accent700,
    snack = Color(0xFF2B2B2D),
    snackError = Color(0xFF5A2F2F),
    hard = Color(0xFF7A4141),
    safety = Color(0xFF6A5A38),
    soft = palette.neutral600,
    flagHard = Color(0xFFA15C5C),
    flagSoft = Color(0xFF8A7645),
    photoFixed = Color(0xFF3D6B52),
    photoAuto = Color(0xFF5A63A8),
    photoSuggest = Color(0xFF8A6A35),
    photoNone = Color(0xFF6C7075),
)

/** Steel's light block — the wireframe as drawn. */
val LightDipi = lightDipi(IndustryPalette.Steel)

val DarkDipi = DipiColors(
    background = Color(0xFF14181C),
    foreground = Color(0xFFECEFF2),
    muted = Color(0xFF98A1A8),
    hairline = Color(0xFF232A31),
    hairlineStrong = Color(0xFF3A424A),
    hover = Color(0xFF1C2229),
    field = Color(0xFF1C2229),
    tint = Color(0xFF22384C),
    accent = Color(0xFF5980A6),
    accentPressed = Color(0xFF416180),
    snack = Color(0xFF0D1114),
    snackError = Color(0xFF5A2F2F),
    hard = Color(0xFFDEAEAE),
    safety = Color(0xFFDBCBA6),
    soft = Color(0xFF7A7A7D),
    flagHard = Color(0xFFA15C5C),
    flagSoft = Color(0xFF8A7645),
    photoFixed = Color(0xFF3D6B52),
    photoAuto = Color(0xFF5A63A8),
    photoSuggest = Color(0xFF8A6A35),
    photoNone = Color(0xFF6C7075),
)

val LocalDipi = staticCompositionLocalOf { LightDipi }

/** The active skin's full Industry token block, provided by [DipiTheme]. */
val LocalIndustry = staticCompositionLocalOf { IndustryPalette.Steel }

/**
 * Status chip colours. Fixed hexes across skins with one exception:
 * Received/Reconfirmation's light background is the skin's accent-100
 * (`TONE.Received.bg` = `var(--color-accent-100)` in the design). Dark mode
 * stays steel throughout.
 */
fun statusColors(tone: StatusTone, dark: Boolean): Pair<Color, Color> = when (tone) {
    StatusTone.Confirmed -> if (dark) Color(0xFF22392C) to Color(0xFFA9CDB6) else Color(0xFFDFEAE1) to Color(0xFF2F5A41)
    StatusTone.Pending -> if (dark) Color(0xFF2A3138) to Color(0xFFC0C7CD) else Color(0xFFE7E7EA) to Color(0xFF5D5D60)
    StatusTone.Received -> if (dark) Color(0xFF22384C) to Color(0xFFB5D9FD) else Industry.accent100 to Color(0xFF2C455D)
    StatusTone.Expected -> if (dark) Color(0xFF3A3223) to Color(0xFFDBCBA6) else Color(0xFFF0ECE2) to Color(0xFF6A5A38)
    StatusTone.Cancelled -> if (dark) Color(0xFF3B2626) to Color(0xFFDEAEAE) else Color(0xFFF0E3E3) to Color(0xFF7A4141)
}

val DipiSans = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
)
val DipiCondensed = FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)
val DipiMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

@Composable
fun DipiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Industry.palette is snapshot state — switching the skin recomposes here
    // and re-provides both locals. Dark mode stays steel by design.
    val palette = Industry.palette
    val colors = if (dark) DarkDipi else lightDipi(palette)
    val scheme = if (dark) {
        darkColorScheme(
            background = colors.background,
            surface = colors.background,
            onBackground = colors.foreground,
            onSurface = colors.foreground,
            primary = colors.accent,
            onPrimary = Color.White,
        )
    } else {
        lightColorScheme(
            background = colors.background,
            surface = colors.field,
            onBackground = colors.foreground,
            onSurface = colors.foreground,
            primary = colors.accent,
            onPrimary = Color.White,
        )
    }
    CompositionLocalProvider(LocalDipi provides colors, LocalIndustry provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
