package org.dhamma.dipi.staff.teacher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HealthAnswerKind
import org.dhamma.dipi.staff.model.HealthRow
import org.dhamma.dipi.staff.model.completeZeroHistory
import org.dhamma.dipi.staff.model.healthAnswerCounts
import org.dhamma.dipi.staff.model.healthAnswerKind
import org.dhamma.dipi.staff.model.healthAnswerPositions
import org.dhamma.dipi.staff.model.healthBadgeLabel
import org.dhamma.dipi.staff.model.healthSourceCaption
import org.dhamma.dipi.staff.model.healthSummaryText
import org.dhamma.dipi.staff.model.historyTileValue
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.ThemeIndustry as Industry
import org.dhamma.dipi.staff.ui.theme.LocalReadableTokens

// Fixed hexes DESIGN.md § Course ops names outside the ramp tokens.
private val PaleFill = Color(0xFFFAFAFB)
private val RowHairline = Color(0xFFEDEDF1)
private val ZeroTileBorder = Color(0xFFE7E7EA)

private fun Modifier.bottomHairline(color: Color): Modifier = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
}

/**
 * Frame 2d — the read-only student card: what the applicant wrote, in the
 * applicant's own words. Fully prop-driven, no ViewModel.
 *
 * - Header 68dp: back, name + status chip, group placement line (room and
 *   seat live next to the photo), and the 56dp elevated `‹ ›` pair walking
 *   the current group (disabled ends at 38% alpha — no drawn spec).
 * - Left 404dp FIXED: photo 132×158, Room/Seat plus the kept Personal rows
 *   and roll facts, ten 50dp history tiles in SERVER order (zeros stay),
 *   history meta including first/last course teacher when the page has them.
 * - Right column SCROLLS: one card per Health row in order, labels
 *   verbatim, `YES` tag + 14.5sp/1.5 body NEVER truncated; answered rows
 *   tint `accent100` on `accent300` with a 2dp `accent500` left rule; empty
 *   rows keep their `NO` tag with no body; Pregnancy renders `N/A` for
 *   gender M.
 * - No edit, no note, no share, no export. Never summarise, score, rank or
 *   colour-code an answer.
 *
 * [card] == null is the not-yet-landed state: offline it reads honestly
 * "Not cached - connect to load"; online the prefetch is still in
 * flight.
 */
@Composable
fun StudentCardScreen(
    row: RollRow,
    group: RollGroup,
    card: ApplicationCard?,
    offline: Boolean = false,
    canPrev: Boolean = false,
    canNext: Boolean = false,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    loadPhoto: suspend (ApplicantId) -> ImageBitmap? = { null },
    /** Door the card was opened from — Teacher list or Seating plan. */
    backLabel: String = "Teacher list",
    /** Hall + seat when the door was the plan, e.g. `Female hall · seat A1`. */
    cameFrom: String? = null,
) {
    Column(Modifier.fillMaxSize().background(Industry.bg)) {
        Header(row, group, card, canPrev, canNext, onPrev, onNext, onBack, backLabel)
        if (card == null) {
            NotCachedBody(offline)
        } else {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                if (maxWidth < 800.dp) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("card-phone-scroll")) {
                        LeftColumn(row, card, loadPhoto)
                        Spacer(Modifier.height(16.dp))
                        RightColumn(card, group.gender, cameFrom)
                    }
                } else {
                    Row(
                        Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Column(Modifier.width(404.dp).fillMaxHeight().verticalScroll(rememberScrollState()).testTag("card-facts-scroll")) {
                            LeftColumn(row, card, loadPhoto)
                        }
                        RightColumn(card, group.gender, cameFrom, Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()))
                    }
                }
            }
        }
    }
}

