package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
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
    content: @Composable () -> Unit = { CourseOpsPlaceholder() },
) {
    val c = LocalDipi.current
    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .testTag("course-ops-host"),
    ) {
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
