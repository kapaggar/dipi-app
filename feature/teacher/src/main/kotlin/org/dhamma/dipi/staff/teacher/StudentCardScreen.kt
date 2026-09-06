package org.dhamma.dipi.staff.teacher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HealthRow
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.backrestSeatLabel
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

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
 * - Header 60dp: back, name + status chip, placement line, and the 48dp
 *   `‹ ›` pair walking the current group (disabled ends at 38% alpha — no
 *   drawn spec).
 * - Left 404dp FIXED: photo 132×158, the eight Personal rows verbatim, ten
 *   50dp history tiles in SERVER order (zeros stay), history meta.
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
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                LeftColumn(row, card, loadPhoto)
                RightColumn(card, group.gender, cameFrom)
            }
        }
    }
}

/* ── Header band 60dp ───────────────────────────────────────────────── */

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
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(start = 12.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .height(48.dp)
                .background(Color.White, RoundedCornerShape(6.dp))
                .border(1.dp, Industry.neutral300, RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(start = 10.dp, end = 14.dp)
                .testTag("card-back"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("‹", fontSize = 19.sp, color = Industry.neutral700)
            Text(backLabel, fontSize = 13.sp, color = Industry.neutral700)
        }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.name,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                    letterSpacing = 0.2.sp,
                    color = Industry.text,
                    maxLines = 1,
                )
                StatusChip(group, card)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                placementLine(row, group),
                fontSize = 12.5.sp,
                color = Industry.neutral600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("card-placement"),
            )
        }
        WalkButton("‹", enabled = canPrev, onClick = onPrev, tag = "card-prev")
        Spacer(Modifier.width(8.dp))
        WalkButton("›", enabled = canNext, onClick = onNext, tag = "card-next")
    }
}

/** `OLD · OM7` — roll seniority + the card's conf when present. */
@Composable
private fun StatusChip(group: RollGroup, card: ApplicationCard?) {
    val text = listOfNotNull(group.seniorityWord.uppercase(), card?.conf).joinToString(" · ")
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
        color = Industry.accent700,
        modifier = Modifier
            .padding(start = 10.dp)
            .background(Industry.accent100, RoundedCornerShape(3.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp)
            .testTag("card-status-chip"),
    )
}

/**
 * `Mbk-37 · seat E1 · Group 1 · TAM · 1 of 18 in this group` — roll facts
 * only. A backrest row's seat carries the shared [backrestSeatLabel] glyph.
 */
private fun placementLine(row: RollRow, group: RollGroup): String = listOfNotNull(
    row.room.takeIf { it.isNotBlank() },
    row.seat.takeIf { it.isNotBlank() }?.let { "seat ${backrestSeatLabel(it, row.backrest)}" },
    "Group ${group.group}",
    group.code,
    "${row.sn} of ${group.total} in this group",
).joinToString(" · ")

