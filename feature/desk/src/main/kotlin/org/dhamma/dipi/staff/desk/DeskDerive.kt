package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.ConfPrefix
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.Gender

/**
 * Every number the desk shows derives from the check-in records plus the
 * roster — nothing here is stored, which is why the board tiles, rail
 * counts, roll table and free-room lists cannot drift apart.
 */

/** The roll: applicants with a conf number who are not cancelled out. */
fun deskRoll(rows: List<ApplicantCard>): List<ApplicantCard> = rows.filter { card ->
    card.confNo != null && card.status.normalize() !in setOf("cancelled", "rejected", "duplicate")
}

/** UI label → gender scope; "Both" (and anything else) means no filter. */
fun deskGenderScope(label: String): Gender? = when (label) {
    "Male" -> Gender.M
    "Female" -> Gender.F
    else -> null
}

/** UI label → old/new scope; "Both" (and anything else) means no filter. */
fun deskSeniorityScope(label: String): ConfSeniority? = when (label) {
    "New" -> ConfSeniority.NEW
    "Old" -> ConfSeniority.OLD
    else -> null
}

/**
 * Restrict a list by the confirmation-number prefix. Null on an axis means
 * Both — unknown prefixes stay visible then, and drop when that axis is set.
 */
fun deskScoped(
    rows: List<ApplicantCard>,
    gender: Gender?,
    seniority: ConfSeniority?,
): List<ApplicantCard> {
    if (gender == null && seniority == null) return rows
    return rows.filter { ConfPrefix.parse(it.confNo?.value).matches(gender, seniority) }
}

/** The roll scoped to this tablet's gender and old/new — null on an axis means Both. */
fun deskRoll(
    rows: List<ApplicantCard>,
    gender: Gender?,
    seniority: ConfSeniority? = null,
): List<ApplicantCard> = deskScoped(deskRoll(rows), gender, seniority)

/**
 * The effective check-in for a card: the local record wins; a server-side
 * `attended` flag seeds one for anyone already in.
 */
fun deskRecord(card: ApplicantCard, checkIns: Map<ApplicantId, CheckInRecord>): CheckInRecord? =
    checkIns[card.id] ?: if (card.attended) CheckInRecord(checkedIn = true) else null

fun deskCheckedIn(card: ApplicantCard, checkIns: Map<ApplicantId, CheckInRecord>): Boolean =
    deskRecord(card, checkIns)?.checkedIn == true

/** Roster search + segmented filter: conf number or name, case-insensitive substring; rows sort by name. */
fun deskRosterRows(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    scan: String,
    filter: String,
): List<ApplicantCard> {
    val q = scan.trim().lowercase()
    return roll.filter { card ->
        val okQ = q.isEmpty() ||
            card.confNo?.value.orEmpty().lowercase().contains(q) ||
            card.displayName.lowercase().contains(q)
        val isIn = deskCheckedIn(card, checkIns)
        val okF = when (filter) {
            "Arrived" -> isIn
            "All" -> true
            else -> !isIn
        }
        okQ && okF
    }.sortedBy { it.displayName.lowercase() }
}

/**
 * THE ROLL table cell — old/new × M/F, derived from the conf number prefix
 * ({N|O|S}{M|F} + serial), which is why the UI never prints a
 * "New student" / "Old student" label.
 */
fun deskRollCell(roll: List<ApplicantCard>, gender: Char, old: Boolean): Int = roll.count { card ->
    val conf = card.confNo?.value?.trim().orEmpty().uppercase()
    conf.length >= 2 && conf[1] == gender && (conf[0] == 'O') == old
}

/** Room codes occupied tonight, optionally ignoring one applicant (the dialog's own row). */
fun deskOccupied(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    except: ApplicantId? = null,
): Set<String> = roll.mapNotNull { card ->
    if (card.id == except) return@mapNotNull null
    val rec = deskRecord(card, checkIns) ?: return@mapNotNull null
    rec.room.takeIf { rec.checkedIn && it.isNotBlank() }
}.toSet()

/** Free rooms in the block matching the student's gender — the dialog's pre-filtered picker list. */
fun deskFreeRooms(
    rooms: List<AccoRoom>,
    gender: Gender,
    occupied: Set<String>,
): List<AccoRoom> = rooms.filter { it.gender == gender && it.code !in occupied }

