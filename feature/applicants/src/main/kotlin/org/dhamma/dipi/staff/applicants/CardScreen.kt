package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.ui.ApplicantHistorySections
import org.dhamma.dipi.staff.ui.SeverityDot
import org.dhamma.dipi.staff.ui.StatusBadge
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun CardScreen(
    card: ApplicantCard,
    photoNote: String,
    dark: Boolean,
    onChangeStatus: () -> Unit,
    onPhoto: () -> Unit,
    sensitive: SensitiveInfo? = null,
    history: ApplicantDeskHistory? = null,
    onExpandHistory: (String) -> Unit = {},
    onOpenClarification: (Int) -> Unit = {},
) {
    val c = LocalDipi.current
    val uri = LocalUriHandler.current
    val hard = card.hardFlagCount > 0
    Column(
        Modifier.fillMaxSize().background(c.background).verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        Text(card.displayName, fontFamily = DipiCondensed, fontSize = 32.sp, color = c.foreground)
        Row {
            StatusBadge(card.status.value, dark = dark)
            Text("  ${card.confNo?.display() ?: "no conf no"}", fontFamily = DipiMono, color = c.muted)
        }
        Text("${card.age ?: "—"} ${card.gender.name}", color = c.muted, modifier = Modifier.padding(top = 4.dp))
        if (card.monk) Text("Monk/Nun", color = c.accent, fontFamily = DipiCondensed)
        TextButton(onPhoto) { Text(photoNote, color = c.accent) }
        val health = sensitive?.health.orEmpty()
        if (health.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth()
                    .border(1.dp, c.accent, RoundedCornerShape(8.dp))
                    .background(c.tint, RoundedCornerShape(8.dp))
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "HEALTH · VERIFY WITH APPLICANT",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.5.sp,
                    color = c.accent,
                )
                health.forEach { (label, text) ->
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = c.foreground)
                        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = c.foreground)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth()
                .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                "ID VERIFICATION",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.5.sp,
                letterSpacing = 1.5.sp,
                color = c.muted,
            )
            val idLabel = sensitive?.idLabel
            val idNumber = sensitive?.idNumber
            if (idLabel != null && idNumber != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(idLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.foreground)
                    Text(
                        idNumber,
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = c.foreground,
                    )
                }
            } else {
                Text("No ID on file", fontSize = 13.sp, color = c.muted)
            }
        }
        if (card.flags.isEmpty()) {
            Text("Audit clean", fontFamily = DipiCondensed, color = c.muted)
            Text("No audit flags. Identity, contact, emergency and cross-course checks all pass.", color = c.muted, fontSize = 13.sp)
        } else {
            Text(
                "Needs attention · ${card.flags.size}",
                fontFamily = DipiCondensed,
                color = if (hard) c.hard else c.foreground,
                fontSize = 18.sp,
            )
            card.flags.forEach { f ->
                Row(Modifier.padding(vertical = 6.dp)) {
                    SeverityDot(f.severity)
                    Column(Modifier.padding(start = 8.dp)) {
                        Text(f.label, color = c.foreground)
                        Text(f.detail, fontFamily = DipiMono, fontSize = 12.sp, color = c.muted)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Fact("Location", card.locationLine)
        Fact("Mobile", card.mobile ?: "—")
        Fact("Email", card.email ?: "—")
        Fact("Home phone", card.phoneHome ?: "—")
        Fact("Date of birth", card.dob ?: "—")
        Fact("Application date", card.createdAt ?: "—")
        if (card.oldStudent && card.history != null) {
            Spacer(Modifier.height(12.dp))
            Text("Courses completed · course audit", fontFamily = DipiCondensed, fontSize = 16.sp)
            Fact("First", card.history!!.first ?: "—")
            Fact("Most recent", card.history!!.recent ?: "—")
            Text(
                card.history!!.counts.joinToString(" · ") { "${it.label} ${it.n}" },
                color = c.muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        ApplicantHistorySections(
            history = history ?: ApplicantDeskHistory(),
            onExpand = onExpandHistory,
            onOpenClarification = onOpenClarification,
        )
        Spacer(Modifier.height(24.dp))
        Row {
            TextButton({ card.mobile?.let { uri.openUri("tel:${it.filter { ch -> ch.isDigit() || ch == '+' }}") } }) { Text("Call") }
            TextButton({ card.email?.let { uri.openUri("mailto:$it") } }) { Text("Email") }
            Button(onChangeStatus, Modifier.weight(1f)) { Text("Change status") }
        }
    }
}

@Composable
private fun Fact(k: String, v: String) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(k, color = c.muted, fontSize = 11.sp)
        Text(v, color = c.foreground)
    }
}
