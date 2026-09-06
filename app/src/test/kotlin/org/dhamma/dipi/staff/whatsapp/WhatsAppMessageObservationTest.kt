package org.dhamma.dipi.staff.whatsapp

import org.junit.Assert.*
import org.junit.Test

class WhatsAppMessageObservationTest {
    @Test fun `bold display hides delimiters but preserves Hindi paragraphs and full links`() {
        val prepared = "*नमस्ते मित्र*\n\n*Please arrive* at 4 pm.\nhttps://example.test/?a=one&b=two%2Bthree"
        val displayed = "नमस्ते मित्र\n\nPlease arrive at 4 pm.\nhttps://example.test/?a=one&b=two%2Bthree"
        assertTrue(outgoingTextMatches(displayed, prepared))
        assertFalse(outgoingTextMatches(displayed.replace("4 pm", "5 pm"), prepared))
        assertFalse(outgoingTextMatches(displayed.replace("two%2Bthree", "someone-else"), prepared))
    }
    @Test fun `literal asterisks and URL characters are never removed`() {
        val text = "Compute 2 * 3 * 4\nhttps://example.test/*token*/?a=1&b=2\nAn unmatched * stays"
        assertTrue(outgoingTextMatches(text, text))
        assertFalse(outgoingTextMatches(text.replace("*token*", "token"), text))
        assertFalse(outgoingTextMatches(text.replace("2 * 3 * 4", "2  3  4"), text))
    }
    @Test fun `collapsed preview is only a candidate and can never confirm full submission`() {
        val prepared = "*नमस्ते*\n" + "Long synthetic letter paragraph. ".repeat(160) + "\nhttps://example.test/?a=1&b=2"
        val prefix = ("नमस्ते\n" + "Long synthetic letter paragraph. ".repeat(10))
        assertTrue(collapsedLetterCandidate(prefix + "… Read more", prepared, true))
        assertFalse(outgoingTextMatches(prefix + "… Read more", prepared))
        assertFalse(collapsedLetterCandidate(prefix, prepared, false))
        assertFalse(collapsedLetterCandidate("different ".repeat(20), prepared, true))
        assertFalse(collapsedLetterCandidate("नमस्ते", prepared, true))
    }
    @Test fun `expansion cannot accept an existing similar row or a nonempty composer`() {
        assertTrue(canExpandNewCandidate("Message", 1, 0))
        assertFalse(canExpandNewCandidate("Message", 1, 1))
        assertFalse(canExpandNewCandidate("Message", 2, 0))
        assertFalse(canExpandNewCandidate("draft", 1, 0))
    }

}
