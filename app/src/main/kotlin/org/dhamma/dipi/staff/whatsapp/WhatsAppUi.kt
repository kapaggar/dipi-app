package org.dhamma.dipi.staff.whatsapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
            decorFitsSystemWindows = false, dismissOnBackPress = !state.running, dismissOnClickOutside = false, securePolicy = SecureFlagPolicy.SecureOn)) {
            Surface(Modifier.imePadding().fillMaxWidth(.94f).fillMaxHeight(.94f), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (state.panel == "settings") "Centre WhatsApp automation" else "WhatsApp batch", style = MaterialTheme.typography.headlineSmall)
                        Row {
                            if (state.running) {
                                TextButton(onClick = { controller.pause() }) { Text("Pause") }
                                TextButton(onClick = { controller.pause("Stopped. Review saved progress before resuming.") }) { Text("Stop") }
                            }
                            TextButton(onClick = controller::close, enabled = !state.running) { Text("Close") }
                        }
                    }
                    Text(state.message, color = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.panel == "settings") AutomationSettings(controller, state) else AutomationBatch(controller, state)
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomationSettings(controller: WhatsAppController, state: WhatsAppUi) {
    val profile = state.profile ?: return
    var code by remember { mutableStateOf("") }
    var ownPhone by remember { mutableStateOf("") }
    var removal by remember { mutableStateOf(false) }
    val tested = profile.testedVersion != null && profile.testedVersion == controller.packageVersion()
    var setupExpanded by rememberSaveable(profile.scope) { mutableStateOf(!state.configured || !tested) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text("WhatsApp automation", style = MaterialTheme.typography.titleLarge)
            Text(if (profile.enabled) "On for this centre and tablet" else "Off for this centre and tablet")
        }
        Switch(checked = profile.enabled, enabled = !state.busy && !state.running && (profile.enabled || state.configured && tested && state.accessibilityReady),
            onCheckedChange = { controller.configure(it, profile.packageName) })
    }
    Text("Keep this tablet unlocked during a run. Messages are sent only after you review a batch and tap Start.")
    Text("${if (state.configured) "✓" else "1."} Provisioning code ${if (state.configured) "saved" else "needed"}   ·   ${if (state.accessibilityReady) "✓ Accessibility on" else "2. Accessibility needed"}   ·   ${if (tested) "✓ Device test passed" else "3. Device test needed"}")
    if (!state.accessibilityReady) OutlinedButton(onClick = controller::accessibilitySettings) { Text("Turn on Android permission") }
    TextButton(onClick = { setupExpanded = !setupExpanded }) { Text(if (setupExpanded) "Hide setup" else "Setup and device check") }
    if (setupExpanded) {
        HorizontalDivider()
        Text("1 · Connect this tablet", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WHATSAPP_PACKAGES.forEach { pkg ->
                FilterChip(selected = profile.packageName == pkg, enabled = !state.busy && !state.running,
                    onClick = { controller.configure(false, pkg) }, label = { Text(if (pkg == "com.whatsapp") "WhatsApp" else "WhatsApp Business") })
            }
        }
        Text(if (state.configured) "Code saved securely. Enter a new code only to replace it." else "Paste the code supplied by your centre administrator.")
        OutlinedTextField(value = code, onValueChange = { code = it.take(256) }, label = { Text("Provisioning code") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = code.isNotBlank() && !state.busy && !state.running, onClick = { controller.provision(code); code = "" }) { Text("Save code") }
        Text("2 · Allow WhatsApp control", style = MaterialTheme.typography.titleMedium)
        Text("In Android settings, turn on DIPI WhatsApp automation, then return here.")
        OutlinedButton(onClick = controller::accessibilitySettings) { Text(if (state.accessibilityReady) "Android permission · On" else "Open Android permission") }
        Text("3 · Test your own chat", style = MaterialTheme.typography.titleMedium)
        Text(if (tested) "Passed for WhatsApp ${profile.testedVersion}. Test again after WhatsApp updates." else "A labelled test checks your own chat. No applicant receives this test.")
        OutlinedTextField(value = ownPhone, onValueChange = { ownPhone = it.take(24) }, label = { Text("Your WhatsApp number, including country code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = !state.busy && !state.running && state.accessibilityReady && automationPhone(ownPhone) != null, onClick = { controller.testSelf(ownPhone) }) { Text("Test my WhatsApp") }
        controller.pilotResult()?.let { Text("Last test: $it") }
        Text("Experimental automation can break after WhatsApp updates and can restrict your WhatsApp account. Share the provisioning code only with eligible admins; it grants shared-key access.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { removal = true }, enabled = !state.busy && !state.running) { Text("Remove this tablet’s setup") }
    }
    if (removal) AlertDialog(onDismissRequest = { removal = false }, title = { Text("Remove automation settings?") }, text = { Text("The stored key, settings and local batch progress for this centre will be erased.") },
        confirmButton = { TextButton(onClick = { controller.removeProfile(); removal = false }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removal = false }) { Text("Cancel") } })
}

@Composable
private fun AutomationBatch(controller: WhatsAppController, state: WhatsAppUi) {
    var discard by remember { mutableStateOf(false) }
    var lettersExpanded by rememberSaveable { mutableStateOf(false) }
    val profile = state.profile ?: return
    if (!controller.ready()) {
        Text("Enable and test automation in Centre settings before starting a batch.")
        OutlinedButton(onClick = controller::openSettings) { Text("Open automation settings") }
    }
    val batch = state.batch
    if (batch != null) {
        Text(batchProgressTitle(batch, state.running), style = MaterialTheme.typography.titleLarge)
        LinearProgressIndicator(progress = { batch.attempts.count { it.state in setOf(WhatsAppAttemptState.SubmissionObserved, WhatsAppAttemptState.Skipped) }.toFloat() / batch.attempts.size }, modifier = Modifier.fillMaxWidth())
        Text("${batch.attempts.count { it.state == WhatsAppAttemptState.SubmissionObserved }} submitted · ${batch.attempts.count { it.state == WhatsAppAttemptState.Skipped }} skipped · ${batch.attempts.count { it.state == WhatsAppAttemptState.OutcomeUnknown }} need checking")
        Text("Submission observed does not mean delivered. An unknown outcome must be checked in WhatsApp and skipped here; it will never be retried automatically.")
        batch.attempts.forEach { attempt ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${state.candidates.firstOrNull { it.id.value == attempt.applicantId }?.displayName ?: "Application ${attempt.applicantId}"} · +${attempt.phone} · ${attemptLabel(attempt.state)}", modifier = Modifier.weight(1f))
                if (!state.running && attempt.state == WhatsAppAttemptState.Failed) TextButton(onClick = { controller.retryFailed(attempt.applicantId) }) { Text("Retry") }
                if (!state.running && attempt.state !in setOf(WhatsAppAttemptState.SubmissionObserved, WhatsAppAttemptState.Skipped)) TextButton(onClick = { controller.skip(attempt.applicantId) }) { Text(if (attempt.state == WhatsAppAttemptState.OutcomeUnknown) "Reviewed · skip" else "Skip") }
            }
        }
        if (batchComplete(batch)) {
            Button(onClick = controller::discard) { Text("Done · prepare another batch") }
        } else Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (state.running) controller.pause() else controller.resume() }, enabled = !state.busy && (state.running || batch.attempts.any { it.state == WhatsAppAttemptState.Pending })) { Text(if (state.running) "Pause" else "Resume pending") }
            OutlinedButton(onClick = { controller.pause(); discard = true }) { Text("Stop / discard batch") }
        }
        if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this batch?") }, text = { Text("Local progress will be removed. Messages already submitted cannot be undone. Starting another batch may send them again.") },
            confirmButton = { TextButton(onClick = { controller.discard(); discard = false }) { Text("Discard") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep progress") } })
        return
    }
    if (state.running) return
    Text("WhatsApp must display the recipient’s full phone number. Saved-name-only chats stop for manual review.")
    Text("1 · Recipients (${state.selected.size} selected)", style = MaterialTheme.typography.titleLarge)
    Text("This list follows your Calling filters. Select who should receive the message.")
    OutlinedButton(onClick = controller::selectAll, enabled = !state.busy) { Text(if (state.selected.isNotEmpty()) "Clear selection" else "Select all valid numbers") }
    Column(Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
    state.candidates.forEach { card ->
        val phone = automationPhone(card.mobile)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(checked = card.id.value in state.selected, enabled = phone != null && !state.busy, onCheckedChange = { controller.select(card.id.value) })
            Text("${card.displayName} · ${phone?.let { "+$it" } ?: "Invalid/missing number"} · ${card.status.value}", modifier = Modifier.weight(1f))
        }
    }
    }
    if (controller.duplicates()) Row {
        Checkbox(checked = state.duplicateConsent, onCheckedChange = controller::duplicateConsent, enabled = !state.busy)
        Text("Some selected applicants share a number. Send a separate personalised message for EACH selected applicant to that number.")
    }
    HorizontalDivider()
    Text("2 · Message", style = MaterialTheme.typography.titleLarge)
    val selectedLetter = state.letters.firstOrNull { it.id == profile.letterId }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(selectedLetter?.name ?: profile.letterId?.let { "Saved letter #$it" } ?: "Choose a letter to continue", modifier = Modifier.weight(1f))
        TextButton(onClick = { lettersExpanded = !lettersExpanded }) { Text(if (lettersExpanded) "Close letters" else if (profile.letterId == null) "Choose letter" else "Change letter") }
    }
    Text("Your letter choice is remembered for this centre.", style = MaterialTheme.typography.bodySmall)
    if (lettersExpanded || profile.letterId == null) {
        OutlinedButton(onClick = controller::refreshLetters, enabled = !state.busy) { Text("Refresh letters") }
        Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
            state.letters.forEach { letter ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = profile.letterId == letter.id, enabled = !state.busy, onClick = { controller.chooseLetter(letter.id); lettersExpanded = false })
                    Text("${letter.name} · ${letter.courseType} · ${letter.event} · #${letter.id}")
                }
            }
        }
    }
    Button(onClick = controller::preview, enabled = !state.busy && state.selected.isNotEmpty() && selectedLetter != null && (!controller.duplicates() || state.duplicateConsent)) { Text(if (state.busy) "Preparing preview…" else "Preview message") }
    Text("Previewing may initialise missing applicant login details on the existing server.", style = MaterialTheme.typography.bodySmall)
    state.preview?.let { preview ->
        HorizontalDivider()
        Text("3 · Review and send", style = MaterialTheme.typography.titleLarge)
        Text("Sample for ${state.candidates.firstOrNull { it.id.value == preview.applicantId }?.displayName ?: "application ${preview.applicantId}"}. Each recipient gets their own personalised letter.")
        Text(preview.text)
        Button(onClick = controller::start, enabled = !state.busy && controller.ready()) { Text("Start sending to ${state.selected.size} selected applicants") }
    }
}

internal fun batchComplete(batch: WhatsAppBatch): Boolean = batch.attempts.all {
    it.state == WhatsAppAttemptState.SubmissionObserved || it.state == WhatsAppAttemptState.Skipped
}

internal fun batchProgressTitle(batch: WhatsAppBatch, running: Boolean): String = when {
    batchComplete(batch) -> "Batch complete"
    running -> "Sending · ${batch.attempts.count { it.state == WhatsAppAttemptState.SubmissionObserved }} of ${batch.attempts.size} submitted"
    else -> "Batch paused · review progress"
}

internal fun attemptLabel(state: WhatsAppAttemptState): String = when (state) {
    WhatsAppAttemptState.Pending -> "Waiting"
    WhatsAppAttemptState.Preparing -> "Preparing letter"
    WhatsAppAttemptState.Opening -> "Opening WhatsApp"
    WhatsAppAttemptState.SendStarted -> "Checking submission"
    WhatsAppAttemptState.SubmissionObserved -> "Submission observed"
    WhatsAppAttemptState.OutcomeUnknown -> "Outcome unknown · check WhatsApp"
    WhatsAppAttemptState.Failed -> "Not submitted · review"
    WhatsAppAttemptState.Skipped -> "Skipped"
}
