package org.dhamma.dipi.staff.whatsapp

import org.junit.Assert.*
import org.junit.Test

class WhatsAppVerificationTest {
    @Test fun `requires exact full number not suffix or display name`() {
        assertTrue(verifiedRecipient("+91 90000 00001", "919000000001", false, false))
        assertFalse(verifiedRecipient("90000 00001", "919000000001", false, false))
        assertFalse(verifiedRecipient("Other person", "919000000001", false, false))
        assertFalse(verifiedRecipient("Desk +919000000001", "919000000001", false, false))
        assertFalse(verifiedRecipient("+91 90000 00002", "919000000001", false, false))
    }
    @Test fun `self marker can only authorise explicitly requested pilot`() {
        assertFalse(verifiedRecipient("Name (You)", "919000000001", false, true))
        assertTrue(verifiedRecipient("Name (You)", "919000000001", true, true))
        assertFalse(verifiedRecipient("Name (You)", "919000000001", true, false))
        assertFalse(verifiedRecipient("Someone", "919000000001", true, true))
    }
    @Test fun `empty composer and old matching message do not prove a submission`() {
        assertFalse(submissionObserved("", 1, 1))
        assertFalse(submissionObserved("", 0, 0))
        assertFalse(submissionObserved("Draft", 1, 0))
        assertTrue(submissionObserved("", 1, 0))
        assertTrue(submissionObserved("Message", 2, 1))
    }
    @Test fun `launch grace never permits another app or dialog`() {
        assertFalse(interruptsWhatsAppLaunch("desk", "wa", "desk", false, 100))
        assertFalse(interruptsWhatsAppLaunch("wa", "wa", "desk", true, 10000))
        assertTrue(interruptsWhatsAppLaunch("launcher", "wa", "desk", false, 100))
        assertTrue(interruptsWhatsAppLaunch("android", "wa", "desk", false, 100))
        assertTrue(interruptsWhatsAppLaunch("desk", "wa", "desk", true, 100))
        assertTrue(interruptsWhatsAppLaunch("desk", "wa", "desk", false, 8001))
    }

}
