package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 2d S3 — the derived FLAGS pure fn, plus the redacting-toString rule
 * (never log answers) on every model that carries health text.
 */
class ApplicationCardTest {

    private fun card(
        health: Map<String, String> = emptyMap(),
        monk: String = "No",
    ) = ApplicationCard(
        name = "Suresh Nair",
        conf = "OM42",
        personal = listOf(
            "Gender" to "Male",
            "Date of Birth" to "2 Feb 1975",
            "Age" to "51",
            "Nationality" to "Indian",
            "Old / New" to "Old",
            "Monk / Nun" to monk,
            "A-List" to "-",
            "Applied On" to "14 Jun 2026",
        ),
        health = ApplicationCard.HEALTH_ORDER.map { HealthRow(it, health[it] ?: "-") },
    )

    @Test
    fun eachFlagFiresOnItsOwnTrigger() {
        assertEquals(listOf("HLTH"), flagsFor(card(mapOf("Physical" to "Chronic back pain")), Gender.M))
        assertEquals(listOf("HLTH"), flagsFor(card(mapOf("Mental" to "Treated for anxiety")), Gender.M))
        assertEquals(listOf("MED"), flagsFor(card(mapOf("Medication" to "Metformin 500mg")), Gender.M))
        assertEquals(listOf("INTOX"), flagsFor(card(mapOf("Intoxicants" to "Tobacco until 2019")), Gender.M))
        assertEquals(listOf("TECH"), flagsFor(card(mapOf("Other Techniques" to "Art of Living, 2 years")), Gender.M))
        assertEquals(listOf("PREG"), flagsFor(card(mapOf("Pregnancy" to "Yes - 4 (months)")), Gender.F))
        assertEquals(listOf("MONK"), flagsFor(card(monk = "Yes"), Gender.M))
    }

    @Test
    fun flagOrderIsFixedAsListed() {
        val all = card(
            mapOf(
                "Physical" to "x",
                "Medication" to "x",
                "Intoxicants" to "x",
                "Other Techniques" to "x",
                "Pregnancy" to "Yes",
            ),
            monk = "Yes",
        )
        assertEquals(FLAG_ORDER, flagsFor(all, Gender.F))
    }

    @Test
    fun pregnancyNeverFlagsForMalesAndOnlyOnYes() {
        // The question does not apply to gender M — even a stray Yes never flags.
        assertEquals(emptyList<String>(), flagsFor(card(mapOf("Pregnancy" to "Yes")), Gender.M))
        // A plain No is not a disclosure.
        assertEquals(emptyList<String>(), flagsFor(card(mapOf("Pregnancy" to "No")), Gender.F))
        assertEquals(listOf("PREG"), flagsFor(card(mapOf("Pregnancy" to "Yes")), Gender.F))
    }

    @Test
    fun blankAndDashBothUnflag() {
        assertEquals(emptyList<String>(), flagsFor(card(mapOf("Physical" to "-")), Gender.M))
        assertEquals(emptyList<String>(), flagsFor(card(mapOf("Physical" to "")), Gender.M))
        assertEquals(emptyList<String>(), flagsFor(card(mapOf("Physical" to "  ")), Gender.M))
        assertEquals(emptyList<String>(), flagsFor(card(), Gender.F))
    }

    @Test
    fun answeredIsNonBlankNonDash() {
        assertTrue(HealthRow("Physical", "Migraines").answered)
        assertFalse(HealthRow("Physical", "-").answered)
        assertFalse(HealthRow("Physical", "").answered)
        assertFalse(HealthRow("Physical", " - ").answered)
    }

    @Test
    fun toStringNeverCarriesAnswersOrPersonalValues() {
        val c = card(
            mapOf(
                "Physical" to "Insulin-dependent diabetes",
                "Mental" to "Treated for severe depression in 2021",
            ),
        )
        val printed = c.toString() + c.health.joinToString { it.toString() }
        assertFalse(printed.contains("diabetes"))
        assertFalse(printed.contains("depression"))
        // Personal values (DOB and the rest) stay out of toString too.
        assertFalse(printed.contains("2 Feb 1975"))
        // Labels may appear — they are the page's fixed vocabulary, not answers.
        assertTrue(printed.contains("Physical"))
    }
}
