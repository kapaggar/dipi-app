package org.dhamma.dipi.staff.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dhamma.dipi.staff.ui.R

/**
 * One CSS `radial-gradient(rx ry at cx cy, accent a, transparent fade)` —
 * the version-3 ambient wash. `cxFraction`/`cyFraction` are fractions of the
 * host size (the design uses 108% / −4% style positions, so values may run
 * outside 0..1).
 */
data class AccentWash(
    val rx: Dp,
    val ry: Dp,
    val cxFraction: Float,
    val cyFraction: Float,
    val alpha: Float,
    val fade: Float = 0.72f,
)

/** Draws the given washes in the accent colour behind the content. */
fun Modifier.accentWash(accent: Color, vararg washes: AccentWash): Modifier = drawBehind {
    for (w in washes) {
        val rx = w.rx.toPx()
        val ry = w.ry.toPx()
        if (rx <= 0f || ry <= 0f) continue
        val center = Offset(size.width * w.cxFraction, size.height * w.cyFraction)
        // The ellipse is a circle of radius rx squashed vertically about its centre.
        scale(scaleX = 1f, scaleY = ry / rx, pivot = center) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to accent.copy(alpha = w.alpha),
                    w.fade to accent.copy(alpha = 0f),
                    center = center,
                    radius = rx,
                ),
                radius = rx,
                center = center,
            )
        }
    }
}

/**
 * The lotus mark washed into a skin's ground: the circular app icon run
 * through the skin's `--sk-markf` filter, faded out radially
 * (solid to 42%, gone by 78%) and held at the skin's watermark opacity.
 * Static by design — Industry motion is progress and toggles only.
 */
@Composable
fun LotusWatermark(
    size: Dp,
    opacity: Float,
    modifier: Modifier = Modifier,
    skin: DeskSkin = Industry.skin,
) {
    val filter = remember(skin) { skin.markColorFilter() }
    Image(
        painter = painterResource(R.drawable.lotus_mark),
        contentDescription = null,
        colorFilter = filter,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                alpha = opacity
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.radialGradient(
                        0.42f to Color.Black,
                        0.78f to Color.Transparent,
                        center = this.size.center,
                        radius = this.size.minDimension / 2f,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    )
}

/**
 * Pre-auth lotus relief: the circular mark at readable opacity (no skin
 * tint — that washed it into the paper), radially masked so the asset's
 * black square disappears, then a vertical gradient so it dissolves into
 * the sign-in card.
 */
@Composable
fun LoginLotusRelief(
    modifier: Modifier = Modifier,
    opacity: Float = 0.34f,
) {
    val paper = LocalDipi.current.background
    Box(modifier.fillMaxSize().testTag("login-lotus")) {
        Image(
            painter = painterResource(R.drawable.lotus_mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = opacity
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            0.38f to Color.Black,
                            0.82f to Color.Transparent,
                            center = this.size.center,
                            radius = this.size.minDimension / 2f,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to paper.copy(alpha = 0.08f),
                        0.44f to paper.copy(alpha = 0.46f),
                        0.74f to paper.copy(alpha = 0.86f),
                        0.97f to paper,
                    ),
                ),
        )
    }
}

private val androidx.compose.ui.geometry.Size.center: Offset
    get() = Offset(width / 2f, height / 2f)

/** The desk frame's two washes (top-right 13%, bottom-left 9%). */
fun Modifier.deskWash(accent: Color): Modifier = accentWash(
    accent,
    AccentWash(rx = 660.dp, ry = 400.dp, cxFraction = 1f, cyFraction = 0f, alpha = 0.13f, fade = 0.72f),
    AccentWash(rx = 420.dp, ry = 300.dp, cxFraction = 0f, cyFraction = 1f, alpha = 0.09f, fade = 0.70f),
)

/** The phone frame's single top-right wash (14%). */
fun Modifier.phoneWash(accent: Color): Modifier = accentWash(
    accent,
    AccentWash(rx = 320.dp, ry = 240.dp, cxFraction = 1.08f, cyFraction = -0.04f, alpha = 0.14f, fade = 0.72f),
)
