package org.dhamma.dipi.staff.model

/** A scraped `<select>` option — value may be a uid, course id, or event label. */
data class NamedOption(val value: String, val label: String)

data class DailyActivityForm(
    val action: String,
    val formBuildId: String,
    val formToken: String?,
    val formId: String,
    val courses: List<NamedOption>,
    val events: List<NamedOption>,
    val users: List<NamedOption>,
    val startDate: String,
    val endDate: String,
)

/** One `#table-daily-activity` row. Names stay in memory only — never persist. */
data class DailyActivityRow(
    val applicant: String,
    val course: String,
    val event: String,
    val message: String,
    val user: String,
    val at: String,
)

data class DailyActivityPage(
    val form: DailyActivityForm?,
    val rows: List<DailyActivityRow>,
)

/** One SMS-report `var dataset` row (`cid` / `course` / `count`). */
data class SmsCourseRow(
    val courseId: Int,
    val course: String,
    val count: Int,
)

/** One `/sms-count/{courseId}` fragment row. */
data class SmsLetterRow(
    val letterId: String,
    val name: String,
    val count: String,
)

/** One DataTables Editor row from `GET /course/handler/{cid}`. */
data class ManagedCourse(
    val id: Int,
    val type: String,
    val start: String,
    val end: String,
    val cancelled: Boolean,
    val status: String,
    val statusNm: String,
    val statusOm: String,
    val statusNf: String,
    val statusOf: String,
    val statusSvrM: String,
    val statusSvrF: String,
    val comments: String,
    val description: String,
    val finalized: Boolean,
)

/** Display-only values scraped from `GET /centre/{cid}/edit`. */
data class CentreFormSettings(
    val name: String = "",
    val trust: String = "",
    val address: String = "",
    val pincode: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val phone: String = "",
    val fax: String = "",
    val email: String = "",
    val website: String = "",
    val emailFrom: String = "",
    val emailReplyTo: String = "",
    val announcement: String = "",
    val preconf: Boolean? = null,
    val reconf: Boolean? = null,
    val expectedMail: Boolean? = null,
    val whatsappPreconf: Boolean? = null,
    val whatsappReconf: Boolean? = null,
    val whatsappMsg: Boolean? = null,
    val preconfDays: String = "",
    val reconfDays: String = "",
    val reconfCancelDays: String = "",
    val expectedDays: String = "",
)

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

data class LetterRow(
    val name: String,
    val status: String,
    val courseType: String,
    val subject: String,
    val body: String = "",
)
