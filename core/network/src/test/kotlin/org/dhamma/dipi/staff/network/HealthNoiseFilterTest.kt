package org.dhamma.dipi.staff.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port of the course-audit "Send to Claude" noise filter — keep/drop cases
 * straight from loader.js and its README.
 */
class HealthNoiseFilterTest {

    private fun drops(v: String?) = assertFalse("expected DROP: \"$v\"", HealthNoiseFilter.keep(v))
    private fun keeps(v: String?) = assertTrue("expected KEEP: \"$v\"", HealthNoiseFilter.keep(v))

    @Test
    fun dropsBlankAndNoLikeValues() {
        listOf(null, "", "  ", "no", "No", "NA", "n/a", "nil", "none", "-", "—", ".", "...", "*")
            .forEach { drops(it) }
    }

    @Test
    fun dropsGenericPositives() {
        listOf(
            "happy", "good", "fine", "normal", "healthy", "nice", "best",
            "cordial", "stable", "cheerful", "satisfied", "peaceful",
            "ok", "okay", "well", "great", "positive", "wonderful",
        ).forEach { drops(it) }
    }

    @Test
    fun dropsMultiWordGenericPositives() {
        listOf(
            "very good", "so good", "all good", "feeling good", "feeling well",
            "happy and good", "happy and cheerful", "happy and satisfied",
            "happy - everything is going fine", "fine and good",
        ).forEach { drops(it) }
    }

    @Test
    fun dropsSingleNegativeStateWordsAlone() {
        listOf("stressed", "confused", "anxious", "sad", "netural", "neutral", "stresssed")
            .forEach { drops(it) }
    }

    @Test
    fun dropsBareGeographicNames() {
        listOf("india", "Pune", "Maharashtra", "new delhi").forEach { drops(it) }
    }

    @Test
    fun keepsDepressedEvenAlone() {
        keeps("depressed")
        keeps("Depressed")
    }

    @Test
    fun keepsMultiWordFreeTextAndMedicationAndAddiction() {
        listOf(
            "back pain since 2019",
            "knee surgery in May, still recovering",
            "insulin for diabetes",
            "on psychiatric medication, dose changed last week",
            "alcohol daily",
            "anxious and on sleeping pills",
        ).forEach { keeps(it) }
    }

    @Test
    fun normalisesCaseNbspAndWhitespaceBeforeMatching() {
        drops("  Fine \u00A0 ")
        drops("GOOD")
        drops("very\u00A0good")
    }

    @Test
    fun pregnancyDropsForMalesAndNoValues() {
        assertFalse(HealthNoiseFilter.keep("Yes ( 3 months )", pregnancy = true, male = true))
        assertFalse(HealthNoiseFilter.keep("No", pregnancy = true, male = false))
        assertFalse(HealthNoiseFilter.keep("No, not pregnant", pregnancy = true, male = false))
    }

    @Test
    fun pregnancyWithDetailsKeepsForFemales() {
        assertTrue(HealthNoiseFilter.keep("Yes ( 3 months )", pregnancy = true, male = false))
        assertTrue(HealthNoiseFilter.keep("Yes", pregnancy = true, male = false))
    }
}
