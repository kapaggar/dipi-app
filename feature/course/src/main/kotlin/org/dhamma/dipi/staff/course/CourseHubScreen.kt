package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun CourseHubScreen(
    course: Course,
    centreName: String,
    onBack: () -> Unit,
    onSettings: () -> Unit = {},
    onApplications: () -> Unit,
    onSummary: () -> Unit,
    onPhotos: () -> Unit,
    onAudit: () -> Unit = {},
    onCalling: () -> Unit = {},
    onZeroDay: () -> Unit = {},
    onCentreOps: () -> Unit = {},
    onLater: (String, String) -> Unit,
) {
    val c = LocalDipi.current
    val cid = course.centreId.value
    val id = course.id.value
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(centreName, color = c.muted, fontSize = 12.sp)
        Text(course.name, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        if (course.start.isNotBlank() || course.end.isNotBlank()) {
            Text(
                listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" - "),
                color = c.muted,
                fontSize = 12.sp,
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Centre") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        Text("Course desk", color = c.muted, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        DeskTileGrid(
            titles = courseHubTiles(cid, id).map { tile ->
                val click: () -> Unit = {
                    when (tile.live) {
                        CourseHubLive.Applications -> onApplications()
                        CourseHubLive.Summary -> onSummary()
                        CourseHubLive.Photos -> onPhotos()
                        CourseHubLive.Audit -> onAudit()
                        CourseHubLive.Calling -> onCalling()
                        CourseHubLive.ZeroDay -> onZeroDay()
                        CourseHubLive.CentreOps -> onCentreOps()
                        null -> onLater(tile.title, tile.route)
                    }
                }
                tile.title to click
            },
        )
    }
}
