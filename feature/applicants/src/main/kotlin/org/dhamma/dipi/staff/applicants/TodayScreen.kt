package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.ui.ApplicantRow
import org.dhamma.dipi.staff.ui.FilterChip
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    course: Course,
    centreName: String,
    query: String,
    onQuery: (String) -> Unit,
    counts: Map<String, Int>,
    selected: Set<String>,
    onToggleStatus: (String) -> Unit,
    rows: List<ApplicantCard>,
    queued: Map<ApplicantId, String>,
    loading: Boolean,
    dark: Boolean,
    onOpen: (ApplicantCard) -> Unit,
    onSummary: () -> Unit,
    onPhotos: () -> Unit,
    onSettings: () -> Unit,
    onRefresh: () -> Unit = {},
) {
    val c = LocalDipi.current
    val chipOrder = listOf("All", "Pending", "Received", "Confirmed", "Expected", "Cancelled", "Rejected")
    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(centreName, color = c.muted, fontSize = 12.sp)
                Text(course.name, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
                Text("${course.start} – ${course.end}", color = c.muted, fontSize = 12.sp)
            }
            Row {
                IconButton(onSummary) { Text("▤") }
                IconButton(onPhotos) { Text("◎") }
                IconButton(onSettings) { Text("⚙") }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("⌕ Name, conf no, phone…") },
            singleLine = true,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            chipOrder.forEach { key ->
                val n = counts[key] ?: counts.entries.firstOrNull { it.key.equals(key, true) }?.value
                val label = if (n != null) "$key $n" else key
                val sel = if (key == "All") selected.isEmpty() else selected.any { it.equals(key, true) }
                FilterChip(label, sel) { onToggleStatus(key) }
            }
        }
        PullToRefreshBox(isRefreshing = loading, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
            if (loading && rows.isEmpty()) {
                TodaySkeleton()
            } else if (rows.isEmpty()) {
                Text("No applicants match those filters.", modifier = Modifier.padding(24.dp), color = c.muted)
            } else {
                LazyColumn {
                    items(rows, key = { it.id.value }) { row ->
                        ApplicantRow(row, queued[row.id], dark) { onOpen(row) }
                    }
                }
            }
        }
    }
}
