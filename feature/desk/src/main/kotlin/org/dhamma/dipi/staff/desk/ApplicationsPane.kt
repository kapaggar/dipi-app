package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.ui.ApplicantHistorySections
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard
import org.dhamma.dipi.staff.ui.theme.statusColors

/**
 * List–detail on one screen: pick a row on the left, everything about them
 * on the right — no navigation, no back stack. The severity dot next to a
 * name repeats the audit verdict at a glance; a mono "!" marks health
 * disclosures on file. Status chips above the list reuse the shared
 * selected/toggleStatus/WorklistFilter machinery, so [rows] is the already
 * filtered list.
 */
@Composable
fun ApplicationsPane(
    rows: List<ApplicantCard>,
    flagsById: Map<ApplicantId, List<AuditFlag>>,
    selectedId: ApplicantId?,
    onSelect: (ApplicantCard) -> Unit,
    onChangeStatus: (ApplicantCard) -> Unit,
    onDial: (String) -> Unit,
    onEdit: (ApplicantCard) -> Unit,
    loadPhoto: suspend (ApplicantId) -> ImageBitmap? = { null },
    counts: Map<String, Int> = emptyMap(),
    selectedStatuses: Set<String> = emptySet(),
    onToggleStatus: (String) -> Unit = {},
    sensitiveById: Map<ApplicantId, SensitiveInfo> = emptyMap(),
    gender: String = "Both",
    seniority: String = "Both",
    onGender: (String) -> Unit = {},
    onSeniority: (String) -> Unit = {},
    historyById: Map<ApplicantId, ApplicantDeskHistory> = emptyMap(),
    onExpandHistory: (ApplicantId, String) -> Unit = { _, _ -> },
    onOpenClarification: (ApplicantId, Int) -> Unit = { _, _ -> },
) {
    val scoped = deskScoped(rows, deskGenderScope(gender), deskSeniorityScope(seniority))
    val selected = scoped.firstOrNull { it.id == selectedId } ?: scoped.firstOrNull()
    val chipCounts = counts.filterKeys { it != "All" }.toList()
        .ifEmpty { rows.groupingBy { it.status.value }.eachCount().toList() }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(396.dp)
                .fillMaxHeight()
                .rightHairline(Industry.neutral300),
        ) {
            if (chipCounts.isNotEmpty()) {
                StatusChipRow(chipCounts, selectedStatuses, onToggleStatus)
            }
            DeskScopeFilters(
                gender,
                seniority,
                onGender,
                onSeniority,
                Modifier
                    .fillMaxWidth()
                    .bottomHairline(Industry.neutral200)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(scoped, key = { it.id.value }) { card ->
                    AppListRow(
                        card = card,
                        flags = flagsById[card.id].orEmpty(),
                        health = sensitiveById[card.id]?.health?.isNotEmpty() == true,
                        selected = card.id == selected?.id,
                        onClick = { onSelect(card) },
                    )
                }
            }
        }
        if (selected != null) {
            AppDetail(
                card = selected,
                flags = flagsById[selected.id].orEmpty(),
                sensitive = sensitiveById[selected.id],
                onChangeStatus = { onChangeStatus(selected) },
                onDial = { selected.mobile?.let(onDial) },
                onEdit = { onEdit(selected) },
                loadPhoto = loadPhoto,
                historyById = historyById,
                onExpandHistory = onExpandHistory,
                onOpenClarification = onOpenClarification,
                modifier = Modifier.weight(1f),
            )
        } else {
            DeskEmpty("No applications loaded.", Modifier.weight(1f).padding(vertical = 46.dp))
        }
    }
}

