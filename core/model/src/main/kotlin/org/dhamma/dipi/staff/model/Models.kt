package org.dhamma.dipi.staff.model

enum class Gender { M, F }

enum class ApplicantType { Student, Sevak }

enum class AuditSeverity { HARD, SAFETY, SOFT }

data class AuditFlag(
    val severity: AuditSeverity,
    val label: String,
    val detail: String,
    val ruleId: String,
)

data class CourseCount(val label: String, val n: Int)

data class ApplicantHistory(
    val first: String? = null,
    val recent: String? = null,
    val counts: List<CourseCount> = emptyList(),
)

data class ApplicantCard(
    val id: ApplicantId,
    val centreId: CentreId,
    val courseId: CourseId,
    val givenName: String,
    val middleName: String = "",
    val familyName: String,
    val gender: Gender,
    val status: ApplicantStatus,
    val type: ApplicantType,
    val oldStudent: Boolean,
    val attended: Boolean,
    val confNo: ConfNo? = null,
    val email: String? = null,
    val mobile: String? = null,
    val phoneHome: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val dob: String? = null,
    val age: Int? = null,
    val monk: Boolean = false,
    val createdAt: String? = null,
    val photoUrl: String? = null,
    val emergencyPresent: Boolean? = null,
    val history: ApplicantHistory? = null,
    val flags: List<AuditFlag> = emptyList(),
) {
    val displayName: String
        get() = listOf(givenName, middleName, familyName).filter { it.isNotBlank() }.joinToString(" ")

    val locationLine: String
        get() = listOf(city, state, country).mapNotNull { it?.takeIf(String::isNotBlank) }.joinToString(", ")

    val metaLine: String
        get() {
            val ageG = listOfNotNull(age?.toString(), gender.name).joinToString(" ")
            val loc = locationLine
            return if (loc.isBlank()) ageG else "$ageG · $loc"
        }

    val hardFlagCount: Int get() = flags.count { it.severity == AuditSeverity.HARD }
}

data class Centre(val id: CentreId, val name: String)

data class Course(
    val id: CourseId,
    val centreId: CentreId,
    val name: String,
    val start: String,
    val end: String,
    val typeKey: String = "",
)

data class Session(
    val uid: Int,
    val name: String,
    val displayName: String,
    val centres: List<Centre>,
    val modeTest: Boolean,
)

data class ApplicantListPage(
    val items: List<ApplicantCard>,
    val counts: Map<String, Int>,
    val nextCursor: String? = null,
)

data class StatusChangeResult(
    val ok: Boolean,
    val status: String,
    val msg: String,
    val confNo: String?,
    val newStatus: String?,
)

data class PhotoReviewItem(
    val applicantId: ApplicantId,
    val kind: String,
    val badge: String,
    val suggestedRotate: Int = 0,
    val suggestedCrop: Boolean = false,
)

/** Geometry-only photo correction, kept on device until upload. */
@kotlinx.serialization.Serializable
data class PhotoEdit(
    val rotate: Int = 0,
    val cropped: Boolean = false,
    val done: Boolean = false,
    val uploaded: Boolean = false,
)

sealed class OutboxOp {
    data class ChangeStatus(
        val applicantId: ApplicantId,
        val status: String,
        val letterId: Int = 0,
        val comment: String = "",
        val state: OutboxState = OutboxState.Pending,
        val message: String? = null,
    ) : OutboxOp()
}

enum class OutboxState { Pending, Synced, Failed }
