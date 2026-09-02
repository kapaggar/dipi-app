package org.dhamma.dipi.staff.desk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.DaySummary
import org.dhamma.dipi.staff.model.OldNew
import org.dhamma.dipi.staff.model.RollMatrix
import org.dhamma.dipi.staff.model.DayRollRow
import org.dhamma.dipi.staff.model.SpecialRow
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Day 0 summary as a native surface (v5 frame `5d`).
 *
 * The desk serves this as an unstyled `#day-summary` fragment, so in the
 * sheet viewer it was the one Board cell that rendered as browser-default
 * HTML. It is a handful of numbers with one question behind them — *how many
 * of the confirmed roll have actually walked in* — so the headline states
 * that gap, the matrix reuses the centre dashboard's idiom exactly, and the
 * facilities card reads as the instruction it is.
 *
 * Everything here is a count. No student data reaches this screen.
 */
@Composable
fun DaySummaryPane(
    summary: DaySummary,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp, vertical = 22.dp)
            .testTag("day-summary-pane"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HeadlineCard(summary)
        RollMatrixCard("CONFIRMED", summary.confirmed, "day-summary-confirmed")
        RollMatrixCard("ATTENDED", summary.attended, "day-summary-attended")
        FacilitiesCard(summary)
    }
}

/**
 * `CONFIRMED 81 → ATTENDED 1`, then the derived gap past a divider. The gap
 * is arithmetic on two numbers we already have, not a third fetch — and it
 * is the number the registrar is actually looking for at 09:00.
 */
@Composable
private fun HeadlineCard(summary: DaySummary) {
    val confirmed = summary.confirmed.total.total
    val attended = summary.attended.total.total
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(shape = DeskStyle.cardShape, elevation = 1.dp)
            .padding(horizontal = 22.dp, vertical = 18.dp)
            .testTag("day-summary-headline"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            HeadlineFigure("CONFIRMED", confirmed)
            Text(
                "→",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                color = Industry.neutral400,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            HeadlineFigure("ATTENDED", attended)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Industry.neutral300))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "STILL TO ARRIVE",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 0.17.em,
                    color = Industry.neutral600,
                )
                Text(
                    "${summary.stillToArrive}",
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 32.sp,
                    color = Industry.text,
                    modifier = Modifier.testTag("day-summary-still-to-arrive"),
                )
            }
            Text(
                "The gap between the two numbers above. Every one of them is " +
                    "expected today and has not checked in yet.",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Industry.neutral600,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeadlineFigure(label: String, value: Int) {
    Column {
        Text(
            label,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.17.em,
            color = Industry.neutral600,
        )
        Text(
            "$value",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = 62.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.02).em,
            color = if (value == 0) Industry.neutral400 else Industry.accent800,
            modifier = Modifier.testTag("day-summary-figure-$label"),
        )
    }
}

/**
 * The centre dashboard's matrix idiom, unchanged: mono group caps, mono
 * column labels, a band behind `TOTAL`, and `SEVAK` outside the total in
 * `neutral600` because it is not part of the roll.
 */
@Composable
private fun RollMatrixCard(title: String, matrix: RollMatrix, tag: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(shape = DeskStyle.cardShape, elevation = 0.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag(tag),
    ) {
        Text(
            title,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.17.em,
            color = Industry.neutral600,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1.6f))
            listOf("OLD", "NEW", "TOTAL", "SEVAK").forEach { col ->
                MatrixColumnLabel(col, Modifier.weight(1f))
            }
        }
        MatrixRow("Male", matrix.male)
        MatrixRow("Female", matrix.female)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Industry.neutral300))
        MatrixRow("Total", matrix.total, emphasised = true)
    }
}

