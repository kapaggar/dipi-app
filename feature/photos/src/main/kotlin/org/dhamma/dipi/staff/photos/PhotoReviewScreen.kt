package org.dhamma.dipi.staff.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.FilterChip
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun PhotoReviewScreen(
    state: PhotoReviewUiState,
    onAction: (PhotoReviewAction) -> Unit,
    loadPreview: suspend (ApplicantId) -> ImageBitmap? = { null },
    loadOriginal: suspend (Int) -> ImageBitmap? = { null },
    loadCorrected: suspend (Int) -> ImageBitmap? = { null },
) {
    val c = LocalDipi.current
    val editor = state.editor
    if (editor != null) {
        Column(Modifier.fillMaxSize().background(c.background)) {
            Text(
                "Ready ${state.readyCount}",
                fontFamily = DipiCondensed,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            PhotoEditor(editor, onAction, loadOriginal, loadCorrected, Modifier.weight(1f))
        }
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Photo review", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Update on desk posts the current application form with the corrected photo. " +
                "The desk save can still change other fields if it transforms what was echoed.",
            color = c.muted,
            fontSize = 11.sp,
        )
        state.notice?.let { notice ->
            Text(notice, color = c.accent, fontSize = 13.sp, modifier = Modifier.clickable {
                onAction(PhotoReviewAction.DismissNotice)
            })
        }
        ActionRow(state, onAction)
        FilterRow(state, onAction)
        SearchRow(state, onAction)
        state.scan?.let { scan ->
            Text("Scanning ${scan.completed} / ${scan.total}", color = c.muted, fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { if (scan.total == 0) 0f else scan.completed.toFloat() / scan.total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.confirmUpdate) {
            Text(
                "This updates the application on the desk. The app echoes every field from " +
                    "the current edit form and attaches the corrected photo.",
                color = c.foreground,
                fontSize = 13.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAction(PhotoReviewAction.StartUpdate) },
                    modifier = Modifier.height(48.dp),
                ) { Text("Update on desk") }
                TextButton(
                    onClick = { onAction(PhotoReviewAction.CancelUpdate) },
                    modifier = Modifier.height(48.dp),
                ) { Text("Cancel") }
            }
        }
        state.updateProgress?.let { (done, total) ->
            Text("Updating $done / $total", color = c.muted, fontSize = 12.sp)
            LinearProgressIndicator(
                progress = { if (total == 0) 0f else done.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        state.snack?.let { Text(it, color = c.accent, fontSize = 13.sp) }
        if (state.exported) Text("Saved locally", color = c.accent, fontSize = 13.sp)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val min = 156.dp
            val columns = maxOf(1, (maxWidth / min).toInt())
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.cards, key = { it.applicant.id.value }) { card ->
                    PhotoCard(card, state.selectedApplicantId, onAction, loadPreview)
                }
            }
        }
    }
}

@Composable
private fun ActionRow(state: PhotoReviewUiState, onAction: (PhotoReviewAction) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.scan != null) {
            Button(
                onClick = { onAction(PhotoReviewAction.CancelScan) },
                modifier = Modifier.height(48.dp),
            ) { Text("Cancel") }
        } else {
            Button(
                onClick = { onAction(PhotoReviewAction.Scan) },
                modifier = Modifier.height(48.dp),
            ) { Text("Scan photos") }
        }
        OutlinedButton(
            onClick = { onAction(PhotoReviewAction.ApproveSelected) },
            modifier = Modifier.height(48.dp),
        ) { Text("Review fixes") }
        Button(
            onClick = { onAction(PhotoReviewAction.PrepareExport) },
            enabled = state.readyCount > 0 && state.scan == null && !state.updating,
            modifier = Modifier.height(48.dp),
        ) { Text("Export (${state.readyCount})") }
        if (state.allowDeskWrite) {
            if (state.updating) {
                OutlinedButton(
                    onClick = { onAction(PhotoReviewAction.StopAfterCurrent) },
                    modifier = Modifier.height(48.dp),
                ) { Text("Stop after current") }
            } else {
                Button(
                    onClick = { onAction(PhotoReviewAction.PrepareUpdate) },
                    enabled = state.readyCount > 0 && state.scan == null,
                    modifier = Modifier.height(48.dp),
                ) { Text("Update on desk (${state.readyCount})") }
            }
        }
        if (state.safeSuggestionCount > 0) {
            OutlinedButton(
                onClick = { onAction(PhotoReviewAction.ApplySafeSuggestions) },
                modifier = Modifier.height(48.dp),
            ) { Text("Apply safe suggestions") }
        }
        TextButton(
            onClick = { onAction(PhotoReviewAction.ClearCourse) },
            modifier = Modifier.height(48.dp),
        ) { Text("Clear this course's edits") }
        Text("Ready ${state.readyCount}", fontFamily = DipiCondensed, modifier = Modifier.align(Alignment.CenterVertically))
    }
}