/* ── Header band 60dp ───────────────────────────────────────────────── */

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Header(
    row: RollRow,
    group: RollGroup,
    card: ApplicationCard?,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    backLabel: String,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onBack)
                    .padding(horizontal = 10.dp).testTag("card-back"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("‹", fontSize = 18.sp, color = Industry.secondary)
                Spacer(Modifier.width(8.dp))
                Text(backLabel, fontSize = 14.sp, color = Industry.secondary)
            }
            WalkButton("‹", canPrev, onPrev, "card-prev")
            Spacer(Modifier.width(12.dp))
            WalkButton("›", canNext, onNext, "card-next")
        }
        Text(row.name, fontFamily = DipiCondensed, fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp, color = Industry.text, modifier = Modifier.fillMaxWidth().testTag("card-name"))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StatusChip(group, card)
            Text(placementLine(row, group), fontSize = 12.5.sp, color = Industry.secondary,
                modifier = Modifier.testTag("card-placement"))
        }
    }
}

/** `OLD · OM7` — roll seniority + the card's conf when present. */
@Composable
private fun StatusChip(group: RollGroup, card: ApplicationCard?) {
    val text = listOfNotNull(group.seniorityWord.uppercase(), card?.conf).joinToString(" · ")
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.6.sp,
        color = Industry.accent700,
        modifier = Modifier
            .padding(start = 10.dp)
            .background(Industry.accent100, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("card-status-chip"),
    )
}

/**
 * `Group 1 · TAM · 1 of 18 in this group` — room and seat sit next to the
 * photo (owner 2026-09-08), not in this kicker.
 */
private fun placementLine(row: RollRow, group: RollGroup): String = listOfNotNull(
    "Group ${group.group}",
    group.code,
    "${row.sn} of ${group.total} in this group",
).joinToString(" · ")

@Composable
private fun WalkButton(glyph: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier
            .size(56.dp)
            .shadow(4.dp, shape, clip = false)
            .background(Industry.card, shape)
            .border(1.dp, if (enabled) Industry.neutral300 else Industry.neutral200, shape)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { contentDescription = if (tag == "card-prev") "Previous applicant" else "Next applicant"; if (!enabled) disabled() }
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 22.sp, color = if (enabled) Industry.neutral700 else Industry.neutral300)
    }
}

/* ── The not-yet-landed body ────────────────────────────────────────── */

@Composable
private fun NotCachedBody(offline: Boolean) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        if (offline) {
            Text(
                "Not cached - connect to load",
                fontSize = 14.sp,
                color = Industry.neutral700,
                modifier = Modifier.testTag("card-not-cached"),
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "This application hasn't arrived yet",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Industry.text,
                    modifier = Modifier.testTag("card-fetching"),
                )
                Text(
                    "The roll row is here; the application behind it is still being pulled. " +
                        "Personal, course history and the six answers appear as soon as it lands.",
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = Industry.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 7.dp).width(480.dp),
                )
            }
        }
    }
}

/* ── Left column, 404dp fixed — the facts, compressed ───────────────── */

