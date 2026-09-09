package org.dhamma.dipi.staff.model

enum class SheetScreenWidth { FIT, READABLE }

data class SheetPresentation(
    val formatLabel: String,
    val supportsReadingWidth: Boolean,
)

fun sheetPresentation(export: SheetExport): SheetPresentation = when (export) {
    SheetExport.Day0List -> SheetPresentation("HTML · A4 portrait · Day 0 Contact hidden in print", true)
    SheetExport.Day0Summary -> SheetPresentation("Native screen · counts only", false)
    SheetExport.StudentChit -> SheetPresentation("HTML · A4 portrait · 12-up chits", true)
    SheetExport.CheckingSlip -> SheetPresentation("HTML · A4 portrait · 2-up slips", true)
    SheetExport.TeacherList -> SheetPresentation("HTML · A4 portrait", true)
    SheetExport.ManagerList -> SheetPresentation("HTML · A4 portrait · allowed contact fields retained", true)
    SheetExport.LaundryList -> SheetPresentation("Excel · opens in an external viewer", false)
    SheetExport.ValuableList -> SheetPresentation("Excel · opens in an external viewer", false)
    SheetExport.SeatingPlan -> SheetPresentation("Native hall · A4 landscape print", false)
    SheetExport.CourseReport -> SheetPresentation("Native table · last successful report", false)
    SheetExport.Day11Report -> SheetPresentation("PDF · opens in an external viewer", false)
}
