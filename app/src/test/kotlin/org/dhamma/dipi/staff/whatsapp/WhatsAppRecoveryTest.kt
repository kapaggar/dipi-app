package org.dhamma.dipi.staff.whatsapp

import android.content.Context
import okhttp3.OkHttpClient
import org.dhamma.dipi.staff.BuildConfig
import org.dhamma.dipi.staff.datastore.WhatsAppStore
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.network.ManagedLetterGateway
import org.dhamma.dipi.staff.ui.DeskUiState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw600dp")
class WhatsAppRecoveryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val store = WhatsAppStore { context.getSharedPreferences("wa-controller", Context.MODE_PRIVATE).also { it.edit().clear().commit() } }
    private val scope = WhatsAppScope(BuildConfig.BASE_URL.trimEnd('/'), 91)
    private fun controller() = WhatsAppController(context, store, ManagedLetterGateway(OkHttpClient(), BuildConfig.BASE_URL))
    private fun state(centre: Int = 91, course: Int = 100) = DeskUiState(
        session = Session(5, "test", "Test", listOf(Centre(CentreId(centre), "Synthetic centre")), false),
        course = Course(CourseId(course), CentreId(centre), "Synthetic course", "", ""),
    )
    @Test fun `process recovery and logout never resume or retry an uncertain send`() {
        store.saveBatch(WhatsAppBatch(scope, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.SendStarted))))
        val restarted = controller()
        restarted.bind(state())
        assertFalse(restarted.ui.value.running)
        assertTrue(restarted.ui.value.batch!!.paused)
        assertEquals(WhatsAppAttemptState.OutcomeUnknown, restarted.ui.value.batch!!.attempts.single().state)
        restarted.retryFailed(1)
        assertEquals(WhatsAppAttemptState.OutcomeUnknown, restarted.ui.value.batch!!.attempts.single().state)
        restarted.endSession()
        assertNull(restarted.ui.value.batch)
        restarted.bind(state())
        assertFalse(restarted.ui.value.running)
        assertEquals(WhatsAppAttemptState.OutcomeUnknown, restarted.ui.value.batch!!.attempts.single().state)
        restarted.skip(1)
        assertEquals(WhatsAppAttemptState.Skipped, restarted.ui.value.batch!!.attempts.single().state)
    }
    @Test fun `centre and course changes isolate recovered progress and erase clears keys`() {
        store.provision(scope, "synthetic-key", "synthetic-iv")
        store.saveBatch(WhatsAppBatch(scope, 100, 44, listOf(WhatsAppAttempt(1, "919000000001"))))
        val controller = controller()
        controller.bind(state())
        assertNotNull(controller.ui.value.batch)
        controller.bind(state(course = 101))
        assertNull(controller.ui.value.batch)
        controller.bind(state(centre = 92))
        assertFalse(controller.ui.value.configured)
        assertNull(controller.ui.value.batch)
        controller.erase()
        assertFalse(store.configured(scope))
        assertNull(store.batch(scope, 100))
    }
    @Test @Config(qualifiers = "sw360dp")
    fun `phone sessions do not expose an automation profile`() {
        val controller = controller()
        controller.bind(state())
        assertNull(controller.ui.value.profile)
        assertFalse(controller.ready())
    }

    @Test fun `finishing preserves observed submissions through process recovery`() {
        store.saveBatch(WhatsAppBatch(scope, 100, 44, listOf(
            WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.SubmissionObserved),
            WhatsAppAttempt(2, "919000000002", WhatsAppAttemptState.Skipped))))
        val first = controller()
        first.bind(state())
        first.finishBatchRun()
        assertFalse(first.ui.value.running)
        assertEquals("Batch complete", batchProgressTitle(first.ui.value.batch!!, false))
        val restarted = controller()
        restarted.bind(state())
        assertEquals("Batch complete", batchProgressTitle(restarted.ui.value.batch!!, false))
        assertEquals(WhatsAppAttemptState.SubmissionObserved, restarted.ui.value.batch!!.attempts.first().state)
    }

    @Test fun `interrupted send never appears complete or becomes retryable`() {
        store.saveBatch(WhatsAppBatch(scope, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.SendStarted))))
        val controller = controller()
        controller.bind(state())
        controller.finishBatchRun()
        assertFalse(batchComplete(controller.ui.value.batch!!))
        assertEquals("Batch paused · review progress", batchProgressTitle(controller.ui.value.batch!!, false))
        controller.retryFailed(1)
        assertEquals(WhatsAppAttemptState.OutcomeUnknown, controller.ui.value.batch!!.attempts.single().state)
    }

    @Test fun `switching courses preserves separate batches and discard affects only current course`() {
        val old = WhatsAppBatch(scope, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.OutcomeUnknown)))
        val other = WhatsAppBatch(scope, 101, 44, listOf(WhatsAppAttempt(2, "919000000002")))
        store.saveBatch(old)
        store.saveBatch(other)
        val controller = controller()
        controller.bind(state())
        assertEquals(old, controller.ui.value.batch)
        controller.bind(state(course = 101))
        assertEquals(other, controller.ui.value.batch)
        assertFalse(controller.ui.value.running)
        controller.discard()
        assertNull(store.batch(scope, 101))
        controller.bind(state())
        assertEquals(old, controller.ui.value.batch)
        assertFalse(controller.ui.value.running)
    }

}
