package org.dhamma.dipi.staff.model

/**
 * The teacher roll — `GET /teacher-list/{cid}/{courseId}` parsed
 * (dh_manageapp/inc/zero-day.inc:877-1072). Pure parse models, deliberately
 * NOT `@Serializable`: persistence is a separate snapshot DTO owned by the
 * course store (spec 2d). Grouping and row order are the page's own —
 * seniority order is meaning, so consumers must never re-sort or re-group.
 *
 * NPI discipline: the web table's 12th column (Comments) is an unlabelled
 * concatenation of health text. By construction there is NO field for it
 * anywhere in these models — the parser never reads that cell.
 */
data class TeacherRoll(val groups: List<RollGroup>) {
    val isEmpty: Boolean get() = groups.isEmpty()

    /** Page band totals, summed — the header's "N on the roll". */
    val rollCount: Int get() = groups.sumOf { it.total }
}

/** Old/New comes ONLY from the block band — no row carries it. */
enum class RollSeniority { OLD, NEW }

/** Seat prefixes: `CW-` chowky/cell, `CH-` chair; everything else sits on the floor. */
enum class SeatKind { CELL, CHAIR, FLOOR }

/**
 * One `<table class="table-teacher-list">` block: (gender, old/new, group),
 * described by its `tl-groupinfo` band. [at] can be the literal
 * `(unassigned)`, in which case [code] is null.
 */
data class RollGroup(
    val at: String,
    val code: String?,
    val gender: Gender,
    val seniority: RollSeniority,
    val group: String,
    val total: Int,
    val rows: List<RollRow>,
) {
    /** Stable client-side key for filtering — never shown to the user. */
    val key: String get() = "${gender.name}-${seniority.name}-$group"

    /** The band's own AT token, re-joined: `AT: {name} [{code}]` / `AT: (unassigned)`. */
    val atLine: String get() = if (code != null) "AT: $at [$code]" else "AT: $at"

    val genderWord: String get() = if (gender == Gender.F) "Female" else "Male"

    val seniorityWord: String get() = if (seniority == RollSeniority.OLD) "Old" else "New"

    /** Frame 2b qualifier: the band tokens ` · ` joined. */
    val qualifier: String get() = "$genderWord · $seniorityWord · Group $group"
}

/**
 * One student row, in the page's 12-column order minus what the client never
 * reads. S/N restarts per block. Blank cells pass through verbatim (the page
 * uses em-dashes and empty strings interchangeably).
 *
 * [applicantId] is always null from the parse: the rendered row carries no id
 * attribute and no link (zero-day.inc:1040-1048 — plain `<td>` text; the
 * SELECT's `a_id` never reaches the markup). Wave 2 maps ids via the
 * worklist / zero-day merge.
 */
data class RollRow(
    val sn: Int,
    val applicantId: ApplicantId? = null,
    val name: String,
    val roleTag: String? = null,
    val room: String,
    val age: String,
    val city: String,
    /** Ordered non-zero course counts, keys ⊂ {10D STP SPL TSC 20D 30D 45D 60D}. Empty = new student. */
    val courses: List<Pair<String, Int>>,
    val cell: String,
    /** Display string verbatim incl. any `CW-`/`CH-` prefix; blank = unseated. */
    val seat: String,
    /** Single source of the CW/CH/floor rule — `SeatGrid` reads this, never the prefix. */
    val seatKind: SeatKind,
    val backrest: Boolean,
    val occupation: String,
    val education: String,
    val languages: String,
) {
    val unseated: Boolean get() = seat.isBlank()
}
