package org.dhamma.dipi.staff.audit

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity

/**
 * Client-side port of the course-audit rule engine (callconfirm/course-audit
 * audit.js). RuleIds are contractual — they must match audit.js check names
 * exactly, because server flags merge on ruleId.
 *
 * Rules that need raw ID values (aadhar_masked, aadhar_length, pan_invalid,
 * id_type_*) are NOT ported: the app never parses-and-stores NPI. The only
 * ID-derived signal is the parse-time presence boolean behind id_missing.
 * cross_course_duplicate needs cross-course caching and is server-only here.
 */
object ClientAudit {
    /** audit.js dates against courseStart; the card carries no course dates, so year-of-writing. */
    private const val NOW_YEAR = 2026
    private const val MIN_AGE = 18
    private const val MAX_AGE = 95

    /** audit.js ACTIVE — every rule except status_unknown runs on these rows only. */
    private val ACTIVE = setOf("expected", "confirmed")

    /**
     * audit.js KNOWN_STATUS plus the live-desk statuses the xlsx export never
     * carried (Pending / Reconfirmation are offered by /change-status itself).
     */
    private val KNOWN_STATUSES = setOf(
        "received", "review", "clarification", "clarification-response",
        "preconfirmation", "confirmed", "expected", "waitlist",
        "duplicate", "rejected", "regret", "cancelled", "attended", "left", "errors",
        "pending", "reconfirmation",
    )

    /**
     * audit.js NAME_PREFIXES (plus Sister/Brother, which the desk sees).
     * Sorted longest-first so multi-word titles win ("Lt Col" before "Lt").
     */
    private val NAME_PREFIXES = listOf(
        "Mr", "Mrs", "Ms", "Miss", "Mx",
        "Shri", "Sri", "Shree", "Smt", "Kumari", "Kum", "Master", "Baby",
        "Dr", "Doctor", "Prof", "Professor", "Asst Prof", "Associate Prof",
        "Principal", "Dean", "Er", "Engineer", "Adv", "Advocate",
        "CA", "CS", "CMA", "CPA", "Architect", "Ar", "Counsel",
        "IAS", "IPS", "IFS", "IRS", "PCS",
        "Retd", "Retired", "Ex",
        "Lt", "Capt", "Captain", "Major", "Maj", "Col", "Colonel",
        "Brig", "Brigadier", "Gen", "General", "Subedar", "Havildar",
        "Inspector", "SI", "ASI", "DSP", "SP", "ACP", "DCP", "DIG", "IG",
        "Lt Col", "Maj Gen", "Brig Gen", "Retd Col", "Col Retd",
        "Swami", "Sadhu", "Sadhvi", "Acharya", "Venerable",
        "Rev", "Reverend", "Fr", "Father", "Pastor",
        "Maulana", "Mufti", "Hafiz", "Haji", "Hajji",
        "Pandit", "Pt", "Pujya", "Guruji", "Muni",
        "Kunwar", "Thakur", "Sir", "Dame", "Lord", "Lady",
        "Sister", "Brother",
    )
    private val PREFIX_LIST = NAME_PREFIXES
        .map { it.lowercase() }
        .sortedByDescending { it.split(" ").size }

    private val EMAIL_RE = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    fun isActive(card: ApplicantCard): Boolean = card.status.normalize() in ACTIVE

    fun evaluate(card: ApplicantCard, courseMates: List<ApplicantCard> = emptyList()): List<AuditFlag> {
        val out = mutableListOf<AuditFlag>()
        statusUnknown(card)?.let(out::add)
        if (!isActive(card)) return out
        val mates = courseMates.filter { it.id != card.id && isActive(it) }
        // Hard
        missingFields(card)?.let(out::add)
        phoneShort(card)?.let(out::add)
        phonePrefix(card)?.let(out::add)
        email(card)?.let(out::add)
        idMissing(card)?.let(out::add)
        ageDob(card)?.let(out::add)
        ageRange(card)?.let(out::add)
        confGender(card)?.let(out::add)
        confType(card)?.let(out::add)
        confNoDuplicate(card, mates)?.let(out::add)
        withinFileDuplicate(card, mates)?.let(out::add)
        nameTitle(card)?.let(out::add)
        // Safety
        emergencyEqSelf(card)?.let(out::add)
        emergencyPartial(card)?.let(out::add)
        // Soft
        sharedMobile(card, mates)?.let(out::add)
        sharedEmailUnrelated(card, mates)?.let(out::add)
        return out
    }

