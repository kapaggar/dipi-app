package org.dhamma.dipi.staff.whatsapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import org.dhamma.dipi.staff.MainActivity
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import org.dhamma.dipi.staff.model.WhatsAppAttemptState
import org.dhamma.dipi.staff.model.WHATSAPP_PACKAGES
import javax.inject.Inject

class SendResult(val state: WhatsAppAttemptState, val reason: String)

/** Reads only the selected package. No coordinates, gestures, chat scraping or logging. */
@AndroidEntryPoint
class WhatsAppAccessibilityService : AccessibilityService() {
    @Inject lateinit var controller: WhatsAppController
    private val handler = Handler(Looper.getMainLooper())
    private var request: Request? = null
    private var controls: LinearLayout? = null
    private var watchingPackage: String? = null
    val hasRequest: Boolean get() = request != null
    private val tick = object : Runnable {
        override fun run() { inspect(); if (request != null || watchingPackage != null) handler.postDelayed(this, 250) }
    }
    private class Request(val pkg: String, val phone: String, val text: String, val self: Boolean,
        val beforeClick: () -> Unit, val result: CompletableDeferred<SendResult>) {
        var appeared = false
        var clicked = false
        var baseline = 0
        var draftSet = false
        var observation = "chat not ready"
        var started = android.os.SystemClock.elapsedRealtime()
    }
    override fun onServiceConnected() {
        connected = this
        serviceInfo = serviceInfo.apply {
            packageNames = WHATSAPP_PACKAGES.toTypedArray()
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Poll the active window to detect app switches too. */ }
    override fun onInterrupt() { if (request != null || controller.ui.value.running) controller.pause("Accessibility was interrupted.") }
    override fun onDestroy() {
        if (request != null || controller.ui.value.running) controller.pause("Accessibility service disconnected.")
        clear()
        if (connected === this) connected = null
        super.onDestroy()
    }

    suspend fun send(pkg: String, phone: String, text: String, self: Boolean, beforeClick: () -> Unit): SendResult {
        check(pkg in WHATSAPP_PACKAGES && request == null) { "Another WhatsApp operation is active" }
        check(!getSystemService(KeyguardManager::class.java).isKeyguardLocked) { "Unlock the tablet first" }
        val completion = CompletableDeferred<SendResult>()
        request = Request(pkg, phone, text, self, beforeClick, completion)
        serviceInfo = serviceInfo.apply { packageNames = arrayOf(pkg) }
        try {
            showControls()
            val uri = Uri.parse("whatsapp://send").buildUpon().appendQueryParameter("phone", phone).build()
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            handler.post(tick)
            return completion.await()
        } finally { clear() }
    }
    fun returnToDipiIfVisible() {
        if (getSystemService(KeyguardManager::class.java).isKeyguardLocked) return
        val root = rootInActiveWindow ?: return
        val visible = try { root.packageName?.toString() in WHATSAPP_PACKAGES } finally { root.recycle() }
        if (visible) startActivity(Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }
    fun abort(reason: String) {
        val active = request ?: return
        active.result.complete(SendResult(if (active.clicked) WhatsAppAttemptState.OutcomeUnknown else WhatsAppAttemptState.Failed, reason))
        clear()
    }
    fun clear() {
        handler.removeCallbacks(tick)
        request = null
        if (controller.ui.value.running && watchingPackage != null) {
            handler.postDelayed(tick, 250)
            return
        }
        watchingPackage = null
        controls?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        controls = null
    }
    private fun showControls() {
        if (controls != null) return
        val row = LinearLayout(this)
        row.setBackgroundColor(0xFF24364B.toInt())
        row.addView(Button(this).apply { text = "PAUSE"; setOnClickListener { controller.pause() } })
        row.addView(Button(this).apply { text = "STOP"; setOnClickListener { controller.pause("Stopped. Return to DIPI to review this batch.") } })
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 48 }
        getSystemService(WindowManager::class.java).addView(row, params)
        controls = row
    }
    private fun inspect() {
        val active = request
        if (active == null) {
            if (!controller.ui.value.running) { clear(); return }
            val root = rootInActiveWindow
            try {
                if (getSystemService(KeyguardManager::class.java).isKeyguardLocked ||
                    !getSystemService(PowerManager::class.java).isInteractive ||
                    root?.packageName?.toString() != watchingPackage) {
                    controller.pause("WhatsApp was interrupted between recipients. Review progress before resuming.")
                }
            } finally { root?.recycle() }
            return
        }
        try {
            if (getSystemService(KeyguardManager::class.java).isKeyguardLocked || !getSystemService(PowerManager::class.java).isInteractive) {
                abort("Screen locked. Review progress before resuming."); return
            }
            val elapsed = android.os.SystemClock.elapsedRealtime() - active.started
            if (elapsed > 20_000) { abort("WhatsApp timed out (${active.observation}). Review the last attempt."); return }
            val root = rootInActiveWindow ?: return
            val obtained = mutableListOf<AccessibilityNodeInfo>()
            try {
                root.refresh()
                if (root.packageName?.toString() != active.pkg) {
                    if (interruptsWhatsAppLaunch(root.packageName?.toString(), active.pkg, packageName, active.appeared, elapsed)) abort("Another app or system dialog interrupted WhatsApp.")
                    return
                }
                active.appeared = true
                if (controller.ui.value.running) watchingPackage = active.pkg
                fun nodes(id: String) = root.findAccessibilityNodeInfosByViewId("${active.pkg}:id/$id").also { obtained.addAll(it) }.filter { it.refresh() && it.isVisibleToUser }
                val names = nodes("conversation_contact_name")
                val entries = nodes("entry")
                if (names.size != 1 || entries.size != 1) { if (elapsed > 8000) abort("Unsupported WhatsApp screen. Nothing further will be sent."); return }
                val header = names.single().text?.toString().orEmpty()
                val selfLabel = nodes("conversation_contact_status").singleOrNull()?.text?.toString() == "Message yourself"
                if (!verifiedRecipient(header, active.phone, active.self, selfLabel)) {
                    if (active.clicked || elapsed > 8000) abort("Recipient identity could not be verified. A saved contact name alone is insufficient.")
                    return
                }
                val entry = entries.single().text?.toString().orEmpty()
                // Refresh recycled message nodes: Android 8 can retain their previous
                // text after Send even when the new row is already visible.
                fun descendants(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
                    if (!node.refresh()) return emptyList()
                    val children = (0 until node.childCount).mapNotNull { node.getChild(it) }
                    obtained.addAll(children)
                    return children + children.flatMap { descendants(it) }
                }
                val rows = nodes("conversation_text_row")
                val matches = rows.count { row ->
                    val children = descendants(row)
                    children.forEach { it.refresh() }
                    children.count { it.viewIdResourceName == "${active.pkg}:id/message_text" && it.text?.toString() == active.text } == 1 &&
                        children.any { it.viewIdResourceName == "${active.pkg}:id/status" && !it.contentDescription.isNullOrBlank() }
                }
                active.observation = if (active.clicked) "after Send; ${matches - active.baseline} new matching outgoing rows; composer ${if (entry in setOf("", "Message")) "clear" else "not clear"}" else "before Send"
                if (active.clicked) {
                    if (submissionObserved(entry, matches, active.baseline)) {
                        active.result.complete(SendResult(WhatsAppAttemptState.SubmissionObserved, "Submission observed"))
                        clear()
                    }
                    return
                }
                if (!active.draftSet && entry in setOf("", "Message")) {
                    active.draftSet = true
                    val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, active.text) }
                    if (!entries.single().performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) abort("WhatsApp composer could not be prepared.")
                    return
                }
                if (entry != active.text) { if (elapsed > 8000) abort("WhatsApp draft does not exactly match the prepared letter."); return }
                val buttons = nodes("send")
                if (buttons.size != 1 || !buttons.single().isEnabled || !buttons.single().isClickable) { abort("WhatsApp Send button is unavailable."); return }
                active.baseline = matches
                active.beforeClick() // Must persist SendStarted successfully before clicking.
                active.clicked = true
                active.started = android.os.SystemClock.elapsedRealtime()
                if (!buttons.single().performAction(AccessibilityNodeInfo.ACTION_CLICK)) abort("Send action was not confirmed. Check WhatsApp before continuing.")
            } finally { obtained.forEach { it.recycle() }; root.recycle() }
        } catch (_: Exception) { abort("WhatsApp could not be verified. Review the last attempt before continuing.") }
    }
    companion object { var connected: WhatsAppAccessibilityService? = null; private set }
}

/** Keep these decisions testable without access to real conversations. */
internal fun verifiedRecipient(header: String, expected: String, selfTest: Boolean, selfLabel: Boolean): Boolean {
    if (selfTest) return header.endsWith("(You)") && selfLabel
    if (!header.matches(Regex("\\+?[0-9 ()-]+"))) return false
    return header.filter { it in '0'..'9' } == expected
}
internal fun submissionObserved(composer: String, matchingOutgoing: Int, baseline: Int): Boolean =
    composer in setOf("", "Message") && matchingOutgoing > baseline

/** Only our own launching window gets grace; another app or system dialog stops immediately. */
internal fun interruptsWhatsAppLaunch(actual: String?, selected: String, own: String, appeared: Boolean, elapsed: Long): Boolean =
    actual != selected && (actual != own || appeared || elapsed > 8000)
