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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.LocalDarkTheme
import org.dhamma.dipi.staff.ui.theme.deskCard
import org.dhamma.dipi.staff.ui.theme.statusColors

fun applicationsEmptyCopy(filtersOn: Boolean): String =
    if (filtersOn) "No applications match these filters." else "No applications loaded."

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
    totalApplications: Int = rows.size,
    openDetailRequest: Int = 0,
    detailTarget: ApplicantCard? = null,
    gender: String = "Both",
    seniority: String = "Both",
    onGender: (String) -> Unit = {},
    onSeniority: (String) -> Unit = {},
    historyById: Map<ApplicantId, ApplicantDeskHistory> = emptyMap(),
    onExpandHistory: (ApplicantId, String) -> Unit = { _, _ -> },
    onOpenClarification: (ApplicantId, Int) -> Unit = { _, _ -> },
) {
    val industry = LocalIndustry.current
    val scoped = deskScoped(rows, deskGenderScope(gender), deskSeniorityScope(seniority))
    // The rail is 190dp.  Keep list/detail side by side only when the pane itself
    // can hold both; portrait desks otherwise push a detail above the list.
    val compactDetail = LocalConfiguration.current.screenWidthDp - 190 < 910
    var detailOpen by remember { mutableStateOf(false) }
    var detailSession by remember { mutableStateOf(0) }
    LaunchedEffect(openDetailRequest) { if (openDetailRequest > 0) detailOpen = true }
    val selected = scoped.firstOrNull { it.id == selectedId }
        ?: detailTarget?.takeIf { openDetailRequest > 0 && it.id == selectedId }
        ?: scoped.firstOrNull()
    val showDetail = !compactDetail || detailOpen
    BackHandler(enabled = compactDetail && detailOpen) {
        detailOpen = false
        detailSession++
    }
    val chipCounts = counts.filterKeys { it != "All" }.toList()
        .ifEmpty { rows.groupingBy { it.status.value }.eachCount().toList() }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .then(if (compactDetail) Modifier.weight(1f) else Modifier.width(396.dp))
                .fillMaxHeight()
                .rightHairline(industry.neutral300),
        ) {
            if (compactDetail && showDetail && selected != null) {
                AppDetail(
                    card = selected,
                    outsideScope = scoped.none { it.id == selected.id },
                    flags = flagsById[selected.id].orEmpty(),
                    sensitive = sensitiveById[selected.id],
                    onChangeStatus = { onChangeStatus(selected) },
                    onDial = { selected.mobile?.let(onDial) },
                    onEdit = { onEdit(selected) },
                    loadPhoto = loadPhoto,
                    historyById = historyById,
                    onExpandHistory = onExpandHistory,
                    onOpenClarification = onOpenClarification,
                    onBack = {
                        detailOpen = false
                        detailSession++
                    },
                    detailSession = detailSession,
                )
                return@Row
            }
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
                    .bottomHairline(industry.neutral200)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )
            Text(
                "Showing ${scoped.size} of $totalApplications applications",
                fontSize = 14.sp,
                color = industry.neutral600,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.weight(1f)) {
                items(scoped, key = { it.id.value }) { card ->
                    AppListRow(
                        card = card,
                        flags = flagsById[card.id].orEmpty(),
                        health = sensitiveById[card.id]?.health?.isNotEmpty() == true,
                        selected = !compactDetail && card.id == selected?.id,
                        onClick = {
                            onSelect(card)
                            if (compactDetail) {
                                detailSession++
                                detailOpen = true
                            }
                        },
                    )
                }
                if (scoped.isEmpty()) {
                    item {
                        val filtersOn = selectedStatuses.isNotEmpty() || gender != "Both" || seniority != "Both"
                        DeskEmpty(
                            applicationsEmptyCopy(filtersOn),
                            Modifier.fillMaxWidth().padding(vertical = 46.dp),
                        )
                    }
                }
            }
        }
        if (!compactDetail && selected != null) {
            AppDetail(
                card = selected,
                outsideScope = scoped.none { it.id == selected.id },
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
        } else if (!compactDetail && scoped.isNotEmpty()) {
            val filtersOn = selectedStatuses.isNotEmpty() || gender != "Both" || seniority != "Both"
            DeskEmpty(applicationsEmptyCopy(filtersOn), Modifier.weight(1f).padding(vertical = 46.dp))
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
    val industry = LocalIndustry.current
    Column(
        Modifier
            .fillMaxWidth()
            .bottomHairline(industry.neutral200)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StatusChip("All", count = null, on = selected.isEmpty()) { onToggle("All") }
            counts.forEach { (label, n) ->
                StatusChip(label, n, on = selected.any { it.equals(label, true) }) { onToggle(label) }
            }
        }
        Text("◦ Off arriving roll", fontSize = 14.sp, color = industry.neutral600)
    }
}

