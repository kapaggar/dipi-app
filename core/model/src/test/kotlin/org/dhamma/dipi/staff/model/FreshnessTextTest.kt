package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class FreshnessTextTest {
    private val now = Instant.parse("2026-09-09T10:00:00Z")
    private val ist = ZoneId.of("Asia/Kolkata")
    private val la = ZoneId.of("America/Los_Angeles")
    private val locale = Locale.UK

    @Test
    fun missingAndInvalidAreUnknown() {
        assertEquals("Unknown", freshnessText(null, now, ist, locale))
        assertEquals("Unknown", freshnessText("not-a-time", now, ist, locale))
    }

    @Test
    fun futureStampNamesClockSkew() {
        val text = freshnessText("2026-09-09T11:00:00Z", now, ist, locale)
        assertTrue(text, text.contains("Device clock differs"))
        assertTrue(text, !text.contains("ago"))
    }

    @Test
    fun knownStampIncludesAbsoluteAndRelative() {
        val text = freshnessText("2026-09-09T09:59:30Z", now, ist, locale)
        assertTrue(text, text.contains("less than a minute ago"))
        val hours = freshnessText("2026-09-09T08:00:00Z", now, la, locale)
        assertTrue(hours, hours.contains("2 hours ago") || hours.contains("1 hour ago"))
    }
}
