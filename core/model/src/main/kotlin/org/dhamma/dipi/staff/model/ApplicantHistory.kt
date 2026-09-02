package org.dhamma.dipi.staff.model

const val HISTORY_COURSES = "courses"
const val HISTORY_ACTIVITY = "activity"
const val HISTORY_CLARIFICATIONS = "clarifications"

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

/**
 * In-memory lazy history for one applicant. Null list = not fetched yet.
 *
 * [expanded] is the open/closed state of each section, deliberately separate
 * from "fetched": a section the desk closes keeps its rows cached, so
 * reopening costs no second request.
 */
data class ApplicantDeskHistory(
    val courses: List<ApplicantCourseRow>? = null,
    val activity: List<ApplicantActivityRow>? = null,
    val clarifications: List<ApplicantClarificationRow>? = null,
    val loading: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
    val expanded: Set<String> = emptySet(),
)

/** Every section key the desk can open, in render order. */
val HISTORY_KEYS = listOf(HISTORY_COURSES, HISTORY_ACTIVITY, HISTORY_CLARIFICATIONS)

/** True once [key]'s rows have arrived, whether or not the section is open. */
fun ApplicantDeskHistory.isLoaded(key: String): Boolean = when (key) {
    HISTORY_COURSES -> courses != null
    HISTORY_ACTIVITY -> activity != null
    HISTORY_CLARIFICATIONS -> clarifications != null
    else -> false
}

/**
 * Open ⇄ closed after the desk taps [key]'s header. Closing keeps the fetched
 * rows, so reopening redraws them instead of asking the desk site again.
 */
fun ApplicantDeskHistory.toggled(key: String): ApplicantDeskHistory =
    if (key in expanded) copy(expanded = expanded - key) else copy(expanded = expanded + key)

/**
 * True when this tap opens a section whose rows have never arrived — the only
 * case that costs a request. A tap that closes, or that reopens cached rows,
 * fetches nothing; an unknown key fetches nothing either.
 */
fun ApplicantDeskHistory.tapNeedsFetch(key: String): Boolean =
    key in HISTORY_KEYS && key !in expanded && !isLoaded(key)
