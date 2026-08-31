package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppTemplateTest {

    @Test
    fun tokensFillFromTheCardOnScreen() {
        val out = whatsAppMessage(
            template = "Hello {name}, {course} at {centre} runs {dates}. Seat {conf}.",
            name = "Rajat Kumar Sharma",
            course = "10 Day",
            dates = "2 Sep - 13 Sep",
            centre = "Dhamma Sudha",
            conf = "NM66",
        )
        // The greeting takes the first word only, so it reads as a name.
        assertEquals("Hello Rajat, 10 Day at Dhamma Sudha runs 2 Sep - 13 Sep. Seat NM66.", out)
    }

    @Test
    fun blankTemplateFallsBackToTheDefault() {
        val out = whatsAppMessage(template = "   ", name = "Priya Nair", centre = "Dhamma Sudha")
        assertTrue(out.startsWith("नमस्ते Priya जी"))
        assertTrue(out.contains("Dhamma Sudha"))
        // Tokens the caller left empty resolve to nothing, never to "{course}".
        assertTrue(WHATSAPP_TOKENS.none { out.contains(it) })
    }

    @Test
    fun anUnknownTokenSurvivesSoTheTypoIsVisibleInThePreview() {
        assertEquals("Hi Priya {venue}", whatsAppMessage("Hi {name} {venue}", "Priya Nair"))
    }
}
