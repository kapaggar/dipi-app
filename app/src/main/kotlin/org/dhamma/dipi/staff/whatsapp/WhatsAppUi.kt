package org.dhamma.dipi.staff.whatsapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.ui.theme.DipiTheme

val LocalWhatsAppController = staticCompositionLocalOf<WhatsAppController?> { null }

@Composable
fun WhatsAppSettingsEntry() {
    val controller = LocalWhatsAppController.current ?: return
    val state by controller.ui.collectAsStateWithLifecycle()
    if (state.profile == null) return
    OutlinedButton(onClick = controller::openSettings, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("WhatsApp automation · ${if (state.profile?.enabled == true) "Enabled" else "Off"}")
    }
}

@Composable
fun WhatsAppDialogs(controller: WhatsAppController) {
    val state by controller.ui.collectAsStateWithLifecycle()
    if (state.panel == null) return
    DipiTheme(dark = false) {
        Dialog(onDismissRequest = controller::close, properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.running, dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn)) {
            Surface(Modifier.fillMaxWidth(.94f).fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (state.panel == "settings") "Centre WhatsApp automation" else "WhatsApp batch", style = MaterialTheme.typography.headlineSmall)
                        TextButton(onClick = controller::close, enabled = !state.running) { Text("Close") }
                    }
                    Text(state.message, color = MaterialTheme.colorScheme.primary)
                    if (state.panel == "settings") AutomationSettings(controller, state) else AutomationBatch(controller, state)
                }
            }
        }
    }
}

