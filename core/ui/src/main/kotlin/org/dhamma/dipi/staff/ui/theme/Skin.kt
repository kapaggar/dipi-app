package org.dhamma.dipi.staff.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import org.dhamma.dipi.staff.ui.R
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * The five skins of the version-3 design ("Five skins, one wireframe").
 * Steel keeps the hand-picked hex ramp the wireframe was drawn in; the other
 * four derive every token in OKLCH from one (hue, chroma) pair on a shared
 * lightness ladder, so no step changes visual weight when the hue does.
 *
 * `markOpacity` and the mark-filter fields mirror the design's `--sk-op` and
 * `--sk-markf` — how the lotus watermark is tinted into each skin's ground.
 */
enum class DeskSkin(
    val key: String,
    val label: String,
    val hue: Double,
    val chroma: Double,
    @DrawableRes val photoRes: Int,
    val markOpacity: Float,
) {
    Steel("steel", "Steel", 245.0, 0.07, R.drawable.skin_photo_steel, 0.11f),
    Paper("paper", "Paper", 262.0, 0.03, R.drawable.skin_photo_paper, 0.18f),
    Blossom("blossom", "Blossom", 352.0, 0.095, R.drawable.skin_photo_blossom, 0.17f),
    Pond("pond", "Pond", 152.0, 0.07, R.drawable.skin_photo_pond, 0.15f),
    Still("still", "Still", 272.0, 0.095, R.drawable.skin_photo_still, 0.16f),
    ;

    companion object {
        fun fromKey(key: String?): DeskSkin = entries.firstOrNull { it.key == key } ?: Steel
    }
}

/** The full Industry token block one skin resolves to. */
data class IndustryPalette(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val neutral100: Color,
    val neutral200: Color,
    val neutral300: Color,
    val neutral400: Color,
    val neutral500: Color,
    val neutral600: Color,
    val neutral700: Color,
    val neutral800: Color,
    val neutral900: Color,
    val accent: Color,
    val accent100: Color,
    val accent200: Color,
    val accent300: Color,
    val accent400: Color,
    val accent500: Color,
    val accent600: Color,
    val accent700: Color,
    val accent800: Color,
    val accent900: Color,
) {
    companion object {
        /** The wireframe as drawn — `.sk-steel`'s explicit hexes. */
        val Steel = IndustryPalette(
            bg = Color(0xFFF2F2F3),
            surface = Color(0xFFE9E9EA),
            text = Color(0xFF1D1F20),
            neutral100 = Color(0xFFF5F5F8),
            neutral200 = Color(0xFFE7E7EA),
            neutral300 = Color(0xFFD4D4D7),
            neutral400 = Color(0xFFB7B7BA),
            neutral500 = Color(0xFF98989B),
            neutral600 = Color(0xFF7A7A7D),
            neutral700 = Color(0xFF5D5D60),
            neutral800 = Color(0xFF424244),
            neutral900 = Color(0xFF2B2B2D),
            accent = Color(0xFF5980A6),
            accent100 = Color(0xFFEEF6FF),
            accent200 = Color(0xFFD6EBFF),
            accent300 = Color(0xFFB5D9FD),
            accent400 = Color(0xFF94BCE3),
            accent500 = Color(0xFF749DC4),
            accent600 = Color(0xFF597EA3),
            accent700 = Color(0xFF416180),
            accent800 = Color(0xFF2C455D),
            accent900 = Color(0xFF1D2D3D),
        )

        /**
         * `.sk-paper/.sk-blossom/.sk-pond/.sk-still` — the OKLCH ladder from
         * the design's CSS, verbatim: same lightness steps, chroma factors
         * per step, hue constant.
         */
        fun of(skin: DeskSkin): IndustryPalette {
            if (skin == DeskSkin.Steel) return Steel
            val h = skin.hue
            val c = skin.chroma
            return IndustryPalette(
                bg = oklch(0.974, c * 0.16, h),
                surface = oklch(0.952, c * 0.14, h),
                text = oklch(0.23, c * 0.25, h),
                neutral100 = oklch(0.976, c * 0.12, h),
                neutral200 = oklch(0.936, c * 0.12, h),
                neutral300 = oklch(0.875, c * 0.12, h),
                neutral400 = oklch(0.785, c * 0.12, h),
                neutral500 = oklch(0.67, c * 0.12, h),
                neutral600 = oklch(0.55, c * 0.14, h),
                neutral700 = oklch(0.45, c * 0.14, h),
                neutral800 = oklch(0.35, c * 0.14, h),
                neutral900 = oklch(0.26, c * 0.14, h),
                accent = oklch(0.56, c, h),
                accent100 = oklch(0.97, c * 0.3, h),
                accent200 = oklch(0.93, c * 0.55, h),
                accent300 = oklch(0.87, c * 0.75, h),
                accent400 = oklch(0.78, c * 0.92, h),
                accent500 = oklch(0.56, c, h),
                accent600 = oklch(0.50, c, h),
                accent700 = oklch(0.43, c, h),
                accent800 = oklch(0.35, c * 0.92, h),
                accent900 = oklch(0.26, c * 0.8, h),
            )
        }
    }
}