    fun merge(client: List<AuditFlag>, server: List<AuditFlag>): List<AuditFlag> {
        val byId = linkedMapOf<String, AuditFlag>()
        (server + client).forEach { byId.putIfAbsent(it.ruleId, it) }
        return byId.values.toList()
    }

    /* ── Hard ─────────────────────────────────────────────────────────── */

    /**
     * audit.js CRIT fields the card actually carries. Address is in the
     * dataset but not on the card, so it is not checked here. The emergency
     * pair is checked through the parse-time presence booleans. One flag per
     * card (merge() keys on ruleId), listing every blank field.
     */
    fun missingFields(card: ApplicantCard): AuditFlag? {
        val missing = mutableListOf<String>()
        if (card.displayName.isBlank()) missing += "Name"
        if (card.age == null) missing += "Age"
        if (card.mobile.isNullOrBlank()) missing += "PhoneMobile"
        if (card.city.isNullOrBlank()) missing += "City"
        if (card.state.isNullOrBlank()) missing += "State"
        if (card.confNo?.value?.isBlank() != false) missing += "Conf No"
        if (card.emergencyNamePresent == false) missing += "Emergency Name"
        if (card.emergencyPresent == false) missing += "Emergency Contact No"
        if (card.dob.isNullOrBlank()) missing += "DOB"
        if (missing.isEmpty()) return null
        return flag(
            AuditSeverity.HARD,
            "A required field is blank",
            "missing_field · ${missing.joinToString(", ") { "'$it'" }}",
            "missing_field",
        )
    }

    fun phoneShort(card: ApplicantCard): AuditFlag? {
        val raw = card.mobile?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val digits = raw.filter { it.isDigit() }
        if (digits.length >= 10) return null
        return flag(
            AuditSeverity.HARD,
            "Mobile number has fewer than 10 digits",
            "phone_short · $raw (${digits.length} digits)",
            "phone_short",
        )
    }

    /**
     * audit.js semantics: only Indian numbers (Country blank or India) are
     * held to the 6–9 leading-digit rule; short numbers are phone_short's job.
     */
    fun phonePrefix(card: ApplicantCard): AuditFlag? {
        val raw = card.mobile?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val digits = raw.filter { it.isDigit() }
        if (digits.length < 10) return null
        val country = card.country?.trim()?.lowercase().orEmpty()
        if (country.isNotEmpty() && country != "india") return null
        if (digits.takeLast(10).first() in '6'..'9') return null
        return flag(
            AuditSeverity.HARD,
            "Mobile number cannot be an Indian number",
            "phone_prefix_invalid · $raw",
            "phone_prefix_invalid",
        )
    }

    fun email(card: ApplicantCard): AuditFlag? {
        val e = card.email?.trim().orEmpty()
        if (e.isEmpty()) {
            return flag(
                AuditSeverity.HARD,
                "Email address is blank",
                "email_missing · (blank)",
                "email_missing",
            )
        }
        if (!EMAIL_RE.matches(e)) {
            return flag(
                AuditSeverity.HARD,
                "Email address is malformed",
                "email_malformed · $e",
                "email_malformed",
            )
        }
        return null
    }

    fun idMissing(card: ApplicantCard): AuditFlag? {
        if (card.idPresent != false) return null
        return flag(
            AuditSeverity.HARD,
            "No ID document on file",
            "id_missing · no Aadhaar / PAN / passport / voter ID",
            "id_missing",
        )
    }

    fun ageDob(card: ApplicantCard): AuditFlag? {
        val age = card.age ?: return null
        val computed = dobAge(card.dob) ?: return null
        if (kotlin.math.abs(computed - age) >= 2) {
            return flag(
                AuditSeverity.HARD,
                "Listed age does not match date of birth",
                "age_dob_mismatch · listed $age, DOB gives $computed",
                "age_dob_mismatch",
            )
        }
        return null
    }

