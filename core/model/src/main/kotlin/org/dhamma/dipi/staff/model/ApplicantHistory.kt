package org.dhamma.dipi.staff.model

data class ApplicantCourseRow(
    val course: String,
    val type: String,
    val status: String,
    val attended: String,
    val address: String,
)

data class ApplicantActivityRow(
    val at: String,
    val activity: String,
    val user: String,
)

data class ApplicantClarificationRow(
    val at: String,
    val message: String,
    val fileLabel: String,
    val clarId: Int? = null,
)

/** In-memory lazy history for one applicant. Null list = not fetched yet. */
data class ApplicantDeskHistory(
    val courses: List<ApplicantCourseRow>? = null,
    val activity: List<ApplicantActivityRow>? = null,
    val clarifications: List<ApplicantClarificationRow>? = null,
    val loading: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
)
