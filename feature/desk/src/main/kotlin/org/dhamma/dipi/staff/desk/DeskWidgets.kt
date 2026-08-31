package org.dhamma.dipi.staff.desk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

internal fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

internal fun Modifier.rightHairline(color: Color): Modifier = drawBehind {
    val x = size.width - 0.5.dp.toPx()
    drawLine(color, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
}

internal fun Modifier.leftHairline(color: Color): Modifier = drawBehind {
    val x = 0.5.dp.toPx()
    drawLine(color, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
}

internal fun Modifier.topHairline(color: Color): Modifier = drawBehind {
    val y = 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

/** Segmented control: rounded soft-bordered track on the card fill, accent fill on the selection. */
@Composable
fun DeskSegmented(
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    optionPadding: Dp = 15.dp,
    verticalPadding: Dp = 11.dp,
    counts: Map<String, Int> = emptyMap(),
) {
    Row(
        Modifier.deskCard(
            shape = DeskStyle.controlShape,
            elevation = 0.dp,
        ),
    ) {
        options.forEachIndexed { i, label ->
            val on = label == selected
            Text(
                counts[label]?.let { "$label $it" } ?: label,
                fontSize = 12.5.sp,
                maxLines = 1,
                color = if (on) Color.White else Industry.neutral700,
                modifier = Modifier
                    .then(if (i > 0) Modifier.rightHairlineStart() else Modifier)
                    .background(if (on) Industry.accent else Color.Transparent)
                    .clickable { onPick(label) }
                    .padding(horizontal = optionPadding, vertical = verticalPadding),
            )
        }
    }
}

/**
 * Tablet-scope pair: gender and old/new, independently. FlowRow so the two
 * controls wrap on a narrow Applications column instead of clipping.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeskScopeFilters(
    gender: String,
    seniority: String,
    onGender: (String) -> Unit,
    onSeniority: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DeskSegmented(
            listOf("Both", "Male", "Female"),
            gender,
            onGender,
            optionPadding = 12.dp,
            verticalPadding = 8.dp,
        )
        DeskSegmented(
            listOf("Both", "New", "Old"),
            seniority,
            onSeniority,
            optionPadding = 12.dp,
            verticalPadding = 8.dp,
        )
    }
}

private fun Modifier.rightHairlineStart(): Modifier = drawBehind {
    drawLine(Industry.neutral300, Offset(0.5.dp.toPx(), 0f), Offset(0.5.dp.toPx(), size.height), 1.dp.toPx())
}

/** Toggle: rounded pill track, circular knob; accent when on, neutral-300 when off; .18s knob motion. */
@Composable
fun DeskToggle(
    on: Boolean,
    onToggle: () -> Unit,
    trackWidth: Dp = 40.dp,
    trackHeight: Dp = 20.dp,
    knob: Dp = 16.dp,
) {
    val x by animateDpAsState(
        targetValue = if (on) trackWidth - knob - 2.dp else 2.dp,
        animationSpec = tween(180),
        label = "knob",
    )
    Box(
        Modifier
            .size(trackWidth, trackHeight)
            .clip(DeskStyle.pillShape)
            .background(if (on) Industry.accent else Industry.neutral300)
            .clickable(onClick = onToggle),
    ) {
        Box(
            Modifier
                .offset(x = x, y = (trackHeight - knob) / 2)
                .size(knob)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** Solid accent primary: rounded, gently elevated — the one deliberate accent fill. */
@Composable
fun DeskPrimaryButton(label: String, onClick: () -> Unit, fontSize: Float = 14f) {
    Text(
        label.uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = Color.White,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = Industry.accent,
                border = Industry.accent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

@Composable
fun DeskOutlineButton(label: String, onClick: () -> Unit) {
    Text(
        label.uppercase(),
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.06.em,
        maxLines = 1,
        color = Industry.text,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                border = Industry.neutral400,
                elevation = 0.dp,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

/**
 * Snackbar anchored to the desk: left 24, bottom 20, max width 520.
 * Success on accent-800, error on the muted error tone — rounded, floating.
 */
@Composable
fun DeskSnackbar(text: String, error: Boolean, modifier: Modifier = Modifier) {
    val tone = if (error) Color(0xFF5A2F2F) else Industry.accent800
    Box(
        modifier
            .padding(start = 24.dp, bottom = 20.dp)
            .width(520.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color.White,
            modifier = Modifier
                .deskCard(
                    shape = DeskStyle.controlShape,
                    fill = tone,
                    border = tone,
                    elevation = DeskStyle.dialogElevation,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

/**
 * Indeterminate progress hairline: 2dp accent sweep on a neutral-200 track —
 * the only sanctioned motion besides the toggles. Sits under a header while
 * the application data downloads.
 */
@Composable
fun DeskProgressHairline(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "desk-progress")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "sweep",
    )
    androidx.compose.foundation.Canvas(modifier.fillMaxWidth().height(2.dp)) {
        drawRect(Industry.neutral200)
        val bar = size.width * 0.28f
        val x = (size.width + bar) * sweep - bar
        drawRect(
            Industry.accent,
            topLeft = Offset(x, 0f),
            size = androidx.compose.ui.geometry.Size(bar, size.height),
        )
    }
}

enum class DeskIconKind { Check, Close, ChevronDown, ArrowRight, Download, Phone, WhatsApp }

/** Lucide-style line icons drawn at stroke 1.5 (2.2 for the roster tick). */
@Composable
fun DeskIcon(
    kind: DeskIconKind,
    size: Dp,
    color: Color,
    strokeWidth: Float = 1.5f,
) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val s = this.size.width / 24f
        // SVG semantics: the 24-unit viewBox scales, and the stroke scales with it.
        val stroke = Stroke(
            width = strokeWidth * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        fun path(vararg pts: Pair<Float, Float>): Path = Path().apply {
            moveTo(pts[0].first * s, pts[0].second * s)
            for (p in pts.drop(1)) lineTo(p.first * s, p.second * s)
        }
        when (kind) {
            DeskIconKind.Check -> drawPath(path(4f to 12.5f, 9.5f to 18f, 20f to 6.5f), color, style = stroke)
            DeskIconKind.Close -> {
                drawPath(path(6f to 6f, 18f to 18f), color, style = stroke)
                drawPath(path(18f to 6f, 6f to 18f), color, style = stroke)
            }
            DeskIconKind.ChevronDown -> drawPath(path(6f to 9f, 12f to 15f, 18f to 9f), color, style = stroke)
            DeskIconKind.ArrowRight -> {
                drawPath(path(4f to 12f, 20f to 12f), color, style = stroke)
                drawPath(path(13f to 5f, 20f to 12f, 13f to 19f), color, style = stroke)
            }
            DeskIconKind.Download -> {
                drawPath(path(12f to 3f, 12f to 14f), color, style = stroke)
                drawPath(path(7f to 9.5f, 12f to 14.5f, 17f to 9.5f), color, style = stroke)
                drawPath(path(4f to 17f, 4f to 21f, 20f to 21f, 20f to 17f), color, style = stroke)
            }
            DeskIconKind.Phone -> drawPhone(color, stroke, s)
            DeskIconKind.WhatsApp -> drawChatBubble(color, stroke, s)
        }
    }
}

/** Message-circle outline — the WhatsApp hand-off glyph, drawn in the same Lucide line style. */
private fun DrawScope.drawChatBubble(color: Color, stroke: Stroke, s: Float) {
    val p = Path().apply {
        // Circle-ish bubble with a tail pulled to the bottom-left corner.
        moveTo(4f * s, 21f * s)
        lineTo(5.6f * s, 16.2f * s)
        cubicTo(4.6f * s, 14.8f * s, 4f * s, 13.2f * s, 4f * s, 11.5f * s)
        cubicTo(4f * s, 6.8f * s, 7.6f * s, 3f * s, 12.2f * s, 3f * s)
        cubicTo(16.8f * s, 3f * s, 20.5f * s, 6.8f * s, 20.5f * s, 11.5f * s)
        cubicTo(20.5f * s, 16.2f * s, 16.8f * s, 20f * s, 12.2f * s, 20f * s)
        cubicTo(10.9f * s, 20f * s, 9.7f * s, 19.7f * s, 8.6f * s, 19.2f * s)
        close()
    }
    drawPath(p, color, style = stroke)
}

private fun DrawScope.drawPhone(color: Color, stroke: Stroke, s: Float) {
    val p = Path().apply {
        moveTo(7.5f * s, 3f * s)
        lineTo(4.5f * s, 3f * s)
        quadraticBezierTo(3f * s, 3f * s, 3f * s, 4.5f * s)
        quadraticBezierTo(3f * s, 13f * s, 8f * s, 17.5f * s)
        quadraticBezierTo(12f * s, 21f * s, 19.5f * s, 21f * s)
        quadraticBezierTo(21f * s, 21f * s, 21f * s, 19.5f * s)
        lineTo(21f * s, 16.5f * s)
        lineTo(16.5f * s, 14.5f * s)
        lineTo(14.5f * s, 16.5f * s)
        quadraticBezierTo(10f * s, 14.5f * s, 9f * s, 10f * s)
        lineTo(11f * s, 8f * s)
        close()
    }
    drawPath(p, color, style = stroke)
}

/** Section body copy — 12.5sp neutral-600, used by most pane subtitles. */
@Composable
fun DeskSub(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.5.sp, lineHeight = 17.sp, color = Industry.neutral600, modifier = modifier)
}

/** Pane h2 — Barlow Condensed 700 / 30. */
@Composable
fun DeskH2(text: String) {
    Text(
        text,
        fontFamily = DipiCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        color = Industry.text,
    )
}

/** Centred empty-state line. */
@Composable
fun DeskEmpty(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 13.sp,
        color = Industry.neutral600,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}