@Composable
private fun LeftColumn(
    row: RollRow,
    card: ApplicationCard,
    loadPhoto: suspend (ApplicantId) -> ImageBitmap?,
) {
    Column(Modifier.width(404.dp).testTag("card-left")) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PhotoBox(row.applicantId, card.hasPhoto, loadPhoto)
            Column(Modifier.weight(1f)) {
                photoSideFacts(row, card).forEach { (key, value) ->
                    PersonalRow(key, value, tag = photoFactTag(key))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Kicker("COURSE HISTORY")
        Spacer(Modifier.height(8.dp))
        CourseHistoryBlock(card)
        Spacer(Modifier.height(8.dp))
        MetaRow("First Course", historyMeta(card.firstCourse))
        MetaRow("First Course Teacher", historyMeta(card.firstCourseTeacher))
        MetaRow("Last Course", historyMeta(card.lastCourse))
        MetaRow("Last Course Teacher", historyMeta(card.lastCourseTeacher))
        MetaRow("Practice Details", historyMeta(card.practiceDetails))
    }
}

@Composable
private fun PhotoBox(
    id: ApplicantId?,
    hasPhoto: Boolean,
    loadPhoto: suspend (ApplicantId) -> ImageBitmap?,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(width = 132.dp, height = 158.dp)
            .background(Industry.neutral200, shape)
            .border(1.dp, Industry.neutral300, shape)
            .testTag("card-photo"),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPhoto && id != null) {
            val photo by produceState<ImageBitmap?>(null, id) { value = loadPhoto(id) }
            photo?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Text(
                "NO PHOTO ON\nTHE APPLICATION",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                lineHeight = 13.5.sp,
                letterSpacing = 1.sp,
                color = Industry.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Room and seat sit beside the photo; Gender / Nationality / Monk / Applied On / A-List stay off the card. */
private fun photoSideFacts(row: RollRow, card: ApplicationCard): List<Pair<String, String>> = buildList {
    row.room.takeIf { it.isNotBlank() }?.let { add("Room" to it) }
    row.seat.takeIf { it.isNotBlank() }?.let { add("Seat" to it) }
    card.personal
        .filter { (key, _) ->
            ApplicationCard.PERSONAL_CARD_HIDDEN.none { hidden -> hidden.equals(key, ignoreCase = true) }
        }
        .forEach { add(it) }
    row.city.takeIf { it.isNotBlank() }?.let { add("City" to it) }
    row.occupation.takeIf { it.isNotBlank() }?.let { add("Occupation" to it) }
    row.education.takeIf { it.isNotBlank() }?.let { add("Education" to it) }
    row.languages.takeIf { it.isNotBlank() }?.let { add("Languages" to it) }
}

private fun photoFactTag(key: String): String? = when (key.lowercase()) {
    "room" -> "card-photo-room"
    "seat" -> "card-photo-seat"
    else -> null
}

private fun historyMeta(value: String): String = value.trim().ifBlank { "Not provided" }

@Composable
private fun CourseHistoryBlock(card: ApplicationCard) {
    var expanded by remember(card.conf, card.historyCountsPresent) { mutableStateOf(false) }
    val collapsed = card.completeZeroHistory() && !expanded
    if (collapsed) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Industry.neutral100, RoundedCornerShape(6.dp))
                .border(1.dp, Industry.neutral200, RoundedCornerShape(6.dp))
                .padding(12.dp)
                .testTag("history-collapsed"),
        ) {
            Text("No prior courses recorded", fontSize = 13.sp, color = Industry.text)
            Text(
                "Show the ten course types",
                fontSize = 12.sp,
                color = Industry.accent700,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .heightIn(min = 48.dp).clickable { expanded = true }
                    .testTag("history-expand"),
            )
        }
        return
    }
    ApplicationCard.HISTORY_ORDER.chunked(5).forEachIndexed { i, chunk ->
        if (i > 0) Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chunk.forEach { key ->
                HistoryTile(key, card.historyTileValue(key), Modifier.weight(1f))
            }
        }
    }
    if (card.completeZeroHistory() && expanded) {
        Text(
            "Hide course types",
            fontSize = 12.sp,
            color = Industry.accent700,
            modifier = Modifier
                .padding(top = 6.dp)
                .heightIn(min = 48.dp).clickable { expanded = false }
                .testTag("history-collapse"),
        )
    }
}

