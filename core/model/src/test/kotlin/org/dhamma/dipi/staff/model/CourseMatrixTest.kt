package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseMatrixTest {

    private val confirmed = MatrixRow("Confirmed", newMale = 7, oldMale = 3, sevakMale = 1, newFemale = 3, oldFemale = 0, sevakFemale = 0)

    @Test
    fun rowDerivesItsOwnTotals() {
        assertEquals(10, confirmed.maleTotal)
        assertEquals(3, confirmed.femaleTotal)
        assertEquals(13, confirmed.studentTotal)
        assertEquals(1, confirmed.sevakTotal)
        assertFalse(confirmed.isEmpty)
    }

    @Test
    fun aRowWithNoStudentsAndNoSevaksIsEmpty() {
        assertTrue(MatrixRow("Errors").isEmpty)
        assertFalse(MatrixRow("Errors", sevakFemale = 1).isEmpty)
    }

    @Test
    fun lookupIsCaseInsensitiveAndMissingRowsAreNull() {
        val m = CourseMatrix(rows = listOf(confirmed))
        assertEquals(confirmed, m.row("confirmed"))
        assertNull(m.row("WaitList"))
    }

    @Test
    fun highlightsKeepSpecOrderAndDropEmptyRows() {
        val m = CourseMatrix(
            rows = listOf(
                MatrixRow("Cancelled", newMale = 2),
                MatrixRow("Received"),
                confirmed,
                MatrixRow("Expected", newMale = 44),
            ),
        )
        // Received is present but all-zero, so it drops; Expected is not a highlight.
        assertEquals(listOf("Confirmed", "Cancelled"), m.highlights.map { it.label })
    }

    @Test
    fun anEmptyMatrixHasNoHighlights() {
        assertEquals(emptyList<MatrixRow>(), CourseMatrix().highlights)
    }

    @Test
    fun plusSumsEveryCountAndKeepsTheReceiverLabel() {
        val a = MatrixRow("Confirmed", newMale = 7, oldMale = 3, sevakMale = 1, newFemale = 3)
        val b = MatrixRow("Expected", newMale = 44, oldMale = 19, newFemale = 19, oldFemale = 10, sevakFemale = 1)
        val sum = a + b
        assertEquals("Confirmed", sum.label)
        assertEquals(51, sum.newMale)
        assertEquals(22, sum.oldMale)
        assertEquals(1, sum.sevakMale)
        assertEquals(22, sum.newFemale)
        assertEquals(10, sum.oldFemale)
        assertEquals(1, sum.sevakFemale)
        assertEquals(73, sum.maleTotal)
    }

    @Test
    fun plusNullIsTheReceiver() {
        val a = MatrixRow("Confirmed", newMale = 7)
        assertEquals(a, a + null)
    }

    @Test
    fun cardRowsAlwaysHasFourRowsInOrderEvenWhenStatusesAreAbsent() {
        val empty = CourseMatrix().cardRows
        assertEquals(listOf("Received", "Confirmed + Expected", "Cancelled"), empty.map { it.label })
        assertEquals(3, empty.size)
        assertTrue(empty.all { it.isEmpty })
    }

    @Test
    fun cardRowsMergesConfirmedAndExpected() {
        val m = CourseMatrix(
            rows = listOf(
                MatrixRow("Received", oldFemale = 1),
                MatrixRow("Confirmed", newMale = 7, oldMale = 3),
                MatrixRow("Expected", newMale = 44, oldMale = 19),
                MatrixRow("Cancelled", newMale = 10),
            ),
        )
        val rows = m.cardRows
        assertEquals(listOf("Received", "Confirmed + Expected", "Cancelled"), rows.map { it.label })
        assertEquals(51, rows[1].newMale)
        assertEquals(22, rows[1].oldMale)
        assertEquals(73, rows[1].maleTotal)
        assertEquals(1, rows[0].oldFemale)
        assertEquals(10, rows[2].newMale)
    }

    @Test
    fun cardRowsKeepsAnAbsentStatusAsAnEmptyRowRatherThanDroppingIt() {
        val m = CourseMatrix(rows = listOf(MatrixRow("Confirmed", newMale = 5)))
        val rows = m.cardRows
        assertEquals(3, rows.size)
        assertTrue(rows[0].isEmpty)   // Received absent
        assertEquals(5, rows[1].newMale)
        assertTrue(rows[2].isEmpty)   // Cancelled absent
    }
}
