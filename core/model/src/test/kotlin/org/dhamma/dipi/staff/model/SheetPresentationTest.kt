package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetPresentationTest {
    @Test
    fun everyExportHasAFormatLabel() {
        SheetExport.entries.forEach { export ->
            val presented = sheetPresentation(export)
            assertTrue(export.name, presented.formatLabel.isNotBlank())
        }
    }

    @Test
    fun htmlSheetsSupportReadingWidthAndDocumentsDoNot() {
        assertTrue(sheetPresentation(SheetExport.StudentChit).supportsReadingWidth)
        assertTrue(sheetPresentation(SheetExport.CheckingSlip).supportsReadingWidth)
        assertTrue(sheetPresentation(SheetExport.Day0List).supportsReadingWidth)
        assertFalse(sheetPresentation(SheetExport.LaundryList).supportsReadingWidth)
        assertFalse(sheetPresentation(SheetExport.SeatingPlan).supportsReadingWidth)
        assertEquals("HTML · A4 portrait · 12-up chits", sheetPresentation(SheetExport.StudentChit).formatLabel)
        assertEquals("Native hall · A4 landscape print", sheetPresentation(SheetExport.SeatingPlan).formatLabel)
    }
}