@Composable
private fun WalkButton(glyph: String, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .size(48.dp)
            .border(1.dp, if (enabled) Industry.neutral300 else Industry.neutral200, shape)
            .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 20.sp, color = if (enabled) Industry.neutral700 else Industry.neutral300)
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
                    color = Industry.neutral600,
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
                card.personal.forEach { (key, value) -> PersonalRow(key, value) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Kicker("COURSE HISTORY")
        Spacer(Modifier.height(8.dp))
        card.historyCounts.chunked(5).forEachIndexed { i, chunk ->
            if (i > 0) Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { (key, n) -> HistoryTile(key, n, Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        MetaRow("First Course", card.firstCourse)
        MetaRow("Last Course", card.lastCourse)
        MetaRow("Practice Details", card.practiceDetails)
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
                color = Industry.neutral500,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PersonalRow(key: String, value: String) {
    Row(
        Modifier.fillMaxWidth().height(22.5.dp).bottomHairline(RowHairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(key, fontSize = 12.sp, color = Industry.neutral500)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontFamily = DipiMono,
            fontSize = 12.5.sp,
            color = Industry.text,
            maxLines = 1,
        )
    }
}

/** 50dp count tile — zeros STAY: the shape of the history is the information. */
@Composable
private fun HistoryTile(key: String, n: Int, modifier: Modifier) {
    val shape = RoundedCornerShape(5.dp)
    val nonZero = n > 0
    Column(
        modifier
            .height(50.dp)
            .background(if (nonZero) Industry.accent100 else PaleFill, shape)
            .border(1.dp, if (nonZero) Industry.accent300 else ZeroTileBorder, shape)
            .testTag("history-tile-$key"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            n.toString(),
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = if (nonZero) Industry.accent700 else Industry.neutral400,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            key.uppercase(),
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 8.5.sp,
            letterSpacing = 0.9.sp,
            color = Industry.neutral500,
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
            .bottomHairline(RowHairline)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(key, fontSize = 12.sp, lineHeight = 15.6.sp, color = Industry.neutral500, modifier = Modifier.width(104.dp))
        Text(value, fontSize = 13.sp, lineHeight = 16.9.sp, color = Industry.text, modifier = Modifier.weight(1f))
    }
}

/* ── Right column — what the applicant wrote ────────────────────────── */

@Composable
private fun RightColumn(card: ApplicationCard, gender: Gender, cameFrom: String?) {
    val answered = card.health.count { healthAnswered(it, gender) }
    val names = card.health.filter { healthAnswered(it, gender) }.map { it.label }
    val whose = if (gender == Gender.F) "her" else "his"
    Column(
        Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .testTag("card-answers"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Column {
            Kicker("WHAT THE APPLICANT WROTE")
            Text(
                "page 2 of the application · in $whose own words",
                fontSize = 11.5.sp,
                color = Industry.neutral400,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        AnswerSummary(answered, names)
        card.health.forEachIndexed { i, row -> AnswerCard(i + 1, row, gender) }
        if (cameFrom != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .height(38.dp)
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
                    color = Industry.neutral500,
                )
                Text(cameFrom, fontSize = 12.5.sp, color = Industry.neutral700, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun healthAnswered(row: HealthRow, gender: Gender): Boolean {
    val notApplicable = row.label.equals("Pregnancy", ignoreCase = true) && gender == Gender.M
    if (notApplicable) return false
    if (row.label.equals("Pregnancy", ignoreCase = true) &&
        !row.answer.trim().startsWith("Yes", ignoreCase = true)
    ) return false
    return row.answered
}

@Composable
private fun AnswerSummary(answered: Int, names: List<String>) {
    val flagged = answered > 0
    val label = if (flagged) {
        names.joinToString(" and ") + " have something written"
    } else {
        "Nothing written on any of the six questions"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(if (flagged) Industry.accent100 else Industry.neutral100, RoundedCornerShape(6.dp))
            .border(1.dp, if (flagged) Industry.accent300 else Industry.neutral200, RoundedCornerShape(6.dp))
            .padding(horizontal = 14.dp)
            .testTag("answer-summary"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (flagged) Industry.accent800 else Industry.neutral700,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$answered OF 6 ANSWERED",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            color = if (flagged) Industry.accent700 else Industry.neutral500,
        )
    }
}

@Composable
private fun AnswerCard(index: Int, row: HealthRow, gender: Gender) {
    val notApplicable = row.label.equals("Pregnancy", ignoreCase = true) && gender == Gender.M
    val answered = healthAnswered(row, gender)
    val shape = RoundedCornerShape(7.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (!answered) Modifier.height(56.dp) else Modifier)
            .background(if (answered) Industry.accent100 else PaleFill, shape)
            .border(1.dp, if (answered) Industry.accent300 else ZeroTileBorder, shape)
            .padding(
                start = 14.dp,
                end = 14.dp,
                top = if (answered) 11.dp else 0.dp,
                bottom = if (answered) 13.dp else 0.dp,
            )
            .testTag("answer-card-${row.label}"),
        verticalArrangement = if (answered) Arrangement.Top else Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                index.toString(),
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 15.6.sp,
                color = if (answered) Industry.accent400 else Industry.neutral400,
                modifier = Modifier.width(20.dp),
            )
            // The question label, verbatim — the teacher must see what was asked.
            Text(
                row.label,
                fontSize = if (answered) 15.5.sp else 14.sp,
                fontWeight = if (answered) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 18.2.sp,
                color = if (answered) Industry.text else Industry.neutral700,
                modifier = Modifier.weight(1f),
            )
            AnswerTag(
                when {
                    notApplicable -> "N/A"
                    answered -> "YES"
                    else -> "NO"
                },
                accent = answered,
            )
        }
        if (answered) {
            // 2dp left rule + the answer, 14.5sp/1.5, NEVER truncated — the
            // largest body type on any screen in the app, on purpose. The
            // rule is drawn, not laid out, so nothing can constrain the
            // text's height.
            val rule = Industry.accent500
            Text(
                row.answer,
                fontSize = 14.5.sp,
                lineHeight = 21.75.sp,
                color = Industry.text,
                modifier = Modifier
                    .padding(top = 9.dp, start = 32.dp)
                    .drawBehind {
                        drawRect(rule, size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
                    }
                    .padding(start = 14.dp)
                    .testTag("answer-body-${row.label}"),
            )
        }
    }
}

@Composable
private fun AnswerTag(text: String, accent: Boolean) {
    Box(
        Modifier
            .height(24.dp)
            .background(
                if (accent) Industry.accent200 else Industry.neutral200,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp),
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
        color = Industry.neutral500,
    )
}