/**
 * Multi-select status filter, one chip per status present plus "All".
 * Rounded, accent border + accent100 fill when selected, counts in
 * mono — the same visual language as the check-in chips.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusChipRow(
    counts: List<Pair<String, Int>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .bottomHairline(Industry.neutral200)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusChip("All", count = null, on = selected.isEmpty()) { onToggle("All") }
        counts.forEach { (label, n) ->
            StatusChip(label, n, on = selected.any { it.equals(label, true) }) { onToggle(label) }
        }
    }
}

@Composable
private fun StatusChip(label: String, count: Int?, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(DeskStyle.controlShape)
            .border(1.dp, if (on) Industry.accent else Industry.neutral400, DeskStyle.controlShape)
            .background(if (on) Industry.accent100 else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Filter $label" }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = if (on) Industry.accent800 else Industry.neutral700,
        )
        if (count != null) {
            Text(
                "$count",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (on) Industry.accent700 else Industry.neutral500,
            )
        }
    }
}

@Composable
private fun AppListRow(
    card: ApplicantCard,
    flags: List<AuditFlag>,
    health: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val hard = flags.any { it.severity != AuditSeverity.SOFT }
    val dot = when {
        hard -> Industry.accent
        flags.isNotEmpty() -> Industry.neutral400
        else -> Color.Transparent
    }
    // Sleek pass: the selected row is a rounded accent-tinted highlight
    // instead of the wireframe's edge bar.
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .bottomHairline(Industry.neutral200)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(DeskStyle.controlShape)
            .background(if (selected) Industry.accent100 else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    card.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.text,
                )
                Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
                if (health) {
                    // Health-disclosure marker — same at-a-glance language as
                    // the severity dot, mono so it reads as a badge not a word.
                    Text(
                        "!",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Industry.accent,
                        modifier = Modifier.semantics { contentDescription = "Health disclosures for ${card.displayName}" },
                    )
                }
            }
            Text(
                listOfNotNull(card.age?.toString(), card.gender.name).joinToString(" ") +
                    (card.city?.let { " · $it" } ?: ""),
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Industry.neutral600,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                card.confNo?.display() ?: "—",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp,
                color = Industry.neutral500,
            )
            StatusPill(card, fontSize = 10.5f)
        }
    }
}

/**
 * Wrap-content status pill — never a fixed width, so the full word always
 * shows ("CANCELLED", never "CANCELL"; owner feedback 2026-08-16).
 */
@Composable
private fun StatusPill(card: ApplicantCard, fontSize: Float) {
    val (bg, fg) = statusColors(card.status.tone, dark = false)
    Text(
        card.status.value,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = 1,
        color = fg,
        modifier = Modifier
            .clip(DeskStyle.pillShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun AppDetail(
    card: ApplicantCard,
    flags: List<AuditFlag>,
    sensitive: SensitiveInfo?,
    onChangeStatus: () -> Unit,
    onDial: () -> Unit,
    onEdit: () -> Unit,
    loadPhoto: suspend (ApplicantId) -> ImageBitmap?,
    historyById: Map<ApplicantId, ApplicantDeskHistory> = emptyMap(),
    onExpandHistory: (ApplicantId, String) -> Unit = { _, _ -> },
    onOpenClarification: (ApplicantId, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DetailPhoto(card, loadPhoto)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    card.displayName,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 33.sp,
                    color = Industry.text,
                )
                Text(
                    listOfNotNull(
                        listOfNotNull(card.age?.toString(), card.gender.name).joinToString(" "),
                        listOfNotNull(card.city, card.state, card.country)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { null },
                    ).joinToString(" · "),
                    fontSize = 14.sp,
                    color = Industry.neutral700,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill(card, fontSize = 11.5f)
                    Text(
                        card.confNo?.display() ?: "no conf number",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Industry.neutral600,
                    )
                }
                Text(
                    courseCountsLine(card) ?: historyLine(card),
                    fontSize = 12.sp,
                    color = Industry.neutral600,
                )
            }
        }

        IdVerificationBlock(sensitive)

        val health = sensitive?.health.orEmpty()
        if (health.isNotEmpty()) {
            HealthPanel(health)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .deskCard(border = Industry.accent, elevation = 0.dp)
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DeskKicker(
                if (flags.isEmpty()) "AUDIT CLEAN · NOTHING TO FIX"
                else "NEEDS ATTENTION · ${flags.size}",
                Industry.accent700,
            )
            flags.forEach { flag ->
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        flag.label,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Industry.text,
                    )
                    Text(
                        flag.detail,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = Industry.neutral600,
                    )
                }
            }
        }

        Column(Modifier.fillMaxWidth().topHairline(Industry.neutral300)) {
            FactRow("Mobile", card.mobile ?: "—")
            FactRow("Email", card.email ?: "—")
            FactRow("Date of birth", card.dob ?: "—")
            FactRow("Applied", card.createdAt ?: "—")
        }

        ApplicantHistorySections(
            history = historyById[card.id] ?: ApplicantDeskHistory(),
            onExpand = { key -> onExpandHistory(card.id, key) },
            onOpenClarification = { clarId -> onOpenClarification(card.id, clarId) },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeskPrimaryButton("Change status", onChangeStatus)
            DeskOutlineButton("Call", onDial)
            DeskOutlineButton("Edit", onEdit)
        }
    }
}

