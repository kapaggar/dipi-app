package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicantId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendedTableParserTest {

    private fun row(
        id: Int,
        conf: String,
        name: String,
        room: String,
        laundry: String = "",
        valuable: String = "",
        chowky: String = "No",
        chair: String = "No",
        backrest: String = "No",
        group: String = "1",
    ): String = """
        <tr>
          <td>$id</td><td>$conf</td>
          <td><a href="/app/$id/edit" appid="$id">$name</a></td>
          <td>Female</td><td>Student</td><td>34</td><td>4</td><td>0</td>
          <td>$room</td><td>$laundry</td><td>$valuable</td>
          <td>$chowky</td><td>$chair</td><td>$backrest</td>
          <td>$group</td><td>||||</td>
        </tr>
    """.trimIndent()

    private fun page(table: String) = """
        <html><body>
        <div id="day-summary"><table id="table-conf"></table></div>
        <h2>Attended Applicants</h2>
        $table
        </body></html>
    """.trimIndent()

    private val header =
        "<thead><tr><th>Update</th><th>ConfNo</th><th>Name</th><th>Gender</th>" +
            "<th>Type</th><th>Age</th><th>Teen/10D/STP</th><th>LC</th><th>RoomNo</th>" +
            "<th>Laundry</th><th>Valuable</th><th>Chowky</th><th>Chair</th>" +
            "<th>BackRest</th><th>Group</th><th>H</th></tr></thead>"

    @Test
    fun fixtureRowBecomesACheckedInSyncedRecord() {
        val html = page(
            """<table id="table-attending">$header<tbody>
            ${row(99, "OF128", "Meera Deshpande", "Fbk-36", laundry = "17", chowky = "Yes", group = "1")}
            </tbody></table>""",
        )
        val rec = AttendedTableParser.parse(html).getValue(ApplicantId(99))
        assertTrue(rec.checkedIn)
        assertTrue(rec.synced)
        assertEquals("Fbk 36", rec.room)
        assertEquals("Chowky", rec.seat)
        assertEquals("1", rec.group)
        assertTrue(rec.laundry)
        assertFalse(rec.valuables)
    }

    @Test
    fun emptyAttendingTableIsEmpty() {
        assertTrue(AttendedTableParser.parse(MockFixtures.zeroDayEmptyAttendingHtml(1, 10)).isEmpty())
    }

    @Test
    fun missingTableIsEmpty() {
        assertTrue(AttendedTableParser.parse("<html><body><p>no table</p></body></html>").isEmpty())
    }

    @Test
    fun dashOnlyRoomStillCheckedIn() {
        val html = page(
            """<table id="table-attending">$header<tbody>
            ${row(7, "NF1", "Meera Deshpande", "-")}
            </tbody></table>""",
        )
        val rec = AttendedTableParser.parse(html).getValue(ApplicantId(7))
        assertTrue(rec.checkedIn)
        assertEquals("", rec.room)
        assertTrue(rec.synced)
    }

    @Test
    fun namesNeverAppearInReturnedFields() {
        val html = page(
            """<table id="table-attending">$header<tbody>
            ${row(1, "OF128", "Meera Deshpande", "Fbk-1")}
            ${row(4, "OM42", "Suresh Nair", "Mbk-8", chowky = "Yes", group = "2")}
            </tbody></table>""",
        )
        val parsed = AttendedTableParser.parse(html)
        assertEquals(setOf(ApplicantId(1), ApplicantId(4)), parsed.keys)
        val blob = parsed.values.joinToString { rec ->
            listOf(rec.room, rec.seat, rec.group, rec.syncedAt.orEmpty()).joinToString()
        }
        assertFalse(blob.contains("Meera", ignoreCase = true))
        assertFalse(blob.contains("Deshpande", ignoreCase = true))
        assertFalse(blob.contains("Suresh", ignoreCase = true))
        assertFalse(blob.contains("Nair", ignoreCase = true))
    }
}
