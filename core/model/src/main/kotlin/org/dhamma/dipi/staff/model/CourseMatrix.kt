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
