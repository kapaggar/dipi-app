package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard
import org.dhamma.dipi.staff.ui.theme.statusColors

/**
 * The pre-course call round, shaped like the call tracker the office already
 * runs in the browser: one card per applicant, closed to a scannable line and
 * opened — one at a time — onto everything a call needs. Log the outcome as
 * you go and the pile empties itself; because the log persists on this device
 * the round survives a restart and can be picked up by someone else after
 * lunch.
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
    gender: String = "Both",
    seniority: String = "Both",
    onGender: (String) -> Unit = {},
    onSeniority: (String) -> Unit = {},
    search: String = "",
    onSearch: (String) -> Unit = {},
    priority: Boolean = false,
    onPriority: () -> Unit = {},
    statusChoices: List<String> = emptyList(),
    onChangeStatus: (ApplicantCard, String) -> Unit = { _, _ -> },
    onWhatsAppBatch: ((List<ApplicantCard>) -> Unit)? = null,
) {
    val scoped = deskScoped(roll, deskGenderScope(gender), deskSeniorityScope(seniority))
    val callList = deskCallList(scoped)
    val logged = callList.count { deskCallLogged(outcomes[it.id]) }
    val shown = deskCallSorted(deskCallRows(scoped, outcomes, filter, search), outcomes, priority)
    val nowMs = System.currentTimeMillis()
    // One card open at a time — the tracker's expandedId. -1 is "none open".
    var openId by rememberSaveable { mutableStateOf(-1) }

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
                optionPadding = 12.dp,
                verticalPadding = 9.dp,
                counts = deskCallCounts(scoped, outcomes),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DeskScopeFilters(gender, seniority, onGender, onSeniority, Modifier.weight(1f))
                SortPill(priority, onPriority)
            }
            CallSearchField(search, onSearch)
            onWhatsAppBatch?.let { open ->
                androidx.compose.material3.OutlinedButton(onClick = { open(shown) }) {
                    Text("WhatsApp batch · ${shown.size} in this view")
                }
            }
        }

        if (shown.isEmpty()) {
            DeskEmpty(
                if (search.isBlank()) "Nothing in this pile." else "No one in this pile matches “${search.trim()}”.",
                Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 46.dp),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(shown, key = { _, card -> card.id.value }) { i, card ->
                    CallCard(
                        card = card,
                        record = outcomes[card.id],
                        index = i + 1,
                        nowMs = nowMs,
                        expanded = openId == card.id.value,
                        onToggle = { openId = if (openId == card.id.value) -1 else card.id.value },
                        onOutcome = { onOutcome(card, it) },
                        onDial = { onDial(card) },
                        onWhatsApp = { onWhatsApp(card) },
                        onNote = { onNote(card, it) },
                        statusChoices = statusChoices,
                        onChangeStatus = { onChangeStatus(card, it) },
                    )
                }
            }
        }
    }
}

/** Order pill: A–Z, or still-to-reach floated to the top. */
@Composable
private fun SortPill(priority: Boolean, onPriority: () -> Unit) {
    Text(
        if (priority) "Priority order" else "A–Z",
        fontSize = 12.5.sp,
        maxLines = 1,
        color = if (priority) Color.White else Industry.neutral700,
        modifier = Modifier
            .deskCard(
                shape = DeskStyle.controlShape,
                fill = if (priority) Industry.accent else DeskStyle.cardFill,
                border = if (priority) Industry.accent else DeskStyle.cardBorder,
                elevation = 0.dp,
            )
            .clickable(onClick = onPriority)
            .semantics { contentDescription = "Toggle call list order" }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** Name box — the tracker's search field, matched to the roster's scan field. */
@Composable
private fun CallSearchField(search: String, onSearch: (String) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(DeskStyle.cardFill, DeskStyle.controlShape)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Industry.accent else Industry.neutral400,
                DeskStyle.controlShape,
            )
            .clip(DeskStyle.controlShape)
            .padding(start = 14.dp, end = if (search.isEmpty()) 14.dp else 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = search,
            onValueChange = onSearch,
            singleLine = true,
            textStyle = TextStyle(fontSize = 14.sp, color = Industry.text),
            cursorBrush = SolidColor(Industry.accent),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = "Search the call list by name" },
            decorationBox = { inner ->
                if (search.isEmpty()) {
                    Text(
                        "Search by name or conf number",
                        fontFamily = DipiSans,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Industry.neutral500,
                    )
                }
                inner()
            },
        )
        if (search.isNotEmpty()) {
            Box(
                Modifier
                    .size(42.dp)
                    .clickable { onSearch("") }
                    .semantics { contentDescription = "Clear the call search" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Industry.neutral200),
                    contentAlignment = Alignment.Center,
                ) {
                    DeskIcon(DeskIconKind.Close, 13.dp, Industry.neutral700)
                }
            }
        }
    }
}

