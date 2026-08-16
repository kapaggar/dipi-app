package org.dhamma.dipi.staff.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.ui.FilterChip
import org.dhamma.dipi.staff.ui.theme.DipiColors
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
    loadPhoto: suspend (ApplicantId) -> ImageBitmap? = { null },
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
            Button(onUpload) { Text("Queue upload ($pendingUploads)") }
        }
        Text(
            "Fixes stay on this device — live upload isn't available on the desk",
            color = c.muted,
            fontSize = 11.sp,
        )
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
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ReviewPhotoFrame(p.id, p.displayName, e, loadPhoto, c)
                    Column {
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
}

private sealed interface ReviewPhoto {
    object Loading : ReviewPhoto
    object Missing : ReviewPhoto
    class Ready(val bitmap: ImageBitmap) : ReviewPhoto
}

/**
 * One review cell at dipi's 260:280 photo ratio. Photos load lazily as the
 * list scrolls — each cell fetches on first composition through the shared
 * concurrency-limited loader. The saved geometry applies on-screen only:
 * rotation via graphicsLayer, crop as a centre-crop preview; the server
 * pixels stay untouched. Loading shows a dimmed hairline frame (no shimmer).
 */
@Composable
private fun ReviewPhotoFrame(
    id: ApplicantId,
    name: String,
    edit: PhotoEdit,
    loadPhoto: suspend (ApplicantId) -> ImageBitmap?,
    c: DipiColors,
) {
    val photo by produceState<ReviewPhoto>(ReviewPhoto.Loading, id) {
        value = ReviewPhoto.Loading
        value = loadPhoto(id)?.let { ReviewPhoto.Ready(it) } ?: ReviewPhoto.Missing
    }
    val loading = photo is ReviewPhoto.Loading
    Box(
        Modifier
            .size(111.dp, 120.dp)
            .border(1.dp, if (loading) c.hairline else c.hairlineStrong)
            .background(if (loading) c.hover else c.field)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        when (val p = photo) {
            is ReviewPhoto.Ready -> Image(
                bitmap = p.bitmap,
                contentDescription = "Photo of $name",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = edit.rotate.toFloat() },
                contentScale = if (edit.cropped) ContentScale.Crop else ContentScale.Fit,
            )
            else -> Text(
                "▣",
                fontSize = 40.sp,
                color = c.muted,
                modifier = Modifier
                    .rotate(edit.rotate.toFloat())
                    .alpha(if (loading) 0.45f else 1f),
            )
        }
    }
}
