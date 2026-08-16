package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

/**
 * The pre-course call round. Log the outcome as you go — the pile empties
 * itself, and because the log persists on this device the round survives a
 * restart and can be picked up by someone else after lunch.
 */
@Composable
fun CallingPane(
    roll: List<ApplicantCard>,
    outcomes: Map<ApplicantId, CallRecord>,
    filter: String,
    onFilter: (String) -> Unit,
    onOutcome: (ApplicantCard, String) -> Unit,
    onDial: (ApplicantCard) -> Unit,
    onWhatsApp: (ApplicantCard) -> Unit,
    onNote: (ApplicantCard, String) -> Unit,
) {
    val callList = deskCallList(roll)
    val logged = callList.count { outcomes[it.id]?.logged == true }
    val shown = deskCallRows(roll, outcomes, filter)
    val nowMs = System.currentTimeMillis()

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                DeskH2("Call round")
                DeskSub("$logged of ${callList.size} logged · log the outcome as you go, the list empties itself")
            }
            DeskSegmented(
                listOf("To call") + CALL_OUTCOMES,
                filter,
                onFilter,
                optionPadding = 14.dp,
                verticalPadding = 9.dp,
                counts = deskCallCounts(roll, outcomes),
            )
        }

        if (shown.isEmpty()) {
            DeskEmpty(
                "Nothing in this pile.",
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 46.dp),
            )
        } else {
            LazyColumn {
                items(shown, key = { it.id.value }) { card ->
                    CallRow(
                        card = card,
                        record = outcomes[card.id],
                        nowMs = nowMs,
                        onOutcome = { onOutcome(card, it) },
                        onDial = { onDial(card) },
                        onWhatsApp = { onWhatsApp(card) },
                        onNote = { onNote(card, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CallRow(
    card: ApplicantCard,
    record: CallRecord?,
    nowMs: Long,
    onOutcome: (String) -> Unit,
    onDial: () -> Unit,
    onWhatsApp: () -> Unit,
    onNote: (String) -> Unit,
) {
    var noteOpen by rememberSaveable(card.id.value) { mutableStateOf(false) }
    val hasNote = !record?.note.isNullOrBlank()

    Column(
        Modifier
            .fillMaxWidth()
            .bottomHairline(Industry.neutral200)
            .padding(horizontal = 26.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                card.confNo?.display() ?: "—",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                color = Industry.neutral600,
                modifier = Modifier.width(56.dp),
            )
            Text(
                card.displayName,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Industry.text,
                modifier = Modifier.width(170.dp),
            )
            Text(
                card.city.orEmpty(),
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Industry.neutral600,
                modifier = Modifier.width(92.dp),
            )
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.clickable(onClick = onDial),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DeskIcon(DeskIconKind.Phone, 15.dp, Industry.accent)
                    Text(
                        card.mobile.orEmpty(),
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        color = Industry.accent800,
                    )
                }
                Row(
                    Modifier
                        .clickable(onClick = onWhatsApp)
                        .semantics { contentDescription = "WhatsApp ${card.displayName}" }
                        .padding(2.dp),
                ) {
                    DeskIcon(DeskIconKind.WhatsApp, 15.dp, Industry.accent)
                }
                // The tracker's attempts counter + time since last attempt.
                val meta = deskCallMeta(record, nowMs)
                if (meta != null) {
                    Text(
                        meta,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1,
                        color = Industry.neutral500,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CALL_OUTCOMES.forEach { label ->
                    val on = record?.outcome == label
                    Text(
                        label,
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = if (on) Industry.accent800 else Industry.neutral600,
                        modifier = Modifier
                            .clip(DeskStyle.controlShape)
                            .background(if (on) Industry.accent100 else Color.Transparent)
                            .border(
                                1.dp,
                                if (on) Industry.accent else Industry.neutral300,
                                DeskStyle.controlShape,
                            )
                            .clickable { onOutcome(label) }
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
                Text(
                    "Note",
                    fontSize = 12.sp,
                    maxLines = 1,
                    color = if (hasNote) Industry.accent800 else Industry.neutral600,
                    modifier = Modifier
                        .clip(DeskStyle.controlShape)
                        .background(if (hasNote) Industry.accent100 else Color.Transparent)
                        .border(
                            1.dp,
                            if (hasNote) Industry.accent else Industry.neutral300,
                            DeskStyle.controlShape,
                        )
                        .clickable { noteOpen = !noteOpen }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        if (noteOpen) {
            BasicTextField(
                value = record?.note.orEmpty(),
                onValueChange = { onNote(it.take(200)) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.5.sp, color = Industry.text),
                cursorBrush = SolidColor(Industry.accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .border(1.dp, Industry.neutral300, DeskStyle.controlShape)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        if (record?.note.isNullOrEmpty()) {
                            Text(
                                "Add a short note — stays on this device",
                                fontSize = 12.5.sp,
                                color = Industry.neutral500,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 70.dp)
                    .semantics { contentDescription = "Note for ${card.displayName}" },
            )
        }
    }
}