@Composable
private fun StatusChip(label: String, count: Int?, on: Boolean, onClick: () -> Unit) {
    val industry = LocalIndustry.current
    val displayLabel = when {
        deskOffRollStatus(label) -> "◦ $label"
        label.replace(" ", "").equals("WaitList", ignoreCase = true) -> "$label · Held"
        else -> label
    }
    Row(
        Modifier
            .clip(DeskStyle.controlShape)
            .border(1.dp, if (on) industry.accent else industry.neutral400, DeskStyle.controlShape)
            .background(if (on) industry.accent100 else Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .semantics { contentDescription = "Filter $label" }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            displayLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            color = if (on) industry.accent800 else industry.neutral700,
        )
        if (count != null) {
            Text(
                "$count",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (on) industry.accent700 else industry.neutral500,
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
    val industry = LocalIndustry.current
    val hard = flags.any { it.severity != AuditSeverity.SOFT }
    val dot = when {
        hard -> industry.accent
        flags.isNotEmpty() -> industry.neutral400
        else -> Color.Transparent
    }
    // Sleek pass: the selected row is a rounded accent-tinted highlight
    // instead of the wireframe's edge bar.
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .bottomHairline(industry.neutral200)
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .clip(DeskStyle.controlShape)
            .background(if (selected) industry.accent100 else Color.Transparent)
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
                    color = industry.text,
                )
                Box(Modifier.size(5.dp).clip(CircleShape).background(dot))
                if (health) {
                    // Health-disclosure marker — same at-a-glance language as
                    // the severity dot, mono so it reads as a badge not a word.
                    Text(
                        "!",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = industry.accent,
                        modifier = Modifier.semantics { contentDescription = "Health disclosures for ${card.displayName}" },
                    )
                }
            }
            Text(
                listOfNotNull(card.age?.toString(), card.gender.name).joinToString(" ") +
                    (card.city?.let { " · $it" } ?: ""),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = industry.neutral600,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                card.confNo?.display() ?: "-",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = industry.neutral600,
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
    val industry = LocalIndustry.current
    val (bg, fg) = statusColors(card.status.tone, dark = LocalDarkTheme.current)
    Text(
        (if (deskOffRollStatus(card.status.value)) "◦ " else "") + card.status.value,
        fontSize = maxOf(fontSize, 14f).sp,
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
    onBack: (() -> Unit)? = null,
    detailSession: Int = 0,
    outsideScope: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val industry = LocalIndustry.current
    Column(modifier.fillMaxHeight()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(top = 24.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (onBack != null) {
                Text(
                    "← Back to list",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = industry.accent800,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onBack)
                        .padding(vertical = 14.dp),
                )
            }
            if (outsideScope) Text("Outside current list filters · opened from another view", fontSize = 14.sp, color = industry.neutral600)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DetailPhoto(card, loadPhoto)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        card.displayName,
                        fontFamily = DipiCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 33.sp,
                        color = industry.text,
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
                        color = industry.neutral700,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusPill(card, fontSize = 11.5f)
                        Text(
                            card.confNo?.display() ?: "no conf number",
                            fontFamily = DipiMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = industry.neutral600,
                        )
                    }
                    Text(
                        courseCountsLine(card) ?: historyLine(card),
                        fontSize = 14.sp,
                        color = industry.neutral600,
                    )
                }
            }

            IdVerificationBlock(card.id, sensitive, detailSession)

            val health = sensitive?.health.orEmpty()
            if (health.isNotEmpty()) {
                HealthPanel(health)
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .deskCard(border = industry.accent, elevation = 0.dp)
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DeskKicker(
                    if (flags.isEmpty()) "AUDIT CLEAN · NOTHING TO FIX"
                    else "NEEDS ATTENTION · ${flags.size}",
                    industry.accent700,
                )
                flags.forEach { flag ->
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            flag.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = industry.text,
                        )
                        Text(
                            flag.detail,
                            fontFamily = DipiMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = industry.neutral600,
                        )
                    }
                }
            }

            Column(Modifier.fillMaxWidth().topHairline(industry.neutral300)) {
                FactRow("Mobile", card.mobile ?: "-")
                FactRow("Email", card.email ?: "-")
                FactRow("Date of birth", card.dob ?: "-")
                FactRow("Applied", card.createdAt ?: "-")
            }

            ApplicantHistorySections(
                history = historyById[card.id] ?: ApplicantDeskHistory(),
                onExpand = { key -> onExpandHistory(card.id, key) },
                onOpenClarification = { clarId -> onOpenClarification(card.id, clarId) },
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .topHairline(industry.neutral300)
                .padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DeskPrimaryButton("Change status", onChangeStatus)
            DeskOutlineButton("Call", onDial)
            DeskOutlineButton("Edit on desk site ↗", onEdit)
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
    val industry = LocalIndustry.current
    val photo by produceState<DetailPhotoState>(DetailPhotoState.Loading, card.id) {
        value = DetailPhotoState.Loading
        value = loadPhoto(card.id)?.let { DetailPhotoState.Ready(it) } ?: DetailPhotoState.Missing
    }
    Box(
        Modifier
            .size(170.dp, 183.dp)
            .deskCard(fill = industry.neutral100, elevation = 0.dp),
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
                color = if (photo is DetailPhotoState.Loading) industry.neutral400 else industry.neutral500,
            )
        }
    }
}

