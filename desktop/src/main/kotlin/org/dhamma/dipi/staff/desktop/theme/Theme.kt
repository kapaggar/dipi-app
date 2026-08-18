package org.dhamma.dipi.staff.desktop.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.dhamma.dipi.staff.model.StatusTone

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
)

/** OLED-first dark: true black so the Steam Deck panel can turn pixels off. */
val DarkDipi = DipiColors(
    background = Color(0xFF000000),
    foreground = Color(0xFFECEFF2),
    muted = Color(0xFF98A1A8),
    hairline = Color(0xFF1C2228),
    hairlineStrong = Color(0xFF3A424A),
    hover = Color(0xFF141A20),
    field = Color(0xFF12181E),
    tint = Color(0xFF1A2E40),
    accent = Color(0xFF6A9BC4),
    accentPressed = Color(0xFF416180),
    snack = Color(0xFF0D1114),
    snackError = Color(0xFF5A2F2F),
    hard = Color(0xFFDEAEAE),
    safety = Color(0xFFDBCBA6),
    soft = Color(0xFF7A7A7D),
    flagHard = Color(0xFFA15C5C),
    flagSoft = Color(0xFF8A7645),
)

val LightDipi = DipiColors(
    background = Color(0xFFF4F1EA),
    foreground = Color(0xFF1D1F20),
    muted = Color(0xFF5D5D60),
    hairline = Color(0xFFD4D0C8),
    hairlineStrong = Color(0xFFB8B3A8),
    hover = Color(0xFFE8E4DB),
    field = Color(0xFFECE8E0),
    tint = Color(0xFFD8E4EE),
    accent = Color(0xFF3A5A78),
    accentPressed = Color(0xFF2C455D),
    snack = Color(0xFF2B2B2D),
    snackError = Color(0xFF5A2F2F),
    hard = Color(0xFF7A4141),
    safety = Color(0xFF6A5A38),
    soft = Color(0xFF5D5D60),
    flagHard = Color(0xFFA15C5C),
    flagSoft = Color(0xFF8A7645),
)

val LocalDipi = staticCompositionLocalOf { DarkDipi }

fun statusColors(tone: StatusTone, dark: Boolean): Pair<Color, Color> = when (tone) {
    StatusTone.Confirmed -> if (dark) Color(0xFF22392C) to Color(0xFFA9CDB6) else Color(0xFFDFEAE1) to Color(0xFF2F5A41)
    StatusTone.Pending -> if (dark) Color(0xFF2A3138) to Color(0xFFC0C7CD) else Color(0xFFE7E7EA) to Color(0xFF5D5D60)
    StatusTone.Received -> if (dark) Color(0xFF22384C) to Color(0xFFB5D9FD) else Color(0xFFD8E4EE) to Color(0xFF2C455D)
    StatusTone.Expected -> if (dark) Color(0xFF3A3223) to Color(0xFFDBCBA6) else Color(0xFFF0ECE2) to Color(0xFF6A5A38)
    StatusTone.Cancelled -> if (dark) Color(0xFF3B2626) to Color(0xFFDEAEAE) else Color(0xFFF0E3E3) to Color(0xFF7A4141)
}

@Composable
fun DipiTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dark) DarkDipi else LightDipi
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
    CompositionLocalProvider(LocalDipi provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
