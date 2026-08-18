package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ManagedCourse
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

@Composable
fun ManageCoursesScreen(
    rows: List<ManagedCourse>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Manage Courses", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Read-only list from the desk. Finalized courses cannot be edited here.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }
        when {
            loading -> Text("Loading…", color = c.muted)
            error != null -> Text(error, color = c.foreground)
            rows.isEmpty() -> Text("No courses on the desk.", color = c.muted)
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .deskCard(fill = c.field, border = c.hairline)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                    ) {
                        Text(
                            row.type.ifBlank { "Course ${row.id}" },
                            fontFamily = DipiCondensed,
                            fontSize = 17.sp,
                            color = c.foreground,
                        )
                        val dates = listOf(row.start, row.end).filter { it.isNotBlank() }
                        if (dates.isNotEmpty()) {
                            Text(dates.joinToString(" – "), color = c.muted, fontSize = 13.sp)
                        }
                        Text(
                            "M ${row.statusNm.ifBlank { "—" }}/${row.statusOm.ifBlank { "—" }} · " +
                                "F ${row.statusNf.ifBlank { "—" }}/${row.statusOf.ifBlank { "—" }} · " +
                                "Sevak ${row.statusSvrM.ifBlank { "—" }}/${row.statusSvrF.ifBlank { "—" }}",
                            fontFamily = DipiMono,
                            fontSize = 12.sp,
                            color = c.muted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            if (row.finalized) "Finalized" else (row.status.ifBlank { "Open" }),
                            color = if (row.finalized) c.accent else c.foreground,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