    /** audit.js age_under_min / age_over_max (defaults 18 / 95). */
    fun ageRange(card: ApplicantCard): AuditFlag? {
        val computed = dobAge(card.dob) ?: return null
        if (computed < MIN_AGE) {
            return flag(
                AuditSeverity.HARD,
                "Date of birth gives an age under $MIN_AGE",
                "age_under_min · DOB gives $computed",
                "age_under_min",
            )
        }
        if (computed > MAX_AGE) {
            return flag(
                AuditSeverity.HARD,
                "Date of birth gives an age over $MAX_AGE",
                "age_over_max · DOB gives $computed",
                "age_over_max",
            )
        }
        return null
    }

    /**
     * The conf number encodes gender ({N|O|S}{M|F} + serial) and can disagree
     * with the recorded gender field. Surface for a human; never auto-correct.
     */
    fun confGender(card: ApplicantCard): AuditFlag? {
        val conf = card.confNo?.value?.trim().orEmpty()
        if (conf.length < 2) return null
        val encoded = conf[1].uppercaseChar()
        if (encoded != 'M' && encoded != 'F') return null
        if (encoded == card.gender.name.first()) return null
        val says = if (encoded == 'M') "male" else "female"
        return flag(
            AuditSeverity.HARD,
            "Conf number disagrees with recorded gender",
            "conf_gender_mismatch · conf says $says · record says ${card.gender.name}",
            "conf_gender_mismatch",
        )
    }

    /**
     * S-prefixed conf numbers (SM/SF) mark sevaks. Not in audit.js yet
     * (README roadmap item), so the ruleId follows conf_gender_mismatch.
     */
    fun confType(card: ApplicantCard): AuditFlag? {
        val conf = card.confNo?.value?.trim().orEmpty()
        if (conf.length < 2) return null
        val first = conf[0].uppercaseChar()
        if (first != 'N' && first != 'O' && first != 'S') return null
        val confSaysSevak = first == 'S'
        val isSevak = card.type == ApplicantType.Sevak
        if (confSaysSevak == isSevak) return null
        return flag(
            AuditSeverity.HARD,
            "Conf number prefix disagrees with sevak role",
            "conf_type_mismatch · $conf · record says ${card.type.name}",
            "conf_type_mismatch",
        )
    }

    fun confNoDuplicate(card: ApplicantCard, courseMates: List<ApplicantCard>): AuditFlag? {
        val conf = card.confNo?.value?.trim().orEmpty()
        if (conf.isEmpty()) return null
        val other = courseMates.firstOrNull {
            it.id != card.id && it.confNo?.value?.trim() == conf
        } ?: return null
        return flag(
            AuditSeverity.HARD,
            "Conf number appears twice in this course",
            "conf_no_duplicate · $conf · also ${other.displayName}",
            "conf_no_duplicate",
        )
    }

    /**
     * audit.js dedups active rows by aadhar / phone / name+dob. The aadhar
     * key is off-limits (NPI), so this matches by phone and name+dob only.
     */
    fun withinFileDuplicate(card: ApplicantCard, courseMates: List<ApplicantCard>): AuditFlag? {
        val others = courseMates.filter { it.id != card.id }
        val phone = normPhone(card.mobile)
        if (phone != null) {
            val hit = others.firstOrNull { normPhone(it.mobile) == phone }
            if (hit != null) {
                return flag(
                    AuditSeverity.HARD,
                    "Same person may be entered twice",
                    "within_file_duplicate · phone · also ${hit.displayName}",
                    "within_file_duplicate",
                )
            }
        }
        val name = normName(card.displayName)
        val dob = card.dob?.trim().orEmpty()
        if (name != null && dob.isNotEmpty()) {
            val hit = others.firstOrNull {
                normName(it.displayName) == name && it.dob?.trim() == dob
            }
            if (hit != null) {
                return flag(
                    AuditSeverity.HARD,
                    "Same person may be entered twice",
                    "within_file_duplicate · name+DOB · also ${hit.displayName}",
                    "within_file_duplicate",
                )
            }
        }
        return null
    }

    /** Runs on every row, active or not (audit.js does the same). */
    fun statusUnknown(card: ApplicantCard): AuditFlag? {
        val s = card.status.normalize()
        if (s.isEmpty() || s in KNOWN_STATUSES) return null
        return flag(
            AuditSeverity.HARD,
            "Status value is not one the desk knows",
            "status_unknown · ${card.status.value}",
            "status_unknown",
        )
    }