@Composable
private fun PersonalRow(key: String, value: String, tag: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .bottomHairline(Industry.neutral300)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, fontSize = 12.sp, color = Industry.caption, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            value,
            fontFamily = DipiMono,
            fontSize = 12.5.sp,
            color = Industry.text,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Count tile — zeros STAY when the source supplied them. Missing keys read Not provided. */
@Composable
private fun HistoryTile(key: String, value: String, modifier: Modifier) {
    val shape = RoundedCornerShape(5.dp)
    val n = value.toIntOrNull()
    val nonZero = n != null && n > 0
    Column(
        modifier
            .heightIn(min = 50.dp)
            .background(if (nonZero) Industry.accent100 else Industry.card, shape)
            .border(1.dp, if (nonZero) Industry.accent300 else Industry.neutral300, shape)
            .testTag("history-tile-$key"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            value,
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (n != null) 18.sp else 9.sp,
            color = if (nonZero) Industry.accent700 else Industry.caption,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            key.uppercase(),
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 8.5.sp,
            letterSpacing = 0.9.sp,
            color = Industry.caption,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetaRow(key: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .bottomHairline(Industry.neutral300)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(key, fontSize = 12.sp, lineHeight = 15.6.sp, color = Industry.caption, modifier = Modifier.width(104.dp))
        Text(value, fontSize = 13.sp, lineHeight = 16.9.sp, color = Industry.text, modifier = Modifier.weight(1f))
    }
}

/* ── Right column — what the applicant wrote ────────────────────────── */

@Composable
private fun RightColumn(card: ApplicationCard, gender: Gender, cameFrom: String?, modifier: Modifier = Modifier) {
    val positions = healthAnswerPositions(card)
    val counts = healthAnswerCounts(positions, gender)
    val whose = if (gender == Gender.F) "her" else "his"
    Column(
        modifier.testTag("card-answers"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Column {
            Kicker("WHAT THE APPLICANT WROTE")
            Text(
                "page 2 of the application · in $whose own words",
                fontSize = 11.5.sp,
                color = Industry.caption,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        AnswerSummary(counts)
        positions.forEachIndexed { i, row -> AnswerCard(i + 1, row, gender) }
        if (cameFrom != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(min = 38.dp)
                    .background(Industry.neutral100, RoundedCornerShape(6.dp))
                    .border(1.dp, Industry.neutral200, RoundedCornerShape(6.dp))
                    .padding(horizontal = 14.dp)
                    .testTag("card-came-from"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "CAME FROM",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 1.4.sp,
                    color = Industry.caption,
                )
                Text(cameFrom, fontSize = 12.5.sp, color = Industry.neutral700, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnswerSummary(counts: org.dhamma.dipi.staff.model.HealthAnswerCounts) {
    val flagged = counts.recorded > 0
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .background(if (flagged) Industry.accent100 else Industry.neutral100, RoundedCornerShape(6.dp))
            .border(1.dp, if (flagged) Industry.accent300 else Industry.neutral200, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("answer-summary"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            healthSummaryText(counts),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (flagged) Industry.accent800 else Industry.neutral700,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AnswerCard(index: Int, row: HealthRow, gender: Gender) {
    val kind = healthAnswerKind(row, gender)
    val recorded = kind == HealthAnswerKind.RECORDED ||
        kind == HealthAnswerKind.EXPLICIT_YES ||
        kind == HealthAnswerKind.EXPLICIT_NO
    val tokens = LocalReadableTokens.current
    val shape = RoundedCornerShape(7.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(if (recorded) tokens.recordedFill else tokens.blankFill, shape)
            .border(1.dp, if (recorded) Industry.accent300 else Industry.neutral300, shape)
            .padding(horizontal = 15.dp, vertical = 13.dp)
            .testTag("answer-card-${row.label}"),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                index.toString(),
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 15.6.sp,
                color = tokens.caption,
                modifier = Modifier.width(20.dp),
            )
            Text(
                row.label,
                fontSize = if (recorded) 15.5.sp else 14.sp,
                fontWeight = if (recorded) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 18.2.sp,
                color = if (recorded) tokens.primary else tokens.secondary,
                modifier = Modifier.weight(1f),
            )
            AnswerTag(
                healthBadgeLabel(kind),
                accent = recorded,
                tag = "answer-badge-${row.label}",
            )
        }
        if (recorded) {
            val rule = Industry.accent500
            Text(
                row.answer,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 15.sp,
                lineHeight = 23.25.sp,
                color = tokens.bodyOnCard,
                modifier = Modifier
                    .padding(top = 9.dp, start = 32.dp)
                    .drawBehind {
                        drawRect(rule, size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                    }
                    .padding(start = 14.dp)
                    .testTag("answer-body-${row.label}"),
            )
        } else {
            Text(
                healthSourceCaption(row, kind),
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = tokens.caption,
                modifier = Modifier
                    .padding(top = 6.dp, start = 32.dp)
                    .testTag("answer-caption-${row.label}"),
            )
        }
    }
}

@Composable
private fun AnswerTag(text: String, accent: Boolean, tag: String? = null) {
    Box(
        Modifier
            .heightIn(min = 24.dp)
            .background(
                if (accent) Industry.accent200 else Industry.neutral200,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontFamily = DipiMono,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 10.5.sp,
            letterSpacing = 1.1.sp,
            color = if (accent) Industry.accent800 else Industry.neutral600,
        )
    }
}

@Composable
private fun Kicker(text: String) {
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 1.7.sp,
        color = Industry.caption,
    )
}
