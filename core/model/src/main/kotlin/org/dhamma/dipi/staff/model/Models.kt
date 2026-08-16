package org.dhamma.dipi.staff.model

@kotlinx.serialization.Serializable
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
    /** Any ID document field (Aadhaar/PAN/passport/voter ID) non-empty — presence only, never the value. */
    val idPresent: Boolean? = null,
    val emergencyNamePresent: Boolean? = null,
    /** Emergency number equals own mobile — computed at parse time, both raw values discarded. */
    val emergencyEqSelf: Boolean? = null,
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

/**
 * Aggregate counts from the centre dashboard's per-course status table.
 * Per-status counts sum the male + female student "Total" columns; [total]
 * follows the page's Total row and adds the SM/SF sevak columns on top of
 * the two student totals. Display-only — never used for any decision.
 */
data class CourseSummary(
    val received: Int = 0,
    val confirmed: Int = 0,
    val expected: Int = 0,
    val cancelled: Int = 0,
    val total: Int = 0,
)

data class Course(
    val id: CourseId,
    val centreId: CentreId,
    val name: String,
    val start: String,
    val end: String,
    val typeKey: String = "",
    /** Null when the centre page shows no status table for this course. */
    val summary: CourseSummary? = null,
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

/**
 * One arrival's Day 0 check-in, keyed by applicant id. Device-local and
 * outbox-style: the live desk has no attendance write, so these reconcile
 * to a future endpoint. Everything on screen derives from these plus the
 * roster — counts are never stored.
 */
@kotlinx.serialization.Serializable
data class CheckInRecord(
    val checkedIn: Boolean = false,
    val room: String = "",
    val seat: String = "None",
    val valuables: Boolean = true,
    val laundry: Boolean = false,
    val group: String = "1",
)

val SEAT_TYPES = listOf("Chowky", "Chair", "Backrest", "None")

@kotlinx.serialization.Serializable
data class RoomFeature(
    val geyser: Boolean = false,
    val indianToilet: Boolean = false,
    val westernToilet: Boolean = false,
)

@kotlinx.serialization.Serializable
data class AccoRoom(
    val code: String,
    val gender: Gender,
    val section: String,
    val features: RoomFeature = RoomFeature(),
    val localExample: Boolean = false,
)

@kotlinx.serialization.Serializable
data class CentreOpsPrefs(
    val laundry: Boolean = true,
    val valuables: Boolean = true,
    val groups: Boolean = false,
    val rooms: List<AccoRoom> = emptyList(),
) {
    companion object {
        fun exampleRooms(): List<AccoRoom> = listOf(
            AccoRoom("F32", Gender.F, "East", localExample = true),
            AccoRoom("M12", Gender.M, "West", localExample = true),
        )

        fun parseRoomCodes(raw: String): List<String> =
            raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}

const val MAIN_DHAMMA_HALL = "Main Dhamma Hall"

