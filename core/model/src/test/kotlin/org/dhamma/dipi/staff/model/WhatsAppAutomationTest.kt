package org.dhamma.dipi.staff.model

import org.junit.Assert.*
import org.junit.Test

class WhatsAppAutomationTest {
    private val scope = WhatsAppScope("https://desk.example.test", 91)
    @Test fun `normalises supported phones and rejects ambiguous input`() {
        assertEquals("919000000001", automationPhone("90000 00001"))
        assertEquals("14155552671", automationPhone("+1 (415) 555-2671"))
        listOf(null,"", "unknown", "123", "00000000000", "९००००००००१", "9000000001 / 9000000002", "+1234567890123456").forEach { assertNull(automationPhone(it)) }
    }
    @Test fun `scope cannot carry a credential query path or plain HTTP`() {
        listOf("http://desk.example.test", "https://user:password@desk.example.test", "https://desk.example.test/path", "https://desk.example.test?token=secret").forEach {
            assertThrows(IllegalArgumentException::class.java) { WhatsAppScope(it, 91) }
        }
    }
    @Test fun `interruption never converts an uncertain submission to retryable`() {
        val batch = WhatsAppBatch(scope, 100, 44, listOf(
            WhatsAppAttempt(1,"919000000001",WhatsAppAttemptState.SendStarted),
            WhatsAppAttempt(2,"919000000002",WhatsAppAttemptState.Opening),
            WhatsAppAttempt(3,"919000000003",WhatsAppAttemptState.SubmissionObserved)))
        val recovered = batch.interrupted()
        assertTrue(recovered.paused)
        assertEquals(listOf(WhatsAppAttemptState.OutcomeUnknown,WhatsAppAttemptState.Pending,WhatsAppAttemptState.SubmissionObserved), recovered.attempts.map { it.state })
        assertEquals(recovered, recovered.interrupted())
    }
    @Test fun `duplicate numbers require explicit batch confirmation`() {
        val attempts = listOf(WhatsAppAttempt(1,"919000000001"), WhatsAppAttempt(2,"919000000001"))
        assertThrows(IllegalArgumentException::class.java) { WhatsAppBatch(scope,100,44,attempts) }
        assertEquals(2,WhatsAppBatch(scope,100,44,attempts,duplicatesConfirmed=true).attempts.size)
        assertThrows(IllegalArgumentException::class.java) { WhatsAppBatch(scope,100,44,listOf(attempts[0], attempts[0]),duplicatesConfirmed=true) }
    }
}
