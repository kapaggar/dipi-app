package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.HealthRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 2d S1 — the application-view parser against a fixture carrying ALL
 * sections, including realistic Identification / Contact / Emergency NPI.
 * The load-bearing negative: no decoy string from the fixture's skipped
 * sections appears anywhere in the parsed model.
 */
class ApplicationViewParserTest {

    /** Every string field of the model, flattened — the grep surface. */
    /**
     * Every property of the model by reflection, so a future field addition is
     * swept automatically (gate review 2d F2). The count pin fails the build
     * when the data class grows, forcing this sweep to be re-checked.
     */
    private fun flatten(card: ApplicationCard): String {
        val props = ApplicationCard::class.java.declaredFields
            .filter { !java.lang.reflect.Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
            .sortedBy { it.name }
        assertEquals("ApplicationCard grew — re-verify the NPI sweep", 13, props.size)
        return buildString {
            props.forEach { p ->
                when (val v = p.get(card)) {
                    is List<*> -> v.forEach { item ->
                        when (item) {
                            is Pair<*, *> -> append(item.first).append('=').append(item.second)
                            is HealthRow -> append(item.label).append('=').append(item.answer)
                            else -> append(item)
                        }
                        append('\n')
                    }
                    is Set<*> -> v.forEach { append(it).append('\n') }
                    else -> append(v).append('\n')
                }
            }
        }
    }

    @Test
    fun npiFromSkippedSectionsNeverEntersTheModel() {
        // Suresh (id 4): all sections present, photo present, health answered.
        val card = ApplicationViewParser.parse(MockFixtures.applicationViewHtml(4))
        val flat = flatten(card)
        listOf(
            MockFixtures.AV_NPI_AADHAAR,
            MockFixtures.AV_NPI_PAN,
            MockFixtures.AV_NPI_PASSPORT,
            MockFixtures.AV_NPI_VOTER,
            MockFixtures.AV_NPI_MOBILE,
            MockFixtures.AV_NPI_EMERGENCY_NAME,
            MockFixtures.AV_NPI_EMERGENCY_NO,
            MockFixtures.AV_NPI_ADDRESS,
            MockFixtures.AV_NPI_FATHER_CONTACT,
            MockFixtures.AV_NPI_MOTHER_CONTACT,
            MockFixtures.AV_NPI_SPOUSE_NAME,
            MockFixtures.AV_NPI_TRAGEDY,
        ).forEach { npi ->
            assertFalse("NPI string \"$npi\" leaked into the parsed model:\n$flat", flat.contains(npi))
        }
        // Nothing from the skipped Background / Languages / Other / lazy
        // sections either.
        assertFalse(flat.contains("Malayalam, English"))
        assertFalse(flat.contains("Friend"))
        assertFalse(flat.contains("Loading"))
    }

    @Test
    fun headerPersonalHistoryAndHealthParseVerbatim() {
        val card = ApplicationViewParser.parse(MockFixtures.applicationViewHtml(4))
        assertEquals("Suresh Nair", card.name)
        assertEquals("OM42", card.conf)
        assertTrue(card.statusLine.startsWith("Confirmed · "))
        assertTrue(card.hasPhoto)

        // The eight Personal rows, keys and values verbatim, page order.
        assertEquals(
            listOf("Gender", "Date of Birth", "Age", "Nationality", "Old / New", "Monk / Nun", "A-List", "Applied On"),
            card.personal.map { it.first },
        )
        assertEquals("Male", card.personalValue("Gender"))
        assertEquals("Old", card.personalValue("Old / New"))
        assertEquals("-", card.personalValue("A-List"))

        // Ten counts in SERVER order; zeros stay.
        assertEquals(ApplicationCard.HISTORY_ORDER, card.historyCounts.map { it.first })
        assertEquals(11, card.historyCounts.first { it.first == "10-Day" }.second)
        assertEquals(3, card.historyCounts.first { it.first == "STP" }.second)
        assertEquals(0, card.historyCounts.first { it.first == "Teen" }.second)
        assertEquals(ApplicationCard.HISTORY_ORDER.toSet(), card.historyCountsPresent)
        assertEquals("2015-1-15, Dhamma sota sohna", card.firstCourse)
        assertEquals("2025-12-12, Dhamma Sudha", card.lastCourse)
        // Live /application-view Course History has date+location only.
        assertEquals("", card.firstCourseTeacher)
        assertEquals("", card.lastCourseTeacher)
        assertEquals("1 hr daily, both sittings", card.practiceDetails)

        // Six health rows, labels verbatim, answers verbatim.
        assertEquals(ApplicationCard.HEALTH_ORDER, card.health.map { it.label })
        assertEquals("Metformin 500mg twice daily", card.healthRow("Medication")?.answer)
        assertEquals("Chewed tobacco, stopped in 2019", card.healthRow("Intoxicants")?.answer)
        assertEquals("-", card.healthRow("Physical")?.answer)
    }

    @Test
    fun pregnancyMonthsParsesVerbatimAndDashPassesThrough() {
        // Meera (id 1): pregnant, migraines, the rest `-`.
        val card = ApplicationViewParser.parse(MockFixtures.applicationViewHtml(MockFixtures.MEERA_ID))
        assertEquals("Yes - 4 (months)", card.healthRow("Pregnancy")?.answer)
        assertTrue(card.healthRow("Pregnancy")!!.answered)
        assertEquals("Occasional migraines, takes Sumatriptan when acute", card.healthRow("Physical")?.answer)
        // `-` stays `-` and reads as unanswered.
        assertEquals("-", card.healthRow("Mental")?.answer)
        assertFalse(card.healthRow("Mental")!!.answered)
    }

    @Test
    fun missingPhotoAndMissingConfParse() {
        // Rakesh (id 2): pending, no conf on the header, no photo on the page.
        val card = ApplicationViewParser.parse(MockFixtures.applicationViewHtml(MockFixtures.RAKESH_ID))
        assertEquals("Rakesh Iyer", card.name)
        assertNull(card.conf)
        assertFalse(card.hasPhoto)
        assertEquals("New", card.personalValue("Old / New"))
        // Empty history: all ten zeros, meta verbatim `-`.
        assertTrue(card.historyCounts.all { it.second == 0 })
        assertEquals(ApplicationCard.HISTORY_ORDER.toSet(), card.historyCountsPresent)
        assertEquals("-", card.firstCourse)
        assertEquals("", card.firstCourseTeacher)
        assertEquals("", card.lastCourseTeacher)
    }

    @Test
    fun firstAndLastCourseTeacherParseWhenCourseHistoryPrintsThem() {
        val live = MockFixtures.applicationViewHtml(4)
        val html = live.replace(
            """<span class="av-label">Practice Details</span>""",
            """<span class="av-label">First Course Teacher</span><span class="av-val">S.N. Goenka</span></div>""" +
                """<div class="av-row"><span class="av-label">Last Course Teacher</span><span class="av-val">Uma Rangan</span></div>""" +
                """<div class="av-row"><span class="av-label">Practice Details</span>""",
        )
        val card = ApplicationViewParser.parse(html)
        assertEquals("S.N. Goenka", card.firstCourseTeacher)
        assertEquals("Uma Rangan", card.lastCourseTeacher)
        assertEquals("1 hr daily, both sittings", card.practiceDetails)
        assertFalse(card.toString().contains("S.N. Goenka"))
    }

    @Test
    fun teacherParenSUnderFirstAndMostRecentCourse() {
        val live = MockFixtures.applicationViewHtml(4)
        val html = live.replace(
            """<span class="av-label">Last Course</span><span class="av-val">2025-12-12, Dhamma Sudha</span></div>""" +
                """<div class="av-row"><span class="av-label">Practice Details</span>""",
            """<span class="av-label">Teacher(s)</span><span class="av-val">Unknown</span></div>""" +
                """<div class="av-row"><span class="av-label">Most Recent Course (Sat)</span>""" +
                """<span class="av-val">2026-2-1, Example Place</span></div>""" +
                """<div class="av-row"><span class="av-label">Teacher(s)</span>""" +
                """<span class="av-val">Mr. Example</span></div>""" +
                """<div class="av-row"><span class="av-label">Practice Details</span>""",
        )
        val card = ApplicationViewParser.parse(html)
        assertEquals("Unknown", card.firstCourseTeacher)
        assertEquals("Mr. Example", card.lastCourseTeacher)
        assertEquals("2026-2-1, Example Place", card.lastCourse)
        assertEquals("1 hr daily, both sittings", card.practiceDetails)
    }

    @Test
    fun longCourseDetailsTeacherNeverBecomesCourseHistoryTeacher() {
        val live = MockFixtures.applicationViewHtml(4)
        val html = live.replace(
            """<span class="av-label">Personal tragedy</span>""",
            """<span class="av-label">Teacher</span><span class="av-val">LC decoy teacher</span></div>""" +
                """<div class="av-row"><span class="av-label">Personal tragedy</span>""",
        )
        val card = ApplicationViewParser.parse(html)
        assertEquals("", card.firstCourseTeacher)
        assertEquals("", card.lastCourseTeacher)
        val flat = flatten(card)
        assertFalse(flat.contains("LC decoy teacher"))
    }
}
