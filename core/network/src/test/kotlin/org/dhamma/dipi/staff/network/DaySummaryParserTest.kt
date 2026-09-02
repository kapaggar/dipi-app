package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The `#day-summary` fragment of `GET /zero-day/{cid}/{courseId}` (v5 T2).
 *
 * The fixture below reproduces the desk's real shape from
 * `version-5/HAR-ROUTES.md` § `zero-day`, **including the unclosed `<b>` in
 * the `Total` cells**. That malformation is not hypothetical — it is what the
 * template emits, browsers recover from it, and a tag-matching parser would
 * swallow the rest of the row.
 */
class DaySummaryParserTest {

    private val fragment = """
        <div id="day-summary">
          <table id="table-conf">
            <tr><th></th><th>Old</th><th>New</th><th>Total</th><th>Server</th></tr>
            <tr><td>Confirmed Male</td><td>18</td><td>27</td><td><b>45</td><td>3</td></tr>
            <tr><td>Confirmed Female</td><td>14</td><td>22</td><td><b>36</td><td>2</td></tr>
            <tr><td>Total</td><td>32</td><td>49</td><td><b>81</td><td>5</td></tr>
          </table>
          <table id="table-totals">
            <tr><th></th><th>Old</th><th>New</th><th>Total</th><th>Server</th></tr>
            <tr><td>Attended Male</td><td>1</td><td>0</td><td><b>1</td><td>0</td></tr>
            <tr><td>Attended Female</td><td>0</td><td>0</td><td><b>0</td><td>0</td></tr>
            <tr><td>Total</td><td>1</td><td>0</td><td><b>1</td><td>0</td></tr>
          </table>
          <table id="table-special">
            <tr><th></th><th>Chowky</th><th>Chair</th><th>Backrest</th></tr>
            <tr><td>Male</td><td>1 (O) + 1 (N)</td><td>0 (O) + 2 (N)</td><td>3 (O) + 0 (N)</td></tr>
            <tr><td>Female</td><td>0 (O) + 0 (N)</td><td>1 (O) + 0 (N)</td><td>0 (O) + 1 (N)</td></tr>
            <tr><td>Total</td><td>1 (O) + 1 (N)</td><td>1 (O) + 2 (N)</td><td>3 (O) + 1 (N)</td></tr>
          </table>
        </div>
    """.trimIndent()

    @Test
    fun parsesTheThreeFixedTables() {
        val s = DaySummaryParser.parse(fragment)

        assertEquals(18, s.confirmed.male.old)
        assertEquals(27, s.confirmed.male.new)
        assertEquals(45, s.confirmed.male.total)
        assertEquals(3, s.confirmed.male.server)
        assertEquals(36, s.confirmed.female.total)
        assertEquals(81, s.confirmed.total.total)

        assertEquals(1, s.attended.male.total)
        assertEquals(0, s.attended.female.total)
        assertEquals(1, s.attended.total.total)

        // Derived, not fetched — the desk never states this number.
        assertEquals(80, s.stillToArrive)
    }

    /**
     * `<td><b>45</td>` — the `<b>` is never closed. Reading the cell as text
     * rather than matching tags makes the malformation inert.
     */
    @Test
    fun survivesTheUnclosedBoldInTotalCells() {
        val s = DaySummaryParser.parse(fragment)
        // If the unclosed <b> swallowed the row, Server would read as 0 and
        // Total would pick up the wrong figure.
        assertEquals(45, s.confirmed.male.total)
        assertEquals(3, s.confirmed.male.server)
        assertEquals(81, s.confirmed.total.total)
        assertEquals(5, s.confirmed.total.server)
    }

    @Test
    fun parsesSpecialSeatingOldNewPairs() {
        val s = DaySummaryParser.parse(fragment)
        assertEquals(1, s.specialSeating.male.chowky.old)
        assertEquals(1, s.specialSeating.male.chowky.new)
        assertEquals(0, s.specialSeating.male.chair.old)
        assertEquals(2, s.specialSeating.male.chair.new)
        assertEquals(3, s.specialSeating.male.backrest.old)
        assertEquals(0, s.specialSeating.male.backrest.new)
        assertEquals(0, s.specialSeating.female.chowky.sum)
        assertEquals(4, s.specialSeating.total.backrest.sum)
    }

    @Test
    fun missingFragmentYieldsAnEmptySummary() {
        val s = DaySummaryParser.parse("<div id=\"day-summary\"></div>")
        assertEquals(0, s.confirmed.total.total)
        assertEquals(0, s.attended.total.total)
        assertEquals(0, s.stillToArrive)
        assertTrue(s.isEmpty)
        assertTrue(s.specialSeating.isEmpty)
    }

    /** Day −1: the real page is all zeros, and that is an answer, not a fault. */
    @Test
    fun allZeroesParseAsZeroesNotAsAFailure() {
        val zeros = fragment
            .replace(Regex("""<td>\d+</td>"""), "<td>0</td>")
            .replace(Regex("""<td><b>\d+</td>"""), "<td><b>0</td>")
            .replace(Regex("""\d+ \(O\) \+ \d+ \(N\)"""), "0 (O) + 0 (N)")
        val s = DaySummaryParser.parse(zeros)
        assertTrue(s.isEmpty)
        assertEquals(0, s.stillToArrive)
    }

    /** A bare figure with no `(O)`/`(N)` marker is kept, not dropped. */
    @Test
    fun anUnmarkedSpecialCellStillCounts() {
        assertEquals(2, DaySummaryParser.oldNew("2").sum)
        assertEquals(0, DaySummaryParser.oldNew("").sum)
        assertEquals(0, DaySummaryParser.oldNew(null).sum)
    }
}