/**
 * One applicant. Closed: a dot in the outcome's tone, the name and a meta line
 * (conf · desk status · attempts). Open: the number to ring, the WhatsApp
 * hand-off, the desk status changer, the outcome grid and the device-local
 * note. The city is deliberately absent — it never decided anything on a call
 * and the line reads faster without it.
 */
@Composable
private fun CallCard(
    card: ApplicantCard,
    record: CallRecord?,
    index: Int,
    nowMs: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOutcome: (String) -> Unit,
    onDial: () -> Unit,
    onWhatsApp: () -> Unit,
    onNote: (String) -> Unit,
    statusChoices: List<String>,
    onChangeStatus: (String) -> Unit,
) {
    val outcome = deskCallOutcome(record?.outcome)
    val (badgeBg, badgeFg) = statusColors(deskCallTone(outcome), dark = false)
    val meta = listOfNotNull(
        card.confNo?.value?.takeIf { it.isNotBlank() },
        card.status.value.takeIf { it.isNotBlank() },
        deskCallMeta(record, nowMs) ?: "No attempts yet",
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(
                border = if (expanded) Industry.accent else DeskStyle.cardBorder,
                elevation = if (expanded) DeskStyle.dialogElevation else DeskStyle.cardElevation,
            ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (outcome.isBlank()) Industry.neutral300 else badgeFg),
            )
            Text(
                "$index.",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Industry.neutral500,
                modifier = Modifier.width(28.dp),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    card.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.text,
                )
                Text(
                    meta,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.neutral600,
                )
            }
            Text(
                outcome.ifBlank { "To call" }.uppercase(),
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.08.em,
                maxLines = 1,
                color = if (outcome.isBlank()) Industry.neutral600 else badgeFg,
                modifier = Modifier
                    .clip(DeskStyle.pillShape)
                    .background(if (outcome.isBlank()) Industry.neutral100 else badgeBg)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            )
            DeskIcon(
                if (expanded) DeskIconKind.Close else DeskIconKind.ChevronDown,
                14.dp,
                Industry.neutral500,
            )
        }

        if (expanded) {
            Column(
                Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!card.mobile.isNullOrBlank()) {
                        ContactButton(DeskIconKind.Phone, card.mobile.orEmpty(), onDial)
                    }
                    ContactButton(
                        DeskIconKind.WhatsApp,
                        "WhatsApp",
                        onWhatsApp,
                        description = "WhatsApp ${card.displayName}",
                    )
                }
                if (statusChoices.isNotEmpty()) {
                    DeskStatusChanger(
                        current = card.status.value,
                        choices = statusChoices,
                        name = card.displayName,
                        onChange = onChangeStatus,
                    )
                }
                OutcomeGrid(outcome, onOutcome)
                NoteField(card.displayName, record?.note.orEmpty(), onNote)
                if (outcome.isNotBlank()) {
                    Text(
                        "↩ Back to To call",
                        fontSize = 12.sp,
                        color = Industry.neutral600,
                        modifier = Modifier
                            .clip(DeskStyle.controlShape)
                            .clickable { onOutcome("") }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .semantics { contentDescription = "Clear the outcome for ${card.displayName}" },
                    )
                }
            }
        }
    }
}

/** Dial / WhatsApp hand-off button — icon plus label, sized for a thumb. */
@Composable
private fun ContactButton(
    icon: DeskIconKind,
    label: String,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        Modifier
            .deskCard(shape = DeskStyle.controlShape, elevation = 0.dp)
            .clickable(onClick = onClick)
            .then(
                if (description == null) Modifier
                else Modifier.semantics { contentDescription = description },
            )
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DeskIcon(icon, 16.dp, Industry.accent)
        Text(
            label,
            fontFamily = if (icon == DeskIconKind.Phone) DipiMono else DipiSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            maxLines = 1,
            color = Industry.accent800,
        )
    }
}

