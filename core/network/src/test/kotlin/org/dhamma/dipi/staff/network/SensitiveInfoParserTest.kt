package org.dhamma.dipi.staff.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The owner-approved display amendment: ID values and health disclosures are
 * parsed for on-screen verification only. They must land in the SearchPage
 * sensitive map and must NEVER reach the ApplicantDto (whose JSON payload is
 * persisted to Room).
 */
class SensitiveInfoParserTest {

    private val html = """
        <script>
        var dataset = [
          {"aid":11,"name":"Meera Deshpande","gender":"Female","courseid":42,"centreid":1,
           "app_status":"Confirmed","aadhar":"9999 1234 5678",
           "physical":"back pain since 2019","mental":"depressed",
           "medication":"insulin for diabetes","addiction":"tobacco daily",
           "othertechnique":"","pregnant":"Yes&nbsp;( 3 months )",
           "course_10d":"3","course_20d":"1","o_n":"Old<br>Female"},
          {"aid":12,"name":"Arun Kale","gender":"Male","courseid":42,"centreid":1,
           "app_status":"Pending","aadhar":"ABCDE1234F",
           "physical":"happy","mental":"stressed","medication":"no",
           "addiction":"na","pregnant":"No"},
          {"aid":13,"name":"Rohan Joshi","gender":"Male","courseid":42,"centreid":1,
           "app_status":"Pending"}
        ];
        </script>
    """.trimIndent()

    @Test
    fun extractsIdAndOnlySurvivingHealthFields() {
        val page = SearchPageParser.parse(html, pathCentreId = 1)
        val info = page.sensitive.getValue(11)
        assertEquals("Aadhaar", info.idLabel)
        assertEquals("9999 1234 5678", info.idNumber)
        assertEquals(
            mapOf(
                "Physical health" to "back pain since 2019",
                "Mental health" to "depressed",
                "Medication" to "insulin for diabetes",
                "Addiction" to "tobacco daily",
                "Pregnancy" to "Yes ( 3 months )",
            ),
            info.health,
        )
        // Blank othertechnique never appears.
        assertFalse(info.health.containsKey("Other meditation"))
    }

    @Test
    fun panShapedValueInAadharColumnClassifiedAsPan() {
        val page = SearchPageParser.parse(html)
        val info = page.sensitive.getValue(12)
        assertEquals("PAN", info.idLabel)
        assertEquals("ABCDE1234F", info.idNumber)
        // Generic positives, single negative-state words, "no"/"na" and the
        // male "No" pregnancy all drop — nothing survives the filter.
        assertTrue(info.health.isEmpty())
    }

    @Test
    fun rowWithoutIdOrDisclosuresHasNoSensitiveEntry() {
        val page = SearchPageParser.parse(html)
        assertNull(page.sensitive[13])
    }

    @Test
    fun sensitiveValuesNeverReachThePersistedDtoJson() {
        val page = SearchPageParser.parse(html)
        val meera = page.dataset.first { it.id == 11 }
        // Exactly what StaffRepository.persist() writes to Room.
        val persisted = Json.encodeToString(ApplicantDto.serializer(), meera)
        listOf(
            "9999 1234 5678", "back pain", "depressed", "insulin", "diabetes",
            "tobacco", "3 months", "aadhar", "pregnant",
        ).forEach { leak ->
            assertFalse("persisted JSON leaks \"$leak\"", persisted.contains(leak, ignoreCase = true))
            assertFalse("DTO toString leaks \"$leak\"", meera.toString().contains(leak, ignoreCase = true))
        }
        // Presence boolean is all the DTO may carry.
        assertEquals(true, meera.idPresent)
    }

    @Test
    fun sensitiveInfoToStringRedactsValues() {
        val info = SearchPageParser.parse(html).sensitive.getValue(11)
        val s = info.toString()
        assertFalse(s.contains("9999"))
        assertFalse(s.contains("back pain"))
        assertFalse(s.contains("insulin"))
        // Field labels (keys) are fine; they carry no disclosure.
        assertTrue(s.contains("Mental health"))
    }

    @Test
    fun extendedCourseCountsLandOnTheDtoHistory() {
        val page = SearchPageParser.parse(html)
        val meera = page.dataset.first { it.id == 11 }
        val counts = meera.history!!.counts.associate { it.label to it.n }
        assertEquals(3, counts["10-day"])
        assertEquals(1, counts["20-day"])
    }
}