fun deskSeatCount(
    roll: List<ApplicantCard>,
    checkIns: Map<ApplicantId, CheckInRecord>,
    seat: String,
): Int = roll.count { card ->
    val rec = deskRecord(card, checkIns)
    rec?.checkedIn == true && rec.seat == seat
}

/* ── Audit ─────────────────────────────────────────────────────────── */

data class DeskFindingPerson(val card: ApplicantCard, val offendingValue: String)

data class DeskFinding(
    val code: String,
    val title: String,
    val severity: AuditSeverity,
    val mustFix: Boolean,
    val batchLabel: String?,
    val people: List<DeskFindingPerson>,
)

/** Priority order of the contractual audit codes within a severity section (mirrors audit.js — keep exactly). */
private val FINDING_ORDER = listOf(
    "phone_prefix_invalid",
    "phone_short",
    "conf_gender_mismatch",
    "conf_type_mismatch",
    "conf_no_duplicate",
    "within_file_duplicate",
    "id_missing",
    "missing_field",
    "email_missing",
    "email_malformed",
    "age_dob_mismatch",
    "age_under_min",
    "age_over_max",
    "status_unknown",
    "name_title_prefix",
    "emergency_eq_self",
    "emergency_partial",
    "cross_course_duplicate",
    "shared_mobile",
    "shared_email_unrelated",
)

private val FINDING_TITLES = mapOf(
    "phone_prefix_invalid" to "Mobile number cannot be an Indian number",
    "phone_short" to "Mobile number has fewer than 10 digits",
    "conf_gender_mismatch" to "Conf number disagrees with recorded gender",
    "conf_type_mismatch" to "Conf number prefix disagrees with sevak role",
    "conf_no_duplicate" to "Conf number appears twice in this course",
    "within_file_duplicate" to "Same person may be entered twice",
    "id_missing" to "No ID document on file",
    "missing_field" to "A required field is blank",
    "email_missing" to "Email address is blank",
    "email_malformed" to "Email address is malformed",
    "age_dob_mismatch" to "Listed age disagrees with date of birth",
    "age_under_min" to "Date of birth gives an age under 18",
    "age_over_max" to "Date of birth gives an age over 95",
    "status_unknown" to "Status value is not one the desk knows",
    "name_title_prefix" to "Honorific left in the name field",
    "emergency_eq_self" to "Emergency contact is their own mobile",
    "emergency_partial" to "Emergency contact is half-filled",
    "cross_course_duplicate" to "Also active in another course",
    "shared_mobile" to "Mobile shared with another applicant",
    "shared_email_unrelated" to "Email shared across unrelated surnames",
)

/** Section kicker labels, in display order Hard → Safety → Soft. */
val FINDING_SECTIONS: List<Pair<AuditSeverity, String>> = listOf(
    AuditSeverity.HARD to "HARD · MUST FIX",
    AuditSeverity.SAFETY to "SAFETY",
    AuditSeverity.SOFT to "SOFT · CHECK",
)

/**
 * Findings grouped by the check that fired, not by person — fix one kind of
 * mistake at a time, ordered Hard → Safety → Soft like audit.js sections.
 * Batch actions exist only where the fix is mechanical.
 */
fun deskFindings(flagged: List<ApplicantCard>): List<DeskFinding> {
    val byCode = linkedMapOf<String, MutableList<DeskFindingPerson>>()
    val severity = mutableMapOf<String, AuditSeverity>()
    for (card in flagged) {
        for (flag in card.flags) {
            val value = flag.detail.substringAfter("· ", flag.detail).trim()
            byCode.getOrPut(flag.ruleId) { mutableListOf() } += DeskFindingPerson(card, value)
            severity.merge(flag.ruleId, flag.severity) { a, b -> minOf(a, b) }
        }
    }
    return byCode.entries
        .sortedWith(
            compareBy(
                { severity.getValue(it.key) },
                { FINDING_ORDER.indexOf(it.key).let { i -> if (i < 0) FINDING_ORDER.size else i } },
            ),
        )
        .map { (code, people) ->
            val sev = severity.getValue(code)
            DeskFinding(
                code = code,
                title = FINDING_TITLES[code] ?: people.first().card.flags.first { it.ruleId == code }.label,
                severity = sev,
                mustFix = sev != AuditSeverity.SOFT,
                batchLabel = if (code == "name_title_prefix") "Strip ${people.size} honorifics" else null,
                people = people,
            )
        }
}

fun deskFindingCount(flagged: List<ApplicantCard>): Int = flagged.sumOf { it.flags.size }

