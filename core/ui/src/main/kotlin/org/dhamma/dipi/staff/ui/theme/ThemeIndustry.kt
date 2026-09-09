package org.dhamma.dipi.staff.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Composition-scoped UI palette. Print and stored skin selection keep the original Industry palette. */
object ThemeIndustry {
    val bg: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.bg
    val surface: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.surface
    val text: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.text
    val neutral100: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral100
    val neutral200: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral200
    val neutral300: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral300
    val neutral400: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral400
    val neutral500: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral500
    val neutral600: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral600
    val neutral700: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral700
    val neutral800: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral800
    val neutral900: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.neutral900
    val accent: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent
    val accent100: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent100
    val accent200: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent200
    val accent300: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent300
    val accent400: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent400
    val accent500: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent500
    val accent600: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent600
    val accent700: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent700
    val accent800: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent800
    val accent900: Color @Composable @ReadOnlyComposable get() = LocalIndustry.current.accent900
    val card: Color @Composable @ReadOnlyComposable get() = LocalDipi.current.field
    val caption: Color @Composable @ReadOnlyComposable get() = LocalReadableTokens.current.caption
    val secondary: Color @Composable @ReadOnlyComposable get() = LocalReadableTokens.current.secondary
}

/** One Steel night palette, independent of the remembered light skin. Ramp boundaries stay boundaries. */
val SteelNightPalette = IndustryPalette.Steel.copy(
    bg = DarkDipi.background, surface = DarkDipi.hover, text = DarkDipi.foreground,
    neutral100 = DarkDipi.field, neutral200 = DarkDipi.hover,
    neutral300 = DarkDipi.hairline, neutral400 = DarkDipi.hairlineStrong,
    neutral500 = Color(0xFF6B7278), neutral600 = DarkDipi.muted,
    neutral700 = Color(0xFFB7BDC3), neutral800 = Color(0xFFD0D5DA), neutral900 = DarkDipi.foreground,
    accent100 = DarkDipi.tint, accent200 = Color(0xFF22384C), accent300 = Color(0xFF416180),
    accent400 = Color(0xFF749DC4), accent500 = Color(0xFF94BCE3),
    accent700 = Color(0xFFB5D9FD), accent800 = Color(0xFFD6EBFF), accent900 = Color(0xFFEEF6FF),
)
