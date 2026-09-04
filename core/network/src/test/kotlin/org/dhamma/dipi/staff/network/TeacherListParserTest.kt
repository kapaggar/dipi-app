package org.dhamma.dipi.staff.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Spec 2b S2: the teacher-roll parser against the fixture built verbatim
 * from `dh_generate_teacher_list`'s shapes (inc/zero-day.inc:877-1072) —
 * pipe band incl. `(unassigned)`, name suffixes, `CW-`/`CH-`/`BR` seats,
 * S/N restart per block, ordered course pairs incl. SPL, and the
 * load-bearing NPI assertion: the Comments cell is present in the fixture
 * but its health text appears NOWHERE in the parsed output.
 */
class TeacherListParserTest {

    private val roll: TeacherRoll =
        TeacherListParser.parse(MockFixtures.teacherListHtml(1, 10))

    @Test
    fun fragmentStartsUnthemedAndStillParses() {
        val html = MockFixtures.teacherListHtml(1, 10)
        assertTrue("live shape: unthemed fragment starting <style>", html.startsWith("<style>"))
        assertFalse(html.contains("<html", ignoreCase = true))
        assertEquals(3, roll.groups.size)
    }

    @Test
    fun bandParsesPipeSeparatedTokensIncludingUnassigned() {
        val g1 = roll.groups[0]
        assertEquals("Trainee-A-M Teacher", g1.at)
        assertEquals("TAM", g1.code)
        assertEquals(Gender.M, g1.gender)
        assertEquals(RollSeniority.OLD, g1.seniority)
        assertEquals("1", g1.group)
        assertEquals(3, g1.total)
        assertEquals("AT: Trainee-A-M Teacher [TAM]", g1.atLine)
        assertEquals("Male · Old · Group 1", g1.qualifier)

        val g2 = roll.groups[1]
        assertEquals("(unassigned)", g2.at)
        assertNull(g2.code)
        assertEquals("AT: (unassigned)", g2.atLine)
        assertEquals(RollSeniority.NEW, g2.seniority)

        val g3 = roll.groups[2]
        assertEquals(Gender.F, g3.gender)
        assertEquals("Uma Rangan", g3.at)
        assertEquals("URN", g3.code)
    }

    @Test
    fun studentSuffixesBecomeRoleTagAndLeaveTheNameClean() {
        val sevak = roll.groups[0].rows[0]
        assertEquals("Suresh Nair", sevak.name)
        assertEquals("Sevak", sevak.roleTag)

        val at = roll.groups[0].rows[1]
        assertEquals("Vikram Joshi", at.name)
        assertEquals("AT-2010", at.roleTag)

        val plain = roll.groups[0].rows[2]
        assertEquals("Nikhil Rane", plain.name)
        assertNull(plain.roleTag)

        val sat = roll.groups[2].rows[0]
        assertEquals("Meera Deshpande", sat.name)
        assertEquals("SAT-2011", sat.roleTag)
    }

    @Test
    fun seatPrefixesAndBackrestSpanParse() {
        val cw = roll.groups[0].rows[1]
        assertEquals("CW-A3", cw.seat)
        assertEquals(SeatKind.CELL, cw.seatKind)
        assertTrue(cw.backrest)

        val ch = roll.groups[1].rows[0]
        assertEquals("CH-12", ch.seat)
        assertEquals(SeatKind.CHAIR, ch.seatKind)
        assertTrue(ch.backrest)

        val floor = roll.groups[1].rows[1]
        assertEquals("14", floor.seat)
        assertEquals(SeatKind.FLOOR, floor.seatKind)
        assertFalse(floor.backrest)

        val plainFloor = roll.groups[0].rows[2]
        assertEquals("A8", plainFloor.seat)
        assertEquals(SeatKind.FLOOR, plainFloor.seatKind)

        val unseated = roll.groups[0].rows[0]
        assertEquals("", unseated.seat)
        assertTrue(unseated.unseated)
    }

    @Test
    fun serialNumbersRestartPerBlock() {
        assertEquals(listOf(1, 2, 3), roll.groups[0].rows.map { it.sn })
        assertEquals(listOf(1, 2), roll.groups[1].rows.map { it.sn })
        assertEquals(listOf(1), roll.groups[2].rows.map { it.sn })
    }

    @Test
    fun coursePairsKeepServerOrderIncludingSpl() {
        assertEquals(
            listOf("10D" to 11, "STP" to 3, "SPL" to 1),
            roll.groups[0].rows[0].courses,
        )
        assertEquals(listOf("10D" to 6, "TSC" to 1), roll.groups[0].rows[1].courses)
        // Empty history = new student; renders nothing.
        assertEquals(emptyList<Pair<String, Int>>(), roll.groups[1].rows[0].courses)
    }

