package org.dhamma.dipi.staff.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.ui.FilterChip
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun PhotoReviewScreen(
    people: List<ApplicantCard>,
    suggestions: List<PhotoReviewItem>,
    edits: Map<ApplicantId, PhotoEdit>,
    filter: String,
    onFilter: (String) -> Unit,
    onRotate: (ApplicantId, Int) -> Unit,
    onCrop: (ApplicantId) -> Unit,
    onDone: (ApplicantId) -> Unit,
    onUpload: () -> Unit,
    pendingUploads: Int,
) {
    val c = LocalDipi.current
    val sug = suggestions.associateBy { it.applicantId }
    val filters = listOf("All", "Suggested", "Auto-fixed", "Fixed", "Unreviewed")
    val shown = people.filter { p ->
        val e = edits[p.id]
        val s = sug[p.id]
        when (filter) {
            "Suggested" -> s?.kind == "suggest"
            "Auto-fixed" -> s?.kind == "auto"
            "Fixed" -> e?.done == true
            "Unreviewed" -> s == null && e?.done != true
            else -> true
        }
    }
    Column(Modifier.fillMaxSize().background(c.background).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Photo review", fontFamily = DipiCondensed, fontSize = 22.sp)
            Button(onUpload) { Text("⬆ dipi ($pendingUploads)") }
        }
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { f ->
                val n = when (f) {
                    "All" -> people.size
                    "Suggested" -> suggestions.count { it.kind == "suggest" }
                    "Auto-fixed" -> suggestions.count { it.kind == "auto" }
                    "Fixed" -> edits.values.count { it.done }
                    else -> people.count { sug[it.id] == null && edits[it.id]?.done != true }
                }
                FilterChip("$f $n", filter == f) { onFilter(f) }
            }
        }
        LazyColumn {
            items(shown, key = { it.id.value }) { p ->
                val e = edits[p.id] ?: PhotoEdit(sug[p.id]?.suggestedRotate ?: 0, false, false)
                val badge = if (e.done) "✓ fixed" else sug[p.id]?.badge ?: "✓ good"
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text("▣", fontSize = 40.sp, modifier = Modifier.rotate(e.rotate.toFloat()), color = c.muted)
                    Text(p.displayName, fontFamily = DipiCondensed)
                    Text("${p.confNo?.display() ?: "—"}  $badge", color = c.muted, fontSize = 12.sp)
                    Row {
                        TextButton({ onRotate(p.id, -90) }) { Text("↺") }
                        TextButton({ onRotate(p.id, 90) }) { Text("↻") }
                        TextButton({ onCrop(p.id) }) { Text("✂") }
                        TextButton({ onDone(p.id) }) { Text("✓") }
                    }
                }
            }
        }
    }
}
