package org.dhamma.dipi.staff.course

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

data class DeskTileSpec(
    val title: String,
    val route: String,
)

enum class CourseHubLive { Applications, Summary, Photos, Audit, Calling, ZeroDay, CentreOps }

data class CourseHubTile(
    val title: String,
    val route: String,
    val live: CourseHubLive? = null,
)

fun centreDeskTiles(centreId: Int): List<DeskTileSpec> = listOf(
    DeskTileSpec("Centre Settings", "centre/$centreId/edit"),
    DeskTileSpec("Manage Courses", "manage-course/$centreId"),
    DeskTileSpec("Advanced Search", "search-app/$centreId"),
    DeskTileSpec("Daily Activity", "daily-activity/$centreId"),
    DeskTileSpec("SMS Report", "centre/$centreId/sms-report"),
    DeskTileSpec("Course Report", "centre/$centreId/course-report"),
    DeskTileSpec("Bulk Mail", "centre/$centreId/bulk-mail-schedule"),
)

fun courseHubTiles(centreId: Int, courseId: Int): List<CourseHubTile> = listOf(
    CourseHubTile("View Applications", "search-course/$centreId/$courseId?s=&t=&g=&d=a", CourseHubLive.Applications),
    CourseHubTile("Add Application", "app/add/$centreId/$courseId"),
    CourseHubTile("Photo review", "Photo review", CourseHubLive.Photos),
    CourseHubTile("Audit applications", "audit/$centreId/$courseId", CourseHubLive.Audit),
    CourseHubTile("Calling students", "calling/$centreId/$courseId", CourseHubLive.Calling),
    CourseHubTile("Zero Day", "zero-day/$centreId/$courseId", CourseHubLive.ZeroDay),
    CourseHubTile("Day 0 List", "day0-list/$centreId/$courseId"),
    CourseHubTile("Day 0 summary", "Day 0 summary", CourseHubLive.Summary),
    CourseHubTile("Seating Plan", "seating/$centreId/$courseId"),
    CourseHubTile("Student Chit", "student-chit/$centreId/$courseId"),
    CourseHubTile("Checking Slip", "checking-slip/$centreId/$courseId"),
    CourseHubTile("Male PDF", "course-pdf-m/$centreId/$courseId"),
    CourseHubTile("Female PDF", "course-pdf-f/$centreId/$courseId"),
    CourseHubTile("Teachers List", "teacher-list/$centreId/$courseId"),
    CourseHubTile("Laundry List", "laundry-list/$centreId/$courseId"),
    CourseHubTile("Valuable List", "valuable-list/$centreId/$courseId"),
    CourseHubTile("Course Summary Report", "report-day11/$centreId/$courseId"),
    CourseHubTile("Centre Settings", "centre/$centreId/edit", CourseHubLive.CentreOps),
)

@Composable
fun DeskTileGrid(
    titles: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val columns = if (wide) 3 else 2
    val c = LocalDipi.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        titles.chunked(columns).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { (title, onClick) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp)
                            .border(1.dp, c.hairlineStrong, RoundedCornerShape(4.dp))
                            .clickable(onClick = onClick)
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            title,
                            color = c.accent,
                            fontFamily = DipiCondensed,
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
