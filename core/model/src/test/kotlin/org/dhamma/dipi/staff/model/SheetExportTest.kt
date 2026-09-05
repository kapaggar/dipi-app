package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Board "Sheets & exports" labels are the seam between BoardPane and
 * the transport: every label must resolve to exactly one enum entry and
 * round-trip through [SheetExport.fromLabel].
 */
class SheetExportTest {

    private val expected = linkedMapOf(
        "Day 0 list" to SheetExport.Day0List,
        "Day 0 summary" to SheetExport.Day0Summary,
        "Student chit" to SheetExport.StudentChit,
        "Checking slip" to SheetExport.CheckingSlip,
        "Teacher list" to SheetExport.TeacherList,
        "Manager list" to SheetExport.ManagerList,
        "Laundry list" to SheetExport.LaundryList,
        "Valuable list" to SheetExport.ValuableList,
        "Seating plan" to SheetExport.SeatingPlan,
        "Course report" to SheetExport.CourseReport,
        "Course summary" to SheetExport.Day11Report,
    )

    @Test
    fun exactlyElevenExports() {
        assertEquals(11, SheetExport.entries.size)
        assertEquals(11, expected.size)
        assertNull(SheetExport.fromLabel("Male PDF"))
        assertNull(SheetExport.fromLabel("Female PDF"))
    }

    @Test
    fun chitOrderDefaultIsNamePerFrame5f() {
        assertEquals("Name", SheetSort.labelFor(SheetExport.StudentChit, SheetSort.Default))
        assertEquals("Seating plan", SheetSort.labelFor(SheetExport.StudentChit, SheetSort.SeatingOrder))
        assertEquals("Seniority", SheetSort.labelFor(SheetExport.TeacherList, SheetSort.Default))
    }

    @Test
    fun everyBoardLabelResolvesToItsEntry() {
        expected.forEach { (label, entry) ->
            assertEquals("label '$label'", entry, SheetExport.fromLabel(label))
        }
    }

    @Test
    fun everyEntryCarriesItsBoardLabel() {
        assertEquals(expected.keys.toList(), SheetExport.entries.map { it.label })
    }

    @Test
    fun fromLabelRoundTripsAllEntries() {
        SheetExport.entries.forEach { entry ->
            assertEquals(entry, SheetExport.fromLabel(entry.label))
        }
    }

    @Test
    fun unknownOrMiscasedLabelsResolveToNull() {
        assertNull(SheetExport.fromLabel("Approved list"))
        assertNull(SheetExport.fromLabel("day 0 list"))
        assertNull(SheetExport.fromLabel(""))
    }

    @Test
    fun olderCourseSummaryReportStringStillResolves() {
        assertEquals(SheetExport.Day11Report, SheetExport.fromLabel("Course summary report"))
    }
}
