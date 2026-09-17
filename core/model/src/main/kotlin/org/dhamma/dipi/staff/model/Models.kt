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

/** Standalone dash tokens are empty-surname placeholders, not a family name. */
private val DASH_ONLY_NAME = Regex("""^[—–−-]+$""")

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
    val courseFinalized: Boolean = false,
    val historicalRoom: String = "",
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
        get() = listOf(givenName, middleName, familyName)
            .filter { it.isNotBlank() && !it.matches(DASH_ONLY_NAME) }
            .joinToString(" ")

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
    /** Null when the centre page shows no status table for this course. */
    val summary: CourseSummary? = null,
    /** The full centre-dashboard status matrix; null when the desk did not render the block. */
    val matrix: CourseMatrix? = null,
)

/** Split of the centre dashboard: next-4 upcoming vs the Select Course older rows. */
data class CentreCourses(
    val upcoming: List<Course>,
    val older: List<Course> = emptyList(),
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

/**
 * One arrival's Day 0 check-in, keyed by applicant id. Device-local truth;
 * everything on screen derives from these plus the roster — counts are
 * never stored. Owner amendment (2026-08-16): records replicate to the
 * desk's own allocation update (`POST /app-update-attended/{id}`) via a
 * user-initiated bulk sync — [synced]/[syncedAt] are that bookkeeping, and
 * any edit clears them (see [clearSyncedIfChanged]).
 */
@kotlinx.serialization.Serializable
data class CheckInRecord(
    val checkedIn: Boolean = false,
    val room: String = "",
    val seat: String = "None",
    val valuables: Boolean = true,
    val laundry: Boolean = false,
    val group: String = "1",
    /** True once the server accepted this exact record; false whenever it changes. */
    val synced: Boolean = false,
    val syncedAt: String? = null,
)

val SEAT_TYPES = listOf("Chowky", "Chair", "Backrest", "None")

@kotlinx.serialization.Serializable
data class RoomFeature(
    val geyser: Boolean = false,
    val indianToilet: Boolean = false,
    val westernToilet: Boolean = false,
)

/**
 * One accommodation room from the centre's server config
 * (`dh_center_setting_acco` via `GET /centre/{cid}/acco-handler`).
 * [code] is "<section> <number>" — the same Section + Room No pair the desk's
 * Update dialog sends — and is what a [CheckInRecord.room] stores. Rooms are
 * read-only in the app; the desk site owns the list.
 */
@kotlinx.serialization.Serializable
data class AccoRoom(
    val code: String,
    val gender: Gender,
    val section: String,
    val features: RoomFeature = RoomFeature(),
    /** Room number within its section, for chart display; falls back to [code]. */
    val number: String = "",
) {
    val displayNo: String get() = number.ifBlank { code }

    /** Chart amenity mark under the room number: G geyser · IC Indian commode · W Western toilet. */
    val amenityMark: String
        get() = buildList {
            if (features.geyser) add("G")
            if (features.indianToilet) add("IC")
            if (features.westernToilet) add("W")
        }.joinToString(" ")
}

@kotlinx.serialization.Serializable
data class CentreOpsPrefs(
    val laundry: Boolean = true,
    val valuables: Boolean = true,
    val groups: Boolean = false,
    /** Offline cache of the server room config — replaced on every centre-page load. */
    val rooms: List<AccoRoom> = emptyList(),
    /** Chart grid shape per gender+section block. Device-local; wiped by Erase-all. */
    val roomLayout: RoomLayout = RoomLayout(),
    /**
     * Live-desk Hall Settings from `GET /centre/{cid}/edit` (read-only).
     * Main Plan columns wrap the Room Chart and seed the seating grid when
     * a block has no local SAVE ROOM LAYOUT. Combined / naming / chowky
     * stay seating facts; no POST.
     */
    val hallSettings: CentreHallSettings = CentreHallSettings(),
    /**
     * The centre's own reconfirmation wording for the calling round's WhatsApp
     * hand-off. Blank uses [WHATSAPP_DEFAULT_TEMPLATE]; see [whatsAppMessage]
     * for the tokens.
     */
    val whatsAppTemplate: String = "",
    /**
     * Hall-grid shape per gender for the seating plan (spec 2c S1), keyed by
     * [Gender.name]. Registrar-configured device-locally; wiped by Erase-all
     * with the rest of centre_ops. Read via [hallGridFor], write via
     * [withHallGrid] — both clamp (RoomLayout pattern).
     */
    val hallGrid: Map<String, HallGrid> = emptyMap(),
) {
    /**
     * Seating grid for [gender]. Desk Main Plan columns and chowky width win
     * when `GET /centre/{cid}/edit` sent them; local SAVE HALL LAYOUT fills
     * depth and any field the edit page omitted. Never invents a POST.
     */
    fun hallGridFor(gender: Gender): HallGrid {
        val local = (hallGrid[gender.name] ?: HallGrid())
        val desk = hallSettings.plan(gender)
        return HallGrid(
            columns = desk.columns ?: local.columns,
            depth = local.depth,
            chowkyRail = desk.chowkyRailOrNull() ?: local.chowkyRail,
        ).clamped()
    }

    fun withHallGrid(gender: Gender, grid: HallGrid): CentreOpsPrefs =
        copy(hallGrid = hallGrid + (gender.name to grid.clamped()))

    fun roomColumns(gender: Gender, section: String): Int =
        roomLayout.columnsFor(gender, section, hallSettings.seatsPerRow(gender))
}

/**
 * One gender's Main Plan on `/centre/{cid}/edit` (`seatcfg_{male|female}_*`).
 * Nulls mean that input was missing. Blank direction / chowky position is
 * the desk's "Default (natural side)" - male right / LTR, female left / RTL.
 */
@kotlinx.serialization.Serializable
data class HallSeatPlan(
    val columns: Int? = null,
    val chowkyColumns: Int? = null,
    val direction: String? = null,
    val chowkyPosition: String? = null,
    val emptySeats: String? = null,
    val emptyChowky: String? = null,
) {
    fun isPresent(): Boolean =
        columns != null ||
            chowkyColumns != null ||
            direction != null ||
            chowkyPosition != null ||
            emptySeats != null ||
            emptyChowky != null

    fun chowkyRailOrNull(): ChowkyRailLayout? {
        if (chowkyColumns == null && chowkyPosition == null) return null
        val pos = chowkyPosition?.lowercase().orEmpty()
        if (pos == "back" || (chowkyColumns != null && chowkyColumns > 1)) {
            return ChowkyRailLayout.WRAP
        }
        return ChowkyRailLayout.SINGLE_ROW
    }

    fun emptySeatCount(): Int = emptyPairCount(emptySeats) + emptyPairCount(emptyChowky)

    fun directionLabel(naturalSide: String): String = when (direction?.lowercase()) {
        "right" -> "Left to right"
        "left" -> "Right to left"
        "" -> "Default ($naturalSide)"
        else -> "not set"
    }

    fun chowkyPositionLabel(naturalSide: String): String = when (chowkyPosition?.lowercase()) {
        "left" -> "Left"
        "right" -> "Right"
        "back" -> "Back"
        "" -> "Default ($naturalSide)"
        else -> "not set"
    }

    fun summaryLine(naturalSide: String): String {
        val cols = columns?.let { "$it columns" } ?: "columns not set"
        val cho = chowkyColumns?.let { "$it chowky" } ?: "chowky not set"
        return "$cols · $cho · ${directionLabel(naturalSide)} · " +
            "Chowky ${chowkyPositionLabel(naturalSide)} · Empty seats ${emptySeatCount()}"
    }
}

/**
 * Hall Settings on `/centre/{cid}/edit`: combined hall, seat naming, and
 * the visual Main Plan (`seatcfg_*` columns, chowky, direction, position,
 * empty seats). [maleSeatsPerRow] / [femaleSeatsPerRow] stay as the
 * persisted seats-per-row aliases so an older centre_ops blob still
 * decodes. Inventory still comes from acco-handler. Nulls mean the edit
 * page did not send that field. The INI `cs_seat_config` is assembled
 * only on POST - live GET HTML does not include it.
 */
@kotlinx.serialization.Serializable
data class CentreHallSettings(
    val combinedHall: Boolean? = null,
    val seatNaming: Int? = null,
    val maleSeatsPerRow: Int? = null,
    val femaleSeatsPerRow: Int? = null,
    val malePlan: HallSeatPlan = HallSeatPlan(),
    val femalePlan: HallSeatPlan = HallSeatPlan(),
) {
    fun seatsPerRow(gender: Gender): Int? = when (gender) {
        Gender.M -> malePlan.columns ?: maleSeatsPerRow
        Gender.F -> femalePlan.columns ?: femaleSeatsPerRow
    }

    fun plan(gender: Gender): HallSeatPlan = when (gender) {
        Gender.M -> if (malePlan.columns == null && maleSeatsPerRow != null) {
            malePlan.copy(columns = maleSeatsPerRow)
        } else {
            malePlan
        }
        Gender.F -> if (femalePlan.columns == null && femaleSeatsPerRow != null) {
            femalePlan.copy(columns = femaleSeatsPerRow)
        } else {
            femalePlan
        }
    }

    fun isPresent(): Boolean =
        combinedHall != null ||
            seatNaming != null ||
            maleSeatsPerRow != null ||
            femaleSeatsPerRow != null ||
            malePlan.isPresent() ||
            femalePlan.isPresent()

    fun combinedLabel(): String = when (combinedHall) {
        true -> "Yes"
        false -> "No"
        null -> "not set"
    }

    fun seatNamingLabel(): String = when (seatNaming) {
        0 -> "Numerical (running number)"
        1 -> "Alphanumeric - columns A, B, C / rows 1, 2, 3"
        2 -> "Alphanumeric - rows A, B, C / columns 1, 2, 3"
        else -> "not set"
    }
}

private fun emptyPairCount(raw: String?): Int {
    if (raw.isNullOrBlank()) return 0
    return raw.split(',').count { it.trim().matches(Regex("""\d+\s*-\s*\d+""")) }
}

const val MAIN_DHAMMA_HALL = "Main Dhamma Hall"