@Composable
private fun AutomationSettings(controller: WhatsAppController, state: WhatsAppUi) {
    val profile = state.profile ?: return
    var key by remember { mutableStateOf("") }
    var iv by remember { mutableStateOf("") }
    var ownPhone by remember { mutableStateOf("") }
    var removal by remember { mutableStateOf(false) }
    Text("Only this centre on this device · ${profile.scope.origin} · centre ${profile.scope.centreId}")
    Text("Experimental: WhatsApp updates can interrupt automation and automated sending can restrict your account. Keep the tablet unlocked and dedicated to the run.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WHATSAPP_PACKAGES.forEach { pkg ->
            FilterChip(selected = profile.packageName == pkg, enabled = !state.busy && !state.running,
                onClick = { controller.configure(false, pkg) }, label = { Text(if (pkg == "com.whatsapp") "WhatsApp" else "WhatsApp Business") })
        }
    }
    Text(if (state.configured) "Letter key is provisioned on this device." else "Provision the centre's shared letter key.")
    Text("A compromised tablet could expose this server-wide key. It is not included in the APK or backups. Enter the existing secret key and secret IV values, not their hashes.")
    OutlinedTextField(value = key, onValueChange = { key = it.take(4096) }, label = { Text("Secret key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(value = iv, onValueChange = { iv = it.take(4096) }, label = { Text("Secret IV") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = key.isNotBlank() && iv.isNotBlank() && !state.busy && !state.running, onClick = { controller.provision(key, iv); key = ""; iv = "" }) { Text("Store key on this tablet") }
    HorizontalDivider()
    Text("Device check", style = MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick = controller::accessibilitySettings) { Text("Enable DIPI WhatsApp accessibility service") }
    Text("The service reads the selected WhatsApp screen and clicks Send only during a started run. Pause and Stop stay available over WhatsApp. No unrelated chat history is stored.")
    OutlinedTextField(value = ownPhone, onValueChange = { ownPhone = it.take(24) }, label = { Text("This WhatsApp account's own number, with country code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = !state.busy && !state.running && automationPhone(ownPhone) != null, onClick = { controller.testSelf(ownPhone) }) { Text("Send labelled test to Message yourself") }
    Text("Tested WhatsApp version: ${profile.testedVersion ?: "Not tested"}. A WhatsApp update requires another test.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Switch(checked = profile.enabled, enabled = !state.busy && !state.running, onCheckedChange = { controller.configure(it, profile.packageName) })
        Text("Enable managed letters and batch sending for this centre")
    }
    TextButton(onClick = { removal = true }, enabled = !state.busy && !state.running) { Text("Remove this centre's key and automation settings") }
    if (removal) AlertDialog(onDismissRequest = { removal = false }, title = { Text("Remove automation settings?") }, text = { Text("The stored key, settings and local batch progress for this centre will be erased.") },
        confirmButton = { TextButton(onClick = { controller.removeProfile(); removal = false }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removal = false }) { Text("Cancel") } })
}

@Composable
private fun AutomationBatch(controller: WhatsAppController, state: WhatsAppUi) {
    var discard by remember { mutableStateOf(false) }
    val profile = state.profile ?: return
    if (!controller.ready()) {
        Text("Enable and test automation in Centre settings before starting a batch.")
        OutlinedButton(onClick = controller::openSettings) { Text("Open automation settings") }
    }
    val batch = state.batch
    if (batch != null) {
        Text("Saved batch · ${batch.attempts.count { it.state == WhatsAppAttemptState.SubmissionObserved }}/${batch.attempts.size} submissions observed", style = MaterialTheme.typography.titleMedium)
        Text("Submission observed does not mean delivered. An unknown outcome must be checked in WhatsApp and skipped here; it will never be retried automatically.")
        batch.attempts.forEach { attempt ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Application ${attempt.applicantId} · +${attempt.phone} · ${attempt.state}", modifier = Modifier.weight(1f))
                if (!state.running && attempt.state == WhatsAppAttemptState.Failed) TextButton(onClick = { controller.retryFailed(attempt.applicantId) }) { Text("Retry") }
                if (!state.running && attempt.state !in setOf(WhatsAppAttemptState.SubmissionObserved, WhatsAppAttemptState.Skipped)) TextButton(onClick = { controller.skip(attempt.applicantId) }) { Text(if (attempt.state == WhatsAppAttemptState.OutcomeUnknown) "Reviewed · skip" else "Skip") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (state.running) controller.pause() else controller.resume() }, enabled = !state.busy && (state.running || batch.attempts.any { it.state == WhatsAppAttemptState.Pending })) { Text(if (state.running) "Pause" else "Resume pending") }
            OutlinedButton(onClick = { controller.pause(); discard = true }) { Text("Stop / discard batch") }
        }
        if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this batch?") }, text = { Text("Local progress will be removed. Messages already submitted cannot be undone. Starting another batch may send them again.") },
            confirmButton = { TextButton(onClick = { controller.discard(); discard = false }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep progress") } })
        HorizontalDivider()
    }
    if (state.running) return
    Text("Recipients from the Calling filters", style = MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick = controller::selectAll, enabled = !state.busy) { Text("Select all valid numbers (${state.candidates.size})") }
    state.candidates.forEach { card ->
        val phone = automationPhone(card.mobile)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = card.id.value in state.selected, enabled = phone != null && !state.busy, onCheckedChange = { controller.select(card.id.value) })
            Text("${card.displayName} · ${phone?.let { "+$it" } ?: "Invalid/missing number"} · ${card.status.value}", modifier = Modifier.weight(1f))
        }
    }
    if (controller.duplicates()) Row {
        Checkbox(checked = state.duplicateConsent, onCheckedChange = controller::duplicateConsent, enabled = !state.busy)
        Text("Some selected applicants share a number. Send a separate personalised message for EACH selected applicant to that number.")
    }
    Text("Active managed letter", style = MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick = controller::refreshLetters, enabled = !state.busy) { Text("Refresh active letters") }
    state.letters.forEach { letter ->
        Row {
            RadioButton(selected = profile.letterId == letter.id, enabled = !state.busy, onClick = { controller.chooseLetter(letter.id) })
            Text("${letter.name} · ${letter.courseType} · ${letter.event} · #${letter.id}")
        }
    }
    Button(onClick = controller::preview, enabled = !state.busy && state.selected.isNotEmpty() && profile.letterId != null) { Text("Prepare personalised sample") }
    Text("Preparing letters can initialise missing applicant login credentials on the existing server. It does not change attendance or desk status.")
    state.preview?.let { preview ->
        HorizontalDivider()
        Text("Sample · application ${preview.applicantId}", style = MaterialTheme.typography.titleMedium)
        Text(preview.text)
        Button(onClick = controller::start, enabled = !state.busy && controller.ready()) { Text("Start sending to ${state.selected.size} selected applicants") }
    }
}
