package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.CentreFormSettings
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun CentreEditScreen(
    settings: CentreFormSettings?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Centre Settings", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            "Values from the desk edit form — display only.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }
        when {
            loading -> Text("Loading…", color = c.muted)
            error != null -> Text(error, color = c.foreground)
            settings == null -> Text("Could not read the form.", color = c.muted)
            else -> {
                Fact("Name", settings.name)
                Fact("Trust", settings.trust)
                Fact("Address", settings.address)
                Fact("City", listOf(settings.city, settings.state, settings.country, settings.pincode).filter { it.isNotBlank() }.joinToString(", "))
                Fact("Phone", settings.phone)
                Fact("Email", settings.email)
                Fact("Website", settings.website)
                Fact("Email from", settings.emailFrom)
                Fact("Reply to", settings.emailReplyTo)
                Fact("Preconfirmation", flag(settings.preconf, settings.preconfDays, "days"))
                Fact("Reconfirmation", flag(settings.reconf, settings.reconfDays, "days before start"))
                Fact("Expected reminder", flag(settings.expectedMail, settings.expectedDays, "days before start"))
                Fact("WhatsApp preconf", yn(settings.whatsappPreconf))
                Fact("WhatsApp reconf", yn(settings.whatsappReconf))
                Fact("WhatsApp on status change", yn(settings.whatsappMsg))
                if (settings.announcement.isNotBlank()) Fact("Announcement", settings.announcement)
            }
        }
    }
}

@Composable
private fun Fact(k: String, v: String) {
    val c = LocalDipi.current
    if (v.isBlank()) return
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(k, color = c.muted, fontSize = 11.sp)
        Text(v, color = c.foreground, fontSize = 15.sp)
    }
}

private fun yn(v: Boolean?): String = when (v) {
    true -> "Yes"
    false -> "No"
    null -> ""
}

private fun flag(on: Boolean?, days: String, unit: String): String = when (on) {
    true -> if (days.isNotBlank()) "Yes · $days $unit" else "Yes"
    false -> "No"
    null -> ""
}