@Composable
private fun MatrixColumnLabel(text: String, modifier: Modifier) {
    Box(
        modifier
            .height(24.dp)
            // The TOTAL column carries the band that follows it down the rows.
            .background(if (text == "TOTAL") Industry.neutral200 else androidx.compose.ui.graphics.Color.Transparent),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            letterSpacing = 0.1.em,
            // SEVAK sits outside the total, and reads that way.
            color = if (text == "SEVAK") Industry.neutral600 else Industry.neutral500,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun MatrixRow(label: String, row: DayRollRow, emphasised: Boolean = false) {
    val height = if (emphasised) 48.dp else 44.dp
    Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = if (emphasised) 15.sp else 14.sp,
            fontWeight = if (emphasised) FontWeight.Medium else FontWeight.Normal,
            color = Industry.text,
            modifier = Modifier.weight(1.6f),
        )
        MatrixFigure(row.old, Modifier.weight(1f), height)
        MatrixFigure(row.new, Modifier.weight(1f), height)
        MatrixFigure(row.total, Modifier.weight(1f), height, banded = true, bold = emphasised)
        MatrixFigure(row.server, Modifier.weight(1f), height, muted = true)
    }
}

/** Zeros read calm — `neutral400`, never dashed, never hidden. */
@Composable
private fun MatrixFigure(
    value: Int,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    banded: Boolean = false,
    bold: Boolean = false,
    muted: Boolean = false,
) {
    Box(
        modifier
            .height(height)
            .background(if (banded) Industry.neutral200 else androidx.compose.ui.graphics.Color.Transparent),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            "$value",
            fontFamily = DipiMono,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = if (bold) 17.sp else 15.sp,
            color = when {
                value == 0 -> Industry.neutral400
                muted -> Industry.neutral600
                else -> Industry.text
            },
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

/**
 * Not a roll count — an instruction to the hall team: put out this many low
 * seats, chairs and backrests. `0 (O) + 0 (N)` becomes two mono figures
 * under one header, and an empty grid says so in words so day −1 does not
 * read as a failed fetch.
 */
@Composable
private fun FacilitiesCard(summary: DaySummary) {
    Column(
        Modifier
            .fillMaxWidth()
            .deskCard(shape = DeskStyle.cardShape, elevation = 0.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("day-summary-facilities"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "PUT OUT IN THE HALL",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.17.em,
            color = Industry.neutral600,
        )
        listOf(
            "Male" to summary.specialSeating.male,
            "Female" to summary.specialSeating.female,
            "Total" to summary.specialSeating.total,
        ).forEach { (label, row) ->
            FacilitiesRow(label, row, emphasised = label == "Total")
        }
        if (summary.specialSeating.isEmpty) {
            Text(
                "Nobody has asked for a low seat, a chair or a backrest. " +
                    "The grid is empty on purpose — there is nothing to put out.",
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                color = Industry.neutral600,
                modifier = Modifier.testTag("day-summary-facilities-empty"),
            )
        }
    }
}

@Composable
private fun FacilitiesRow(label: String, row: SpecialRow, emphasised: Boolean) {
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (emphasised) FontWeight.Medium else FontWeight.Normal,
            color = Industry.text,
            modifier = Modifier.weight(1.2f),
        )
        listOf("CHOWKY" to row.chowky, "CHAIR" to row.chair, "BACKREST" to row.backrest)
            .forEach { (header, pair) ->
                FacilitiesCell(header, pair, Modifier.weight(1f))
            }
    }
}

@Composable
private fun FacilitiesCell(header: String, pair: OldNew, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            header,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.14.em,
            color = Industry.neutral500,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FacilitiesFigure("O", pair.old)
            FacilitiesFigure("N", pair.new)
        }
    }
}

@Composable
private fun FacilitiesFigure(marker: String, value: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            "$value",
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 18.sp,
            color = if (value == 0) Industry.neutral400 else Industry.text,
        )
        Text(
            marker,
            fontFamily = DipiMono,
            fontSize = 9.sp,
            lineHeight = 12.sp,
            color = Industry.neutral400,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}