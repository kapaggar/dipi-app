package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.statusColors

@Composable
fun StatusBadge(label: String, modifier: Modifier = Modifier, dark: Boolean) {
    val tone = org.dhamma.dipi.staff.model.ApplicantStatus(label).tone
    val (bg, fg) = statusColors(tone, dark)
    Text(
        text = label,
        color = fg,
        fontSize = 11.sp,
        fontFamily = DipiCondensed,
        modifier = modifier
            .clip(DeskStyle.pillShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun ApplicantRow(
    card: ApplicantCard,
    queuedTo: String?,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalDipi.current
    val tinted = queuedTo != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (tinted) c.tint else c.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(card.displayName, color = c.foreground, fontSize = 16.sp, fontFamily = DipiCondensed)
            Text(
                card.metaLine + if (queuedTo != null) "  queued: → $queuedTo" else "",
                color = c.muted,
                fontSize = 12.sp,
            )
            if (card.flags.isNotEmpty()) {
                val hard = card.hardFlagCount > 0
                Text(
                    if (hard) "△ ${card.hardFlagCount} to fix" else "△ ${card.flags.size} flag",
                    color = if (hard) c.flagHard else c.flagSoft,
                    fontSize = 12.sp,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(card.confNo?.display() ?: "—", fontFamily = DipiMono, fontSize = 13.sp, color = c.foreground)
            StatusBadge(card.status.value, dark = dark)
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    val bg = if (selected) c.accent else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) androidx.compose.ui.graphics.Color.White else c.foreground
    Text(
        text = label,
        color = fg,
        fontFamily = DipiCondensed,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(DeskStyle.controlShape)
            .border(1.dp, if (selected) c.accent else c.hairlineStrong, DeskStyle.controlShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
fun SeverityDot(sev: AuditSeverity) {
    val c = LocalDipi.current
    val color = when (sev) {
        AuditSeverity.HARD -> c.hard
        AuditSeverity.SAFETY -> c.safety
        AuditSeverity.SOFT -> c.soft
    }
    Box(
        Modifier
            .clip(CircleShape)
            .background(color)
            .padding(6.dp),
    )
}
