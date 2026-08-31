package org.dhamma.dipi.staff.model

/** One status row of the centre dashboard matrix, split the way the desk splits it. */
data class MatrixRow(
    val label: String,
    val newMale: Int = 0,
    val oldMale: Int = 0,
    val sevakMale: Int = 0,
    val newFemale: Int = 0,
    val oldFemale: Int = 0,
    val sevakFemale: Int = 0,
) {
    val maleTotal: Int get() = newMale + oldMale
    val femaleTotal: Int get() = newFemale + oldFemale
    val studentTotal: Int get() = maleTotal + femaleTotal
    val sevakTotal: Int get() = sevakMale + sevakFemale
    val isEmpty: Boolean get() = studentTotal == 0 && sevakTotal == 0
}

/**
 * Everything `course_summary()` rendered for one course. [rows] keeps the
 * desk's own order and label spelling — the status set is data-driven
 * (dh_type_detail) and must never be hardcoded here.
 */
data class CourseMatrix(
    val rows: List<MatrixRow> = emptyList(),
    val total: MatrixRow? = null,
) {
    fun row(label: String): MatrixRow? = rows.firstOrNull { it.label.equals(label, ignoreCase = true) }

    /** The three the registrar acts on, in this order, omitting any that are all-zero. */
    val highlights: List<MatrixRow> get() = HIGHLIGHT_LABELS.mapNotNull { row(it) }.filterNot { it.isEmpty }

    companion object {
        val HIGHLIGHT_LABELS = listOf("Received", "Confirmed", "Cancelled")
    }
}

/** Field-wise sum, for rows the desk keeps apart but the desk-hand reads together. */
operator fun MatrixRow.plus(other: MatrixRow?): MatrixRow {
    if (other == null) return this
    return copy(
        newMale = newMale + other.newMale,
        oldMale = oldMale + other.oldMale,
        sevakMale = sevakMale + other.sevakMale,
        newFemale = newFemale + other.newFemale,
        oldFemale = oldFemale + other.oldFemale,
        sevakFemale = sevakFemale + other.sevakFemale,
    )
}

/**
 * The fixed card rows, in order; absent statuses become empty rows so every
 * card is the same height. The Total row is [CourseMatrix.total] and the card
 * renders it separately.
 */
val CourseMatrix.cardRows: List<MatrixRow>
    get() {
        val confirmedPlusExpected =
            (row("Confirmed") ?: MatrixRow("Confirmed")) + row("Expected")
        return listOf(
            row("Received") ?: MatrixRow("Received"),
            confirmedPlusExpected.copy(label = "Confirmed + Expected"),
            row("Cancelled") ?: MatrixRow("Cancelled"),
        )
    }
