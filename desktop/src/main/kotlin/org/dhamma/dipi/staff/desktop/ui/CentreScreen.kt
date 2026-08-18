package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.Session

@Composable
fun CentreScreen(
    session: Session,
    courses: List<Course>,
    olderCourses: List<Course>,
    loading: Boolean,
    onPickCourse: (Course) -> Unit,
    onPickCentre: (Centre) -> Unit,
    onSettings: () -> Unit,
) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxSize().background(c.background).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CENTRE", color = c.muted, fontSize = 12.sp, letterSpacing = 1.sp)
                Text(session.centres.firstOrNull()?.name ?: session.displayName, color = c.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(session.displayName, color = c.muted, fontSize = 14.sp)
            }
            DeskButton("Settings", onSettings)
        }
        if (session.centres.size > 1) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                session.centres.forEach { centre ->
                    val on = centre.id == session.centres.first().id
                    Box(
                        Modifier
                            .clip(DeskShape)
                            .background(if (on) c.tint else c.field)
                            .border(1.dp, if (on) c.accent else c.hairline, DeskShape)
                            .clickable { onPickCentre(centre) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(centre.name, color = if (on) c.accent else c.foreground, fontSize = 14.sp)
                    }
                }
            }
        }
        if (loading) {
            Text("Loading courses…", color = c.muted, modifier = Modifier.padding(top = 24.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Upcoming", color = c.muted, fontSize = 12.sp, letterSpacing = 1.sp)
            courses.forEach { course -> CourseRow(course, onPickCourse) }
            if (olderCourses.isNotEmpty()) {
                Text("Older", color = c.muted, fontSize = 12.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 12.dp))
                olderCourses.forEach { course -> CourseRow(course, onPickCourse) }
            }
        }
    }
}

@Composable
private fun CourseRow(course: Course, onPick: (Course) -> Unit) {
    val c = LocalDipi.current
    val summary = course.summary
    Box(
        Modifier
            .fillMaxWidth()
            .clip(DeskShape)
            .background(c.field)
            .border(1.dp, c.hairline, DeskShape)
            .clickable { onPick(course) }
            .padding(16.dp),
    ) {
        Column {
            Text(course.name, color = c.foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            if (summary != null) {
                Text(
                    "Received ${summary.received} · Confirmed ${summary.confirmed} · Expected ${summary.expected} · Total ${summary.total}",
                    color = c.muted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else if (course.start.isNotBlank()) {
                Text("${course.start} – ${course.end}", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