    /**
     * audit.js namePrefix: normalizes "Mr.Ramesh" → "Mr. Ramesh", matches
     * longest title first, and only fires when a name follows the title —
     * a standalone given name like "Baby" or "Kumari" never trips it.
     */
    fun nameTitle(card: ApplicantCard): AuditFlag? {
        val norm = card.displayName
            .replace(Regex("""\(sevak\)""", RegexOption.IGNORE_CASE), "")
            .replace(".", ". ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (norm.isEmpty()) return null
        val tokens = norm.split(" ")
        val bare = tokens.map { it.trimEnd('.').lowercase() }
        for (p in PREFIX_LIST) {
            val words = p.split(" ")
            if (tokens.size <= words.size) continue
            if (bare.take(words.size).joinToString(" ") == p) {
                val shown = tokens.take(words.size).joinToString(" ")
                return flag(
                    AuditSeverity.HARD,
                    "Honorific left in the name field",
                    "name_title_prefix · '$shown'",
                    "name_title_prefix",
                )
            }
        }
        return null
    }

    /* ── Safety ───────────────────────────────────────────────────────── */

    fun emergencyEqSelf(card: ApplicantCard): AuditFlag? {
        if (card.emergencyEqSelf != true) return null
        return flag(
            AuditSeverity.SAFETY,
            "Emergency contact is their own mobile",
            "emergency_eq_self · same number as own mobile",
            "emergency_eq_self",
        )
    }

    fun emergencyPartial(card: ApplicantCard): AuditFlag? {
        val hasName = card.emergencyNamePresent ?: return null
        val hasPhone = card.emergencyPresent ?: return null
        if (hasName == hasPhone) return null
        val which = if (hasName) "name without a number" else "number without a name"
        return flag(
            AuditSeverity.SAFETY,
            "Emergency contact is half-filled",
            "emergency_partial · $which",
            "emergency_partial",
        )
    }

    /* ── Soft ─────────────────────────────────────────────────────────── */

    fun sharedMobile(card: ApplicantCard, courseMates: List<ApplicantCard>): AuditFlag? {
        val mine = normPhone(card.mobile) ?: return null
        val other = courseMates.firstOrNull {
            it.id != card.id && normPhone(it.mobile) == mine
        } ?: return null
        return flag(
            AuditSeverity.SOFT,
            "Mobile shared with another applicant",
            "shared_mobile · $mine · also ${other.displayName}",
            "shared_mobile",
        )
    }

    fun sharedEmailUnrelated(card: ApplicantCard, courseMates: List<ApplicantCard>): AuditFlag? {
        val mine = normEmail(card.email) ?: return null
        val group = courseMates.filter { it.id != card.id && normEmail(it.email) == mine }
        if (group.isEmpty()) return null
        val surnames = (group + card).map { surname(it.displayName) }.toSet()
        if (surnames.size <= 1) return null
        return flag(
            AuditSeverity.SOFT,
            "Email shared across unrelated surnames",
            "shared_email_unrelated · $mine · also ${group.first().displayName}",
            "shared_email_unrelated",
        )
    }

    /* ── Normalizers (audit.js _internal) ─────────────────────────────── */

    private fun normPhone(raw: String?): String? {
        val d = raw?.filter { it.isDigit() } ?: return null
        if (d.isEmpty()) return null
        return if (d.length >= 10) d.takeLast(10) else d
    }

    private fun normName(raw: String?): String? = raw
        ?.lowercase()
        ?.replace("(sevak)", "")
        ?.replace(Regex("""\s+"""), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    private fun normEmail(raw: String?): String? {
        val s = raw?.trim()?.lowercase() ?: return null
        return s.takeIf { EMAIL_RE.matches(it) }
    }

    private fun surname(name: String): String =
        name.trim().split(Regex("""\s+""")).last().lowercase()

    /** Year-only age from the free-text DOB string — the card carries no parsed date. */
    private fun dobAge(dob: String?): Int? {
        val raw = dob ?: return null
        val year = Regex("""(\d{4})""").findAll(raw).lastOrNull()?.value?.toIntOrNull() ?: return null
        return NOW_YEAR - year
    }

    private fun flag(sev: AuditSeverity, label: String, detail: String, id: String) =
        AuditFlag(sev, label, detail, id)
}
