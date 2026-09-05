package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.teacher.CourseOpsOfflineStrip
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.LocalDipi

/**
 * The course-ops surface (spec 2a S5) — everything the assistant teacher
 * sees while the mode is on. Header band 62dp: "Teacher list", the locked
 * course line (or the empty state — no picker, ever), and the 48dp ⚙
 * affordance that opens Settings only through the device-PIN prompt.
 *
 * The [content] slot is the wave-2 seam: the teacher roll replaces the
 * placeholder body in the next slice; this frame does not change.
 *
 * No desk rail, no queued strip, no desk destination composes here — the
 * caller guarantees it (spec 2a hard rule).
 */
@Composable
fun CourseOpsHost(
    course: Course?,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    cachedAt: String? = null,
    content: @Composable () -> Unit = { CourseOpsPlaceholder() },
) {
    val c = LocalDipi.current
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("course-ops-host"),
    ) {
        if (offline) CourseOpsOfflineStrip(cachedAt)
        Row(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Teacher list",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 23.sp,
                    letterSpacing = 0.2.sp,
                    color = c.foreground,
                )
                Text(
                    course?.name ?: "No course is running today",
                    fontSize = 13.sp,
                    color = c.muted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // 48dp target; Settings opens only after the PIN prompt (spec S3).
            Box(
                Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onSettings)
                    .testTag("course-ops-settings"),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙", fontSize = 22.sp, color = c.muted)
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

/** Wave-2 seam: the roll lands in the [CourseOpsHost] content slot next. */
@Composable
private fun CourseOpsPlaceholder() {
    val c = LocalDipi.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Roll arrives in the next slice.",
            fontSize = 14.sp,
            color = c.muted,
            modifier = Modifier.testTag("course-ops-placeholder"),
        )
    }
}

/** Roll fetch failed — the server's words, verbatim, and nothing else. */
@Composable
fun CourseOpsRollError(message: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
            .testTag("course-ops-roll-error"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Industry.neutral200, RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFFDEDEE1), RoundedCornerShape(6.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("◍ No cached roll on this tablet for this course", fontSize = 13.5.sp, color = Industry.neutral800)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFFBEFEE), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFFE8CDC9), RoundedCornerShape(8.dp))
                .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp),
        ) {
            Text(
                "THE DESK REFUSED THE ROLL",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 1.7.sp,
                color = Color(0xFFA33A34),
            )
            Text(
                message,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = Industry.text,
                modifier = Modifier
                    .padding(top = 11.dp)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFE8CDC9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .testTag("course-ops-roll-error-body"),
            )
            Text(
                "Printed exactly as it arrived — no rewording, no friendly summary.",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = Color(0xFF7A5450),
                modifier = Modifier.padding(top = 11.dp),
            )
        }
    }
}

/**
 * Between entry and the roll landing. With no running course the body is a
 * dashed empty-host (v6 C9), not a failure — the dates simply do not match.
 */
@Composable
fun CourseOpsRollPending(hasCourse: Boolean) {
    if (!hasCourse) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp)
                .drawBehind {
                    drawRoundRect(
                        color = Color(0xFFD4D4D7),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                }
                .background(Color(0xFFFAFAFB), RoundedCornerShape(8.dp))
                .testTag("course-ops-empty-host"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 52.dp),
            ) {
                Text(
                    "COURSE OPS IS ON",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 1.7.sp,
                    color = Industry.neutral500,
                )
                Text(
                    "No course is running today",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    letterSpacing = 0.3.sp,
                    color = Industry.text,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "The roll follows the course whose dates contain today, and the desk has no such course for this centre. " +
                        "This is what the tablet looks like between courses — nothing is broken and nothing needs fetching.",
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = Industry.neutral600,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "Fetching the roll…",
            fontSize = 14.sp,
            color = Industry.neutral500,
            modifier = Modifier.testTag("course-ops-roll-pending"),
        )
    }
}