/**
 * The desk status changer, inline. Same write the Applications sheet makes —
 * `GET /change-status` through the outbox — so a reconfirmation lands while
 * the applicant is still on the line instead of after a trip to another
 * section. Custom needs its own words before UPDATE does anything.
 */
@Composable
private fun DeskStatusChanger(
    current: String,
    choices: List<String>,
    name: String,
    onChange: (String) -> Unit,
) {
    val start = choices.firstOrNull { it.equals(current, ignoreCase = true) }
        ?: choices.firstOrNull { !it.contains("Custom", ignoreCase = true) }
        ?: choices.first()
    var pick by remember(current) { mutableStateOf(start) }
    var custom by remember(current) { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    val isCustom = pick.contains("Custom", ignoreCase = true)
    val resolved = if (isCustom) custom.trim() else pick

    Column(
        Modifier
            .fillMaxWidth()
            .background(Industry.neutral100, DeskStyle.controlShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Desk status",
                fontSize = 12.sp,
                color = Industry.neutral600,
            )
            Text(
                current.ifBlank { "—" },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = Industry.text,
            )
            Spacer(Modifier.weight(1f))
            Box {
                Row(
                    Modifier
                        .border(1.dp, Industry.neutral400, DeskStyle.controlShape)
                        .clip(DeskStyle.controlShape)
                        .clickable { menuOpen = true }
                        .semantics { contentDescription = "Choose a new status for $name" }
                        .padding(start = 12.dp, end = 9.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(pick, fontSize = 12.5.sp, maxLines = 1, color = Industry.text)
                    DeskIcon(DeskIconKind.ChevronDown, 13.dp, Industry.neutral600)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice, fontSize = 13.sp) },
                            onClick = {
                                pick = choice
                                menuOpen = false
                            },
                        )
                    }
                }
            }
            Text(
                "UPDATE",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 0.06.em,
                maxLines = 1,
                color = if (resolved.isBlank()) Industry.neutral500 else Color.White,
                modifier = Modifier
                    .clip(DeskStyle.controlShape)
                    .background(if (resolved.isBlank()) Industry.neutral200 else Industry.accent)
                    .clickable(enabled = resolved.isNotBlank()) { onChange(resolved) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
        if (isCustom) {
            BasicTextField(
                value = custom,
                onValueChange = { custom = it.take(60) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.5.sp, color = Industry.text),
                cursorBrush = SolidColor(Industry.accent),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .background(DeskStyle.cardFill, DeskStyle.controlShape)
                            .border(1.dp, Industry.neutral400, DeskStyle.controlShape)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (custom.isEmpty()) {
                            Text("Custom status text", fontSize = 12.5.sp, color = Industry.neutral500)
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Custom status for $name" },
            )
        }
    }
}

/** The outcome grid: three across, so five buttons read as one block. */
@Composable
private fun OutcomeGrid(outcome: String, onOutcome: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CALL_OUTCOMES.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { label ->
                    val on = outcome == label
                    val (bg, fg) = statusColors(deskCallTone(label), dark = false)
                    Box(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .clip(DeskStyle.controlShape)
                            .background(if (on) bg else Color.Transparent)
                            .border(
                                if (on) 1.5.dp else 1.dp,
                                if (on) fg else Industry.neutral300,
                                DeskStyle.controlShape,
                            )
                            .clickable { onOutcome(label) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            color = if (on) fg else Industry.neutral700,
                        )
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Device-local note — never sent, never logged. */
@Composable
private fun NoteField(name: String, note: String, onNote: (String) -> Unit) {
    BasicTextField(
        value = note,
        onValueChange = { onNote(it.take(200)) },
        textStyle = TextStyle(fontSize = 12.5.sp, color = Industry.text),
        cursorBrush = SolidColor(Industry.accent),
        decorationBox = { inner ->
            Box(
                Modifier
                    .background(DeskStyle.cardFill, DeskStyle.controlShape)
                    .border(1.dp, Industry.neutral300, DeskStyle.controlShape)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
            ) {
                if (note.isEmpty()) {
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
            .heightIn(min = 40.dp)
            .semantics { contentDescription = "Note for $name" },
    )
}