    @Test
    fun plainCellsPassThroughVerbatimIncludingEmDashBlanks() {
        val r = roll.groups[0].rows[1]
        assertEquals("Mbk-2", r.room)
        assertEquals("46", r.age)
        assertEquals("Indore", r.city)
        assertEquals("C2 (2)", r.cell)
        assertEquals("Engineer", r.occupation)
        assertEquals("B.E.", r.education)
        assertEquals("Hindi, English", r.languages)

        val dash = roll.groups[1].rows[1]
        assertEquals("—", dash.occupation)
        assertEquals("—", dash.education)
    }

    @Test
    fun rowsCarryNoApplicantIdBecauseTheMarkupHasNone() {
        // zero-day.inc:1040-1048 renders plain <td> text — the SELECT's a_id
        // never reaches the markup, so the parse must not invent one.
        assertFalse(MockFixtures.teacherListHtml(1, 10).contains("appid"))
        roll.groups.flatMap { it.rows }.forEach { assertNull(it.applicantId) }
    }

    /**
     * THE load-bearing NPI assertion (spec 2b + plan Global Constraints):
     * the Comments cell is unlabelled health text. The fixture plants a
     * distinctive disclosure there; it must appear nowhere in the parsed
     * output, and the models must have no field that could hold it.
     */
    @Test
    fun commentsCellIsNeverParsedOrStored() {
        assertTrue(
            "fixture must actually carry the health text in its comments cell",
            MockFixtures.teacherListHtml(1, 10).contains(MockFixtures.TEACHER_HEALTH_NOISE),
        )
        val everything = roll.groups.joinToString("\n") { g ->
            g.toString() + g.rows.joinToString("\n") { it.toString() }
        }
        assertFalse(
            "health text from the comments cell leaked into the parse",
            everything.contains(MockFixtures.TEACHER_HEALTH_NOISE),
        )
        // Word-level sweep too, in case a future field stores a substring.
        listOf("Insulin", "olanzapine", "diabetes").forEach { word ->
            assertFalse("'$word' leaked into the parse", everything.contains(word, ignoreCase = true))
        }
        // Structural guarantee: no model field can hold the column at all.
        listOf(RollRow::class.java, RollGroup::class.java, TeacherRoll::class.java).forEach { cls ->
            cls.declaredFields.forEach { f ->
                assertFalse(
                    "${cls.simpleName}.${f.name} looks like a comments field",
                    f.name.contains("comment", ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun groupsAndRowsStayInPageOrderNeverSorted() {
        // Page order: Male Old, Male New, Female Old — a re-sort by name,
        // age or seat would break each of these positional facts.
        assertEquals(
            listOf("M-OLD-1", "M-NEW-1", "F-OLD-1"),
            roll.groups.map { it.key },
        )
        assertEquals(
            listOf("Suresh Nair", "Vikram Joshi", "Nikhil Rane"),
            roll.groups[0].rows.map { it.name },
        )
    }

    @Test
    fun educationDecodesNumericApostrophe() {
        val html = """
            <style></style>
            <table class="table-teacher-list">
            <thead><tr><th class="tl-groupinfo">AT: (unassigned) | Male | Old | Group 1 | 1 total</th></tr></thead>
            <tbody><tr>
            <td>1</td><td>Rakesh Iyer</td><td>Mbk-1</td><td>40</td><td>Pune</td>
            <td></td><td></td><td>A1</td><td>Teacher</td>
            <td>Master&#039;s in business admi</td><td>English</td><td>never-read health</td>
            </tr></tbody></table>
        """.trimIndent()
        val row = TeacherListParser.parse(html).groups.single().rows.single()
        assertEquals("Master's in business admi", row.education)
        assertEquals("Rakesh Iyer", row.name)
    }

    @Test
    fun emptyOrRefusalHtmlParsesToAnEmptyRoll() {
        assertTrue(TeacherListParser.parse("").isEmpty)
        assertTrue(TeacherListParser.parse(MockFixtures.accessDeniedHtml).isEmpty)
    }
}

/**
 * Spec 2b request-line assertion: the roll fetch is `GET
 * /teacher-list/{cid}/{courseId}` with NO query — an `r` param would run
 * server-side bulk seat auto-allocation, and the endpoint itself mutates
 * server data on GET (`zeroize_new_course_data`), so the path shape is a
 * safety property, not a formality.
 */
class TeacherListMockTest {

    private lateinit var server: MockWebServer
    private lateinit var api: StaffApi

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = DipiMockDispatcher()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .build()
            .create(StaffApi::class.java)
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun rollFetchIsAPathOnlyGetAndParses() {
        val roll = runBlocking {
            val resp = api.sheetPage("teacher-list", 3, 42)
            TeacherListParser.parse(resp.html())
        }
        val req = server.takeRequest(1, TimeUnit.SECONDS)!!
        assertEquals("GET", req.method)
        assertEquals("/teacher-list/3/42", req.path)
        assertEquals(0, req.requestUrl!!.querySize)
        assertEquals(3, roll.groups.size)
        assertEquals("Suresh Nair", roll.groups[0].rows[0].name)
    }
}
