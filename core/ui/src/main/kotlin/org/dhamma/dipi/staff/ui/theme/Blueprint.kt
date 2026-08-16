package org.dhamma.dipi.staff.ui.theme

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Historical name, new rendering (sleek pass, owner decision 2026-08-16):
 * the blueprint frame no longer draws the `+` corner registration marks —
 * it is a soft rounded hairline in the given colour. Surfaces that want the
 * full card treatment (fill + elevation) use [deskCard] instead.
 */
fun Modifier.blueprint(border: Color): Modifier =
    this.border(1.dp, border, DeskStyle.cardShape)

/**
 * The old Industry registration marks — 9dp arms, 50% opacity, 1dp accent
 * cross, offset 5dp outside each corner. Kept for reference; no live surface
 * draws them since the sleek pass.
 */
fun Modifier.blueprintMarks(marks: Color = Industry.accent): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val outside = 5.dp.toPx()
    val inside = 4.dp.toPx()
    val color = marks.copy(alpha = 0.5f)
    val corners = listOf(
        Triple(Offset(0f, 0f), -1f, -1f),
        Triple(Offset(size.width, 0f), 1f, -1f),
        Triple(Offset(0f, size.height), -1f, 1f),
        Triple(Offset(size.width, size.height), 1f, 1f),
    )
    for ((c, dx, dy) in corners) {
        // Each arm runs from 5dp outside the corner to 4dp inside it; the
        // perpendicular offset sits half a stroke outside, matching the CSS.
        drawLine(
            color = color,
            start = Offset(c.x + dx * outside, c.y + dy * stroke / 2f),
            end = Offset(c.x - dx * inside, c.y + dy * stroke / 2f),
            strokeWidth = stroke,
        )
        drawLine(
            color = color,
            start = Offset(c.x + dx * stroke / 2f, c.y + dy * outside),
            end = Offset(c.x + dx * stroke / 2f, c.y - dy * inside),
            strokeWidth = stroke,
        )
    }
}
