package org.dhamma.dipi.staff.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun DaySummaryScreen(course: Course, rows: List<ApplicantCard>) {
    val c = LocalDipi.current
    val confirmed = rows.filter { it.status.normalize() == "confirmed" }
    val students = confirmed.filter { it.type == ApplicantType.Student }
    val servers = confirmed.filter { it.type == ApplicantType.Sevak }
    val expected = students.size + servers.size
    val arrived = rows.count { it.attended }
    val pct = if (expected == 0) 0 else (arrived * 100 / expected)
    fun bar(g: Gender): String {
        val list = confirmed.filter { it.gender == g }
        val old = list.count { it.oldStudent }
        val neu = list.size - old
        val srv = servers.count { it.gender == g }
        val tot = list.size
        val op = if (tot == 0) 0 else old * 100 / tot
        val np = if (tot == 0) 0 else neu * 100 / tot
        return "${g.name}  $tot  +$srv server   old $old ($op%)  new $neu ($np%)"
    }
    Column(Modifier.background(c.background).padding(20.dp)) {
        Text("Day 0 · ${formatDay(course.start)}", fontFamily = DipiCondensed, fontSize = 22.sp)
        Text("Expected today", color = c.muted, modifier = Modifier.padding(top = 16.dp))
        Text("$expected = ${students.size} + ${servers.size}", fontFamily = DipiCondensed, fontSize = 36.sp)
        Text("Arrived  $arrived of $expected · $pct%", modifier = Modifier.padding(top = 12.dp))
        Text("Registration desk opens 14:00 · no one marked attended yet", color = c.muted, fontSize = 13.sp)
        Text("Confirmed · old / new", fontFamily = DipiCondensed, modifier = Modifier.padding(top = 16.dp))
        Text(bar(Gender.M), fontSize = 13.sp)
        Text(bar(Gender.F), fontSize = 13.sp)
        Text("Seating requests · not assigned", fontFamily = DipiCondensed, modifier = Modifier.padding(top = 16.dp))
        Text("Chowky / Chair / Backrest  ·  M —   F —", color = c.muted)
    }
}

private fun formatDay(iso: String): String = runCatching {
    val d = java.time.LocalDate.parse(iso)
    "${d.dayOfMonth} ${d.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)} ${d.year}"
}.getOrDefault(iso)