fun deskMustFixCount(flagged: List<ApplicantCard>): Int =
    flagged.sumOf { card -> card.flags.count { it.severity != AuditSeverity.SOFT } }

/** Strips a leading honorific from a given name — the mechanical part of the batch fix. */
fun stripHonorific(givenName: String): String {
    // Single-word entries from the audit.js NAME_PREFIXES list (multi-word
    // titles like "Lt Col" stay manual — stripping them is not mechanical here).
    val titles = listOf(
        "sister", "brother", "mr", "mrs", "ms", "miss", "mx",
        "shri", "sri", "shree", "smt", "kumari", "kum", "master", "baby",
        "dr", "doctor", "prof", "professor", "principal", "dean",
        "er", "engineer", "adv", "advocate", "ca", "cs", "cma", "cpa",
        "architect", "ar", "counsel", "ias", "ips", "ifs", "irs", "pcs",
        "retd", "retired", "ex", "lt", "capt", "captain", "major", "maj",
        "col", "colonel", "brig", "brigadier", "gen", "general",
        "subedar", "havildar", "inspector", "si", "asi", "dsp", "sp",
        "acp", "dcp", "dig", "ig", "swami", "sadhu", "sadhvi", "acharya",
        "venerable", "rev", "reverend", "fr", "father", "pastor",
        "maulana", "mufti", "hafiz", "haji", "hajji",
        "pandit", "pt", "pujya", "guruji", "muni",
        "kunwar", "thakur", "sir", "dame", "lord", "lady",
    )
    val first = givenName.trim().substringBefore(" ")
    return if (first.lowercase().trim('.') in titles) {
        givenName.trim().substringAfter(" ", "").ifBlank { givenName }
    } else {
        givenName
    }
}

/* ── Calling ───────────────────────────────────────────────────────── */

val CALL_OUTCOMES = listOf("Reached", "No answer", "Call back")

/** The call round: everyone on the roll with a number. */
fun deskCallList(roll: List<ApplicantCard>): List<ApplicantCard> =
    roll.filter { !it.mobile.isNullOrBlank() }

fun deskCallRows(
    roll: List<ApplicantCard>,
    outcomes: Map<ApplicantId, CallRecord>,
    filter: String,
): List<ApplicantCard> = deskCallList(roll).filter { card ->
    val o = outcomes[card.id]?.outcome.orEmpty()
    if (filter == "To call") o.isBlank() else o == filter
}

/** Pile sizes for the segmented labels — "To call" plus each outcome. */
fun deskCallCounts(
    roll: List<ApplicantCard>,
    outcomes: Map<ApplicantId, CallRecord>,
): Map<String, Int> =
    (listOf("To call") + CALL_OUTCOMES).associateWith { deskCallRows(roll, outcomes, it).size }

/** The tracker's timeAgo: "just now" / "12m ago" / "3h ago" / "2d ago". */
fun deskCallAgo(thenMs: Long, nowMs: Long): String {
    val m = (nowMs - thenMs) / 60_000L
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 24 * 60 -> "${m / 60}h ago"
        else -> "${m / (24 * 60)}d ago"
    }
}

/** Row meta: "×2 · 12m ago" — null until the first attempt or outcome is logged. */
fun deskCallMeta(record: CallRecord?, nowMs: Long): String? {
    if (record == null || record.lastAttemptMs == 0L) return null
    val ago = deskCallAgo(record.lastAttemptMs, nowMs)
    return if (record.attempts > 0) "×${record.attempts} · $ago" else ago
}

/**
 * wa.me hand-off number: bare 10-digit Indian mobiles (starting 6–9) get the
 * 91 country code; anything else passes its digits through untouched.
 */
fun deskWaNumber(raw: String?): String? {
    val digits = raw.orEmpty().filter { it.isDigit() }
    if (digits.isEmpty()) return null
    return if (digits.length == 10 && digits[0] in '6'..'9') "91$digits" else digits
}

/* ── Check-in save ─────────────────────────────────────────────────── */

/** Exactly one error case exists: no room chosen. Returns text + error flag. */
fun deskSaveSnack(record: CheckInRecord, card: ApplicantCard): Pair<String, Boolean> {
    val first = card.displayName.substringBefore(" ")
    return if (record.room.isBlank()) {
        "Choose a room before checking $first in" to true
    } else {
        "✓ ${card.displayName} checked in · ${record.room} · ${record.seat}" to false
    }
}