private sealed interface DetailPhotoState {
    object Loading : DetailPhotoState
    object Missing : DetailPhotoState
    class Ready(val bitmap: ImageBitmap) : DetailPhotoState
}

/**
 * The rounded photo card, enlarged to 170×183 (dipi's 260:280 ratio) so
 * the face reads across a desk. The live photo replaces the initials once
 * fetched; while loading the initials sit dimmed (no shimmer — the design
 * system forbids entrance animations), and on 403/404 they stay as the
 * permanent fallback.
 */
@Composable
private fun DetailPhoto(card: ApplicantCard, loadPhoto: suspend (ApplicantId) -> ImageBitmap?) {
    val photo by produceState<DetailPhotoState>(DetailPhotoState.Loading, card.id) {
        value = DetailPhotoState.Loading
        value = loadPhoto(card.id)?.let { DetailPhotoState.Ready(it) } ?: DetailPhotoState.Missing
    }
    Box(
        Modifier
            .size(170.dp, 183.dp)
            .deskCard(fill = Industry.neutral100, elevation = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val p = photo) {
            is DetailPhotoState.Ready -> Image(
                bitmap = p.bitmap,
                contentDescription = "Photo of ${card.displayName}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            else -> Text(
                initials(card.displayName),
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = if (photo is DetailPhotoState.Loading) Industry.neutral400 else Industry.neutral500,
            )
        }
    }
}

/**
 * The physical-document check: the desk admin reads the full number off the
 * screen against the ID in the applicant's hand. Display only — the value
 * lives in the session-scoped in-memory map, never in Room or logs.
 */
@Composable
private fun IdVerificationBlock(sensitive: SensitiveInfo?) {
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(elevation = 0.dp)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        DeskKicker("ID VERIFICATION", Industry.neutral500)
        val label = sensitive?.idLabel
        val number = sensitive?.idNumber
        if (label != null && number != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                )
                Text(
                    number,
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = Industry.text,
                )
            }
        } else {
            Text("No ID on file", fontSize = 13.sp, color = Industry.neutral500)
        }
    }
}

/**
 * Surviving health disclosures (post noise-filter), above the audit panel.
 * Accent frame on accent100 — attention, not alarm; no sound, no motion.
 */
@Composable
private fun HealthPanel(health: Map<String, String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(fill = Industry.accent100, border = Industry.accent, elevation = 0.dp)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DeskKicker("HEALTH · VERIFY WITH APPLICANT", Industry.accent700)
        health.forEach { (label, text) ->
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Industry.text,
                )
                Text(
                    text,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Industry.neutral800,
                )
            }
        }
    }
}

@Composable
private fun FactRow(key: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .bottomHairline(Industry.neutral200)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, fontSize = 12.5.sp, color = Industry.neutral600, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = Industry.text)
    }
}

internal fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

internal fun historyLine(card: ApplicantCard): String {
    val h = card.history
    val recent = h?.recent?.takeIf { it.isNotBlank() }
    val counts = h?.counts?.filter { it.n > 0 }
        ?.joinToString(" · ") { "${it.n} ${it.label}" }
        ?.takeIf { it.isNotBlank() }
    return when {
        recent != null -> "Last course · $recent"
        counts != null -> "History · $counts"
        else -> "First course at this centre"
    }
}

/** "Courses · 4 total · 3× 10-day · 1× 20-day" — null when no counts parsed. */
internal fun courseCountsLine(card: ApplicantCard): String? {
    val counts = card.history?.counts?.filter { it.n > 0 }.orEmpty()
    if (counts.isEmpty()) return null
    val total = counts.sumOf { it.n }
    return "Courses · $total total · " + counts.joinToString(" · ") { "${it.n}× ${it.label}" }
}