/**
 * The physical-document check. The number begins masked and can be revealed
 * only for the current detail view. It lives in the session-scoped in-memory
 * map, never in Room or logs.
 */
@Composable
private fun IdVerificationBlock(applicantId: ApplicantId, sensitive: SensitiveInfo?, detailSession: Int) {
    val industry = LocalIndustry.current
    var revealed by remember(applicantId, sensitive?.idLabel, sensitive?.idNumber, detailSession) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(elevation = 0.dp)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        DeskKicker("ID VERIFICATION", industry.neutral600)
        val label = sensitive?.idLabel
        val number = sensitive?.idNumber
        if (label != null && number != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = industry.text,
                )
                Text(
                    if (revealed) number else maskId(number),
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp,
                    color = industry.text,
                )
                Text(
                    if (revealed) "Hide" else "Reveal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = industry.accent800,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { revealed = !revealed }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                )
            }
        } else {
            Text("No ID on file", fontSize = 14.sp, color = industry.neutral600)
        }
    }
}

/**
 * Surviving health disclosures (post noise-filter), above the audit panel.
 * Accent frame on accent100 — attention, not alarm; no sound, no motion.
 */
@Composable
private fun HealthPanel(health: Map<String, String>) {
    val industry = LocalIndustry.current
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(fill = industry.accent100, border = industry.accent, elevation = 0.dp)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DeskKicker("HEALTH · VERIFY WITH APPLICANT", industry.accent700)
        health.forEach { (label, text) ->
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = industry.text,
                )
                Text(
                    text,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = industry.neutral800,
                )
            }
        }
    }
}

@Composable
private fun FactRow(key: String, value: String) {
    val industry = LocalIndustry.current
    Row(
        Modifier
            .fillMaxWidth()
            .bottomHairline(industry.neutral200)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, fontSize = 14.sp, color = industry.neutral600, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = industry.text)
    }
}

internal fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

/** Keep the original grouping while never exposing an ID until the registrar asks. */
private fun maskId(number: String): String = number.map { char ->
    when {
        char.isLetterOrDigit() -> '•'
        else -> char
    }
}.joinToString("")

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