/**
 * OKLCH → sRGB (Björn Ottosson's OKLab, D65). Out-of-gamut components are
 * clamped in linear light — at the design's chromas (≤ .095) nothing strays
 * far enough to distort.
 */
fun oklch(lightness: Double, chroma: Double, hueDegrees: Double): Color {
    val hr = Math.toRadians(hueDegrees)
    val a = chroma * cos(hr)
    val b = chroma * sin(hr)
    val l1 = lightness + 0.3963377774 * a + 0.2158037573 * b
    val m1 = lightness - 0.1055613458 * a - 0.0638541728 * b
    val s1 = lightness - 0.0894841775 * a - 1.2914855480 * b
    val l = l1 * l1 * l1
    val m = m1 * m1 * m1
    val s = s1 * s1 * s1
    val rLin = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    return Color(gamma(rLin), gamma(gLin), gamma(bLin))
}

private fun gamma(linear: Double): Float {
    val v = linear.coerceIn(0.0, 1.0)
    val srgb = if (v <= 0.0031308) 12.92 * v else 1.055 * v.pow(1.0 / 2.4) - 0.055
    return srgb.toFloat().coerceIn(0f, 1f)
}

/**
 * The skin switcher's 135° gradient chip: steel is the drawn ramp,
 * the others read `oklch(88% c·.75 h) → oklch(56% c h) → oklch(30% c·.8 h)`.
 */
fun DeskSkin.chipGradientColors(): List<Color> = if (this == DeskSkin.Steel) {
    listOf(Color(0xFFB5D9FD), Color(0xFF5980A6), Color(0xFF2F4A66))
} else {
    listOf(oklch(0.88, chroma * 0.75, hue), oklch(0.56, chroma, hue), oklch(0.30, chroma * 0.8, hue))
}

/**
 * The `--sk-markf` CSS filter for the lotus watermark, as a colour matrix.
 * Filter functions compose left-to-right, so the right operand of `*=`
 * is the one that runs first.
 */
fun DeskSkin.markColorFilter(): ColorFilter? = when (this) {
    // saturate(.45) hue-rotate(165deg)
    DeskSkin.Steel -> ColorFilter.colorMatrix(hueRotateMatrix(165f).apply { timesAssign(saturationMatrix(0.45f)) })
    // grayscale(.92) == saturate(.08)
    DeskSkin.Paper -> ColorFilter.colorMatrix(saturationMatrix(0.08f))
    DeskSkin.Blossom -> null
    // hue-rotate(76deg) saturate(.8)
    DeskSkin.Pond -> ColorFilter.colorMatrix(saturationMatrix(0.8f).apply { timesAssign(hueRotateMatrix(76f)) })
    // hue-rotate(202deg) saturate(.85)
    DeskSkin.Still -> ColorFilter.colorMatrix(saturationMatrix(0.85f).apply { timesAssign(hueRotateMatrix(202f)) })
}

private fun saturationMatrix(s: Float): ColorMatrix = ColorMatrix().apply { setToSaturation(s) }

/** CSS `hue-rotate()` matrix (Filter Effects Module Level 1). */
private fun hueRotateMatrix(degrees: Float): ColorMatrix {
    val r = Math.toRadians(degrees.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    return ColorMatrix(
        floatArrayOf(
            0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0f, 0f,
            0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0f, 0f,
            0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}
