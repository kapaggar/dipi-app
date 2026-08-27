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
}
