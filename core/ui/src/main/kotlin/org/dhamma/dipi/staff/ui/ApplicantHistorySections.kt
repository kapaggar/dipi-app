package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantActivityRow
import org.dhamma.dipi.staff.model.ApplicantClarificationRow
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi

const val HISTORY_COURSES = "courses"
const val HISTORY_ACTIVITY = "activity"
const val HISTORY_CLARIFICATIONS = "clarifications"

@Composable
fun ApplicantHistorySections(
    history: ApplicantDeskHistory,
    onExpand: (String) -> Unit,
    onOpenClarification: (Int) -> Unit = {},
) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryBlock(
            title = "Prior courses",
            key = HISTORY_COURSES,
            loaded = history.courses != null,
            loading = HISTORY_COURSES in history.loading,
            error = history.errors[HISTORY_COURSES],
            onExpand = onExpand,
        ) {
            val rows = history.courses.orEmpty()
            if (rows.isEmpty()) {
                Text("No prior courses on the desk.", color = c.muted, fontSize = 13.sp)
            } else {
                rows.forEach { row ->
                    Text(
                        listOf(row.course, row.type, row.status, row.attended, row.address)
                            .filter { it.isNotBlank() }.joinToString(" · "),
                        color = c.foreground,
                        fontSize = 13.sp,
                    )
                }
            }
        }
        HistoryBlock(
            title = "Activity",
            key = HISTORY_ACTIVITY,
            loaded = history.activity != null,
            loading = HISTORY_ACTIVITY in history.loading,
            error = history.errors[HISTORY_ACTIVITY],
            onExpand = onExpand,
        ) {
            val rows = history.activity.orEmpty()
            if (rows.isEmpty()) {
                Text("No activity on the desk.", color = c.muted, fontSize = 13.sp)
            } else {
                rows.forEach { row: ApplicantActivityRow ->
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text(row.activity, color = c.foreground, fontSize = 13.sp)
                        Text(
                            listOf(row.at, row.user).filter { it.isNotBlank() }.joinToString(" · "),
                            color = c.muted,
                            fontSize = 12.sp,
                            fontFamily = DipiMono,
                        )
                    }
                }
            }
        }
        HistoryBlock(
            title = "Clarifications",
            key = HISTORY_CLARIFICATIONS,
            loaded = history.clarifications != null,
            loading = HISTORY_CLARIFICATIONS in history.loading,
            error = history.errors[HISTORY_CLARIFICATIONS],
            onExpand = onExpand,
        ) {
            val rows = history.clarifications.orEmpty()
            if (rows.isEmpty()) {
                Text("No clarifications on the desk.", color = c.muted, fontSize = 13.sp)
            } else {
                rows.forEach { row: ApplicantClarificationRow ->
                    Column(Modifier.padding(bottom = 6.dp)) {
                        Text(row.message, color = c.foreground, fontSize = 13.sp)
                        Text(row.at, color = c.muted, fontSize = 12.sp, fontFamily = DipiMono)
                        if (row.clarId != null) {
                            Text(
                                "Open PDF",
                                color = c.accent,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { onOpenClarification(row.clarId!!) }
                                    .semantics { contentDescription = "Open clarification PDF" }
                                    .padding(top = 2.dp),
                            )
                        } else {
                            Text(row.fileLabel.ifBlank { "No Upload" }, color = c.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryBlock(
    title: String,
    key: String,
    loaded: Boolean,
    loading: Boolean,
    error: String?,
    onExpand: (String) -> Unit,
    content: @Composable () -> Unit,
) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth()) {
        Text(
            if (loaded) "▾ $title" else "▸ $title",
            fontFamily = DipiCondensed,
            fontSize = 16.sp,
            color = c.accent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpand(key) }
                .semantics { contentDescription = "Expand $title" }
                .padding(vertical = 6.dp),
        )
        when {
            loading -> Text("Loading…", color = c.muted, fontSize = 13.sp)
            error != null -> Text(error, color = c.foreground, fontSize = 13.sp)
            loaded -> content()
        }
    }
}