@Composable
private fun FilterRow(state: PhotoReviewUiState, onAction: (PhotoReviewAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("All", state.reviewFilter == PhotoReviewFilter.ALL) {
                onAction(PhotoReviewAction.SetReviewFilter(PhotoReviewFilter.ALL))
            }
            FilterChip("Needs review", state.reviewFilter == PhotoReviewFilter.NEEDS_REVIEW) {
                onAction(PhotoReviewAction.SetReviewFilter(PhotoReviewFilter.NEEDS_REVIEW))
            }
            FilterChip("Ready", state.reviewFilter == PhotoReviewFilter.READY) {
                onAction(PhotoReviewAction.SetReviewFilter(PhotoReviewFilter.READY))
            }
            FilterChip("Updated", state.reviewFilter == PhotoReviewFilter.UPDATED) {
                onAction(PhotoReviewAction.SetReviewFilter(PhotoReviewFilter.UPDATED))
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("All genders", state.gender == null) {
                onAction(PhotoReviewAction.SetGender(null))
            }
            FilterChip("Male", state.gender == Gender.M) {
                onAction(PhotoReviewAction.SetGender(Gender.M))
            }
            FilterChip("Female", state.gender == Gender.F) {
                onAction(PhotoReviewAction.SetGender(Gender.F))
            }
            FilterChip("All old/new", state.seniority == null) {
                onAction(PhotoReviewAction.SetSeniority(null))
            }
            FilterChip("New", state.seniority == ConfSeniority.NEW) {
                onAction(PhotoReviewAction.SetSeniority(ConfSeniority.NEW))
            }
            FilterChip("Old", state.seniority == ConfSeniority.OLD) {
                onAction(PhotoReviewAction.SetSeniority(ConfSeniority.OLD))
            }
        }
    }
}

@Composable
private fun SearchRow(state: PhotoReviewUiState, onAction: (PhotoReviewAction) -> Unit) {
    val c = LocalDipi.current
    BasicTextField(
        value = state.search,
        onValueChange = { onAction(PhotoReviewAction.SetSearch(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, c.hairlineStrong)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .semantics { contentDescription = "Search photos" },
        singleLine = true,
        decorationBox = { inner ->
            if (state.search.isEmpty()) Text("Search name or confirmation", color = c.muted)
            inner()
        },
    )
}

@Composable
private fun PhotoCard(
    card: PhotoCardUi,
    selectedId: Int?,
    onAction: (PhotoReviewAction) -> Unit,
    loadPreview: suspend (ApplicantId) -> ImageBitmap?,
) {
    val c = LocalDipi.current
    val person = card.applicant
    val photo by produceState<ImageBitmap?>(null, person.id) {
        value = loadPreview(person.id)
    }
    val selected = selectedId == person.id.value
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, if (selected) c.accent else c.hairline)
            .background(c.field)
            .clickable { onAction(PhotoReviewAction.SelectApplicant(person.id.value)) }
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(168.dp)
                .border(1.dp, c.hairlineStrong)
                .background(c.hover)
                .clickable { onAction(PhotoReviewAction.SelectApplicant(person.id.value)) },
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo!!,
                    contentDescription = "Photo of ${person.displayName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Text("▣", fontSize = 36.sp, color = c.muted)
            }
        }
        Text(person.displayName, fontFamily = DipiCondensed, color = c.foreground)
        Text(
            "${person.confNo?.display() ?: "-"}  ${person.status.value}",
            color = c.muted,
            fontSize = 12.sp,
        )
        Text(card.label, color = if (card.ready) c.accent else c.muted, fontSize = 12.sp)
        TextButton(
            onClick = { onAction(PhotoReviewAction.SelectApplicant(person.id.value)) },
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 48.dp)
                .semantics { contentDescription = "Edit ${person.displayName}" },
        ) { Text("Edit") }
    }
}
