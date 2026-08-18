package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LettersParserTest {
    @Test
    fun listsNameStatusTypeSubjectAndDropsActionCells() {
        val rows = LettersParser.letters(MockFixtures.lettersHtml(1))
        assertEquals(2, rows.size)
        assertEquals("Confirmed", rows[0].name)
        assertEquals("Confirmed", rows[0].status)
        assertEquals("10-Day", rows[0].courseType)
        assertEquals("Your place is confirmed", rows[0].subject)
        assertEquals("Dear meditator, your place is confirmed.", rows[0].body)
        assertFalse(rows[0].name.contains("Edit"))
        assertEquals("All", rows[1].courseType)
    }

    @Test
    fun emptyAndMissingTable() {
        assertTrue(LettersParser.letters("<html></html>").isEmpty())
        assertTrue(LettersParser.letters("<table id=\"table-letters-d\"></table>").isEmpty())
    }
}
