package org.dhamma.dipi.staff.whatsapp

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.dhamma.dipi.staff.BuildConfig
import org.dhamma.dipi.staff.datastore.WhatsAppStore
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.network.LetterLinkCipher
import org.dhamma.dipi.staff.network.ManagedLetterGateway
import org.dhamma.dipi.staff.ui.DeskUiState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// No personalised text is saved by this UI state or the controller.
data class WhatsAppUi(
    val profile: CentreWhatsAppProfile? = null,
    val configured: Boolean = false,
    val panel: String? = null,
    val candidates: List<ApplicantCard> = emptyList(),
    val selected: Set<Int> = emptySet(),
    val letters: List<ManagedLetter> = emptyList(),
    val preview: RenderedLetter? = null,
    val batch: WhatsAppBatch? = null,
    val busy: Boolean = false,
    val running: Boolean = false,
    val duplicateConsent: Boolean = false,
    val message: String = "",
)

@Singleton
class WhatsAppController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: WhatsAppStore,
    private val gateway: ManagedLetterGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutable = MutableStateFlow(WhatsAppUi())
    val ui = mutable.asStateFlow()
    private var job: Job? = null
    private var session: Triple<Int, WhatsAppScope, Int?>? = null
    private var roll: List<ApplicantCard> = emptyList()
    private var offline = false
    private var epoch = 0

    fun bind(state: DeskUiState) {
        val centre = state.session?.centres?.firstOrNull()?.id?.value
        val next = if (centre != null && !state.session!!.modeTest && context.resources.configuration.smallestScreenWidthDp >= 600)
            Triple(state.session.uid, WhatsAppScope(BuildConfig.BASE_URL.trimEnd('/'), centre), state.course?.id?.value) else null
        if (next != session) {
            endSession()
            session = next
            if (next != null) {
                val profile = store.profile(next.second)
                mutable.value = WhatsAppUi(profile = profile, configured = store.configured(next.second),
                    batch = store.batch(next.second)?.takeIf { it.courseId == next.third })
            }
        }
        roll = state.rows
        offline = state.offline
        if (offline && mutable.value.running) pause("Connection lost. Review progress before resuming.")
    }

    fun endSession() {
        pause("Paused because the desk session changed.")
        session = null
        roll = emptyList()
        mutable.value = WhatsAppUi()
    }
    fun erase() { endSession(); store.wipeAll() }
    fun openSettings() { mutable.value = mutable.value.copy(panel = "settings", message = "") }
    fun close() { if (!mutable.value.running) { job?.cancel(); epoch++; mutable.value = mutable.value.copy(panel = null, preview = null, busy = false) } }
    fun accessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun packageVersion(): String? = runCatching {
        context.packageManager.getPackageInfo(mutable.value.profile?.packageName ?: "com.whatsapp", 0).versionName
    }.getOrNull()
    fun ready(): Boolean = mutable.value.profile?.let {
        it.enabled && mutable.value.configured && it.testedVersion != null && it.testedVersion == packageVersion()
    } == true

    fun configure(enabled: Boolean, packageName: String) {
        pause("Settings changed.")
        val current = mutable.value.profile ?: return
        if (enabled && (!mutable.value.configured || current.testedVersion != packageVersion())) {
            message("Provision the letter key and pass the self-test before enabling automation."); return
        }
        val next = current.copy(enabled = enabled, packageName = packageName,
            testedVersion = current.testedVersion.takeIf { packageName == current.packageName })
        store.save(next)
        mutable.value = mutable.value.copy(profile = next, preview = null, message = "Settings saved for this centre.")
    }
    fun provision(key: String, iv: String) {
        val current = mutable.value.profile ?: return
        safe {
            store.provision(current.scope, key, iv)
            mutable.value = mutable.value.copy(configured = true, message = "Letter key stored on this device.")
        }
    }
    fun removeProfile() {
        val current = mutable.value.profile ?: return
        pause("Settings removed."); store.remove(current.scope)
        mutable.value = WhatsAppUi(profile = CentreWhatsAppProfile(current.scope), panel = "settings")
    }
    fun openBatch(candidates: List<ApplicantCard>) {
        if (mutable.value.running) { mutable.value = mutable.value.copy(panel = "batch"); return }
        val current = session ?: return
        val eligible = candidates.filter { it.centreId.value == current.second.centreId && it.courseId.value == current.third && it.status.value in setOf("Expected", "Confirmed") }
        mutable.value = mutable.value.copy(panel = "batch", candidates = eligible, selected = emptySet(), preview = null, duplicateConsent = false, message = "Choose recipients and one active letter.")
        refreshLetters()
    }
    fun offerSingle(card: ApplicantCard): Boolean {
        if (mutable.value.profile?.enabled != true) return false
        openBatch(listOf(card))
        if (mutable.value.candidates.any { it.id == card.id }) mutable.value = mutable.value.copy(selected = setOf(card.id.value))
        return true
    }
    fun select(id: Int) {
        if (mutable.value.busy || mutable.value.running) return
        val selected = mutable.value.selected.toMutableSet()
        if (!selected.add(id)) selected.remove(id)
        mutable.value = mutable.value.copy(selected = selected, preview = null, duplicateConsent = false)
    }
    fun selectAll() {
        if (mutable.value.busy || mutable.value.running) return
        mutable.value = mutable.value.copy(selected = mutable.value.candidates.filter { automationPhone(it.mobile) != null }.map { it.id.value }.toSet(), preview = null, duplicateConsent = false)
    }
    fun chooseLetter(id: Int) {
        if (mutable.value.busy || mutable.value.running) return
        val profile = mutable.value.profile ?: return
        if (mutable.value.letters.none { it.id == id }) return
        store.save(profile.copy(letterId = id))
        mutable.value = mutable.value.copy(profile = profile.copy(letterId = id), preview = null)
    }
    fun duplicateConsent(value: Boolean) { mutable.value = mutable.value.copy(duplicateConsent = value, preview = null) }
    fun duplicates(): Boolean {
        val phones = selectedCards().mapNotNull { automationPhone(it.mobile) }
        return phones.distinct().size != phones.size
    }
    private fun selectedCards() = mutable.value.candidates.filter { it.id.value in mutable.value.selected }

    fun refreshLetters() = task {
        val profile = mutable.value.profile ?: return@task
        val letters = gateway.letters(profile.scope)
        mutable.value = mutable.value.copy(letters = letters, message = "${letters.size} active letters loaded.")
    }
    fun preview() = task {
        check(!offline) { "Connect to the desk before preparing messages" }
        check(ready()) { "Enable and test WhatsApp automation in Centre settings first" }
        val cards = selectedCards()
        check(cards.isNotEmpty()) { "Choose at least one recipient" }
        check(cards.all { automationPhone(it.mobile) != null }) { "A selected phone number is invalid" }
        check(!duplicates() || mutable.value.duplicateConsent) { "Resolve the shared phone numbers first" }
        val letter = mutable.value.profile?.letterId ?: error("Choose a letter")
        val rendered = render(cards.first().id.value, letter)
        mutable.value = mutable.value.copy(preview = rendered, message = "Review the sample and all selected recipients before starting.")
    }
    private suspend fun render(applicant: Int, letter: Int): RenderedLetter {
        val current = session ?: error("Sign in again")
        val card = roll.firstOrNull { it.id.value == applicant } ?: error("Applicant is no longer in this course")
        check(card.centreId.value == current.second.centreId && card.courseId.value == current.third && card.status.value in setOf("Confirmed", "Expected")) { "Applicant eligibility changed; refresh the batch" }
        val token = store.withSecrets(current.second) { key, iv -> LetterLinkCipher.encrypt(applicant, letter, key, iv) }
        val result = gateway.render(current.second, applicant, letter, token)
        currentCoroutineContext().ensureActive()
        check(session == current) { "Centre session changed" }
        return result
    }
    fun start() {
        if (mutable.value.preview == null || mutable.value.busy || mutable.value.running) return
        safe {
            val current = session ?: error("Sign in again")
            val previous = mutable.value.batch
            check(previous == null || previous.attempts.all { it.state in setOf(WhatsAppAttemptState.SubmissionObserved, WhatsAppAttemptState.Skipped) }) { "Finish or discard the previous batch before creating another" }
            val cards = selectedCards()
            val batch = WhatsAppBatch(current.second, current.third ?: error("Open a course"), mutable.value.profile?.letterId ?: error("Choose a letter"),
                cards.map { WhatsAppAttempt(it.id.value, automationPhone(it.mobile) ?: error("Invalid number")) }, duplicatesConfirmed = mutable.value.duplicateConsent)
            store.saveBatch(batch)
            mutable.value = mutable.value.copy(batch = batch, preview = null)
            resume()
        }
    }
    fun resume() {
        if (mutable.value.running || mutable.value.busy) return
        safe {
            check(ready() && !offline) { "Check the connection, key and WhatsApp self-test in Centre settings" }
            check(WhatsAppAccessibilityService.connected != null) { "Enable the DIPI WhatsApp accessibility service" }
            val batch = mutable.value.batch ?: error("No saved batch")
            check(batch.attempts.none { it.state in setOf(WhatsAppAttemptState.OutcomeUnknown, WhatsAppAttemptState.Failed) }) { "Review uncertain or failed attempts before continuing" }
            val mine = ++epoch
            job = scope.launch {
                mutable.value = mutable.value.copy(running = true, preview = null)
                try {
                    for (attempt in batch.attempts.filter { it.state == WhatsAppAttemptState.Pending }) {
                        currentCoroutineContext().ensureActive()
                        val currentCard = roll.firstOrNull { it.id.value == attempt.applicantId }
                        check(automationPhone(currentCard?.mobile) == attempt.phone) { "Recipient number changed; stop and create a new batch" }
                        mark(attempt.applicantId, WhatsAppAttemptState.Preparing)
                        val letter = render(attempt.applicantId, batch.letterId)
                        mark(attempt.applicantId, WhatsAppAttemptState.Opening)
                        val result = WhatsAppAccessibilityService.connected?.send(mutable.value.profile!!.packageName, attempt.phone, letter.text, false) {
                            mark(attempt.applicantId, WhatsAppAttemptState.SendStarted)
                        } ?: error("Accessibility service disconnected")
                        currentCoroutineContext().ensureActive()
                        mark(attempt.applicantId, result.state)
                        check(result.state == WhatsAppAttemptState.SubmissionObserved) { result.reason }
                        delay(1500) // Give the UI time to settle; never parallelise recipient operations.
                    }
                    mutable.value = mutable.value.copy(message = "Batch finished. Submission observed is not delivery confirmation.")
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { message(e.message ?: "Batch paused") }
                finally {
                    if (mine == epoch) {
                        interruptBatch()
                        mutable.value = mutable.value.copy(running = false)
                        WhatsAppAccessibilityService.connected?.clear()
                    }
                }
            }
        }
    }
    fun pause(reason: String = "Paused. Review progress before resuming.") {
        epoch++
        WhatsAppAccessibilityService.connected?.abort(reason)
        job?.cancel(); job = null
        interruptBatch()
        mutable.value = mutable.value.copy(running = false, busy = false, preview = null, message = reason)
    }
    private fun interruptBatch() {
        val batch = mutable.value.batch ?: return
        val interrupted = batch.interrupted()
        store.saveBatch(interrupted)
        mutable.value = mutable.value.copy(batch = interrupted)
    }
    fun discard() { pause("Batch stopped. Observed submissions cannot be undone."); mutable.value.profile?.let { store.clearBatch(it.scope) }; mutable.value = mutable.value.copy(batch = null) }
    fun skip(id: Int) { if (!mutable.value.running && mutable.value.batch?.attempts?.any { it.applicantId == id && it.state != WhatsAppAttemptState.SubmissionObserved } == true) mark(id, WhatsAppAttemptState.Skipped) }
    fun retryFailed(id: Int) { if (!mutable.value.running && mutable.value.batch?.attempts?.any { it.applicantId == id && it.state == WhatsAppAttemptState.Failed } == true) mark(id, WhatsAppAttemptState.Pending) }
    private fun mark(id: Int, state: WhatsAppAttemptState) {
        val next = mutable.value.batch?.update(id, state) ?: error("Batch was stopped")
        store.saveBatch(next) // Durable write BEFORE allowing any Send action.
        mutable.value = mutable.value.copy(batch = next)
    }
    fun testSelf(raw: String) = task {
        val phone = automationPhone(raw) ?: error("Enter your own WhatsApp number with country code")
        val profile = mutable.value.profile ?: return@task
        val service = WhatsAppAccessibilityService.connected ?: error("Enable the DIPI WhatsApp accessibility service first")
        val version = packageVersion() ?: error("WhatsApp is not installed")
        val result = service.send(profile.packageName, phone, "DIPI automation SELF TEST ${UUID.randomUUID()}\nनमस्ते · https://example.com/?a=1&b=2", true) {}
        check(result.state == WhatsAppAttemptState.SubmissionObserved) { result.reason }
        val next = profile.copy(testedVersion = version)
        store.save(next)
        mutable.value = mutable.value.copy(profile = next, message = "Self-test passed for WhatsApp $version. Only submission was observed.")
    }
    private fun task(block: suspend () -> Unit) {
        if (mutable.value.running || mutable.value.busy) return
        val mine = ++epoch
        job = scope.launch {
            mutable.value = mutable.value.copy(busy = true, message = "Working…")
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (mine == epoch) message(e.message ?: "Operation failed") }
            finally { if (mine == epoch) mutable.value = mutable.value.copy(busy = false) }
        }
    }
    private fun safe(block: () -> Unit) { try { block() } catch (e: Exception) { message(e.message ?: "Operation failed") } }
    private fun message(value: String) { mutable.value = mutable.value.copy(message = value) }
}
