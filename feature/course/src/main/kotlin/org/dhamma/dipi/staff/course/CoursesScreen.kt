package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CoursesScreen(session: Session, courses: List<Course>, onPick: (Course) -> Unit) {
    val c = LocalDipi.current
    val centre = session.centres.firstOrNull()
    Column(Modifier.fillMaxSize().background(c.background).padding(20.dp)) {
        Text(
            "${centre?.name ?: "Centre"} · from your account · ${session.displayName}",
            fontFamily = DipiCondensed,
            fontSize = 22.sp,
            color = c.foreground,
        )
        Text("Upcoming courses", color = c.muted, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        LazyColumn {
            itemsIndexed(courses, key = { _, it -> it.id.value }) { index, course ->
                val days = runCatching {
                    ChronoUnit.DAYS.between(LocalDate.parse("2026-08-13"), LocalDate.parse(course.start))
                }.getOrDefault(0)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(course) }
                        .padding(vertical = 12.dp),
                ) {
                    Text(course.name, fontFamily = DipiCondensed, fontSize = 18.sp, color = c.foreground)
                    Text("${course.start} – ${course.end}", color = c.muted, fontSize = 13.sp)
                    if (index == 0) {
                        Text("STARTS IN $days DAYS", color = c.accent, fontFamily = DipiCondensed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
