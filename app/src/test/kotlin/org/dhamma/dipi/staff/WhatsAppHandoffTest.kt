package org.dhamma.dipi.staff

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.dhamma.dipi.staff.ui.whatsAppIntents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhatsAppHandoffTest {

    @Test
    fun theAppItselfIsTriedBeforeTheBrowser() {
        val intents = whatsAppIntents("919876543210", "Hello Priya")
        assertEquals(3, intents.size)
        assertEquals("com.whatsapp", intents[0].`package`)
        assertEquals("com.whatsapp.w4b", intents[1].`package`)
        // Last resort only: no package, so whatever handles https takes it.
        assertNull(intents[2].`package`)
        intents.forEach { assertEquals(Intent.ACTION_VIEW, it.action) }
    }

    @Test
    fun theDeepLinkCarriesTheNumberAndTheEncodedMessage() {
        val intents = whatsAppIntents("919876543210", "Hello Priya & family")
        val deep = intents[0].data.toString()
        assertTrue(deep.startsWith("whatsapp://send?phone=919876543210&text="))
        // Encoded, so an ampersand cannot split the query.
        assertTrue(deep.endsWith("Hello%20Priya%20%26%20family"))
        assertEquals("Hello Priya & family", intents[0].data?.getQueryParameter("text"))

        val web = intents[2].data.toString()
        assertTrue(web.startsWith("https://wa.me/919876543210?text="))
    }
}
