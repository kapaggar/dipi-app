package org.dhamma.dipi.staff.audit

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity

object ClientAudit {
    private val titles = listOf("sister", "brother", "mr", "mrs", "ms", "smt", "shri")

    fun evaluate(card: ApplicantCard, courseMates: List<ApplicantCard> = emptyList()): List<AuditFlag> {
        val out = mutableListOf<AuditFlag>()
        phonePrefix(card)?.let(out::add)
        missingEmergency(card)?.let(out::add)
        ageDob(card)?.let(out::add)
        nameTitle(card)?.let(out::add)
        sharedMobile(card, courseMates)?.let(out::add)
        return out
    }

    fun merge(client: List<AuditFlag>, server: List<AuditFlag>): List<AuditFlag> {
        val byId = linkedMapOf<String, AuditFlag>()
        (server + client).forEach { byId.putIfAbsent(it.ruleId, it) }
        return byId.values.toList()
    }

    fun phonePrefix(card: ApplicantCard): AuditFlag? {
        val raw = card.mobile ?: return null
        val digits = raw.filter { it.isDigit() }
        if (!raw.contains("+91") && !digits.startsWith("91")) return null
        val national = when {
            digits.startsWith("91") && digits.length > 10 -> digits.drop(2)
            else -> digits.takeLast(10)
        }
        if (national.length != 10) {
            return flag(
                AuditSeverity.HARD,
                "Mobile number cannot be an Indian number",
                "phone_prefix_invalid · $raw",
                "phone_prefix_invalid",
            )
        }
        val first = national.first()
        if (first !in listOf('6', '7', '8', '9')) {
            return flag(
                AuditSeverity.HARD,
                "Mobile number cannot be an Indian number",
                "phone_prefix_invalid · $raw",
                "phone_prefix_invalid",
            )
        }
        return null
    }

    fun missingEmergency(card: ApplicantCard): AuditFlag? {
        if (card.emergencyPresent == false) {
            return flag(
                AuditSeverity.HARD,
                "Emergency contact number is blank",
                "missing_field · 'Emergency Contact No'",
                "missing_field",
            )
        }
        return null
    }

    fun ageDob(card: ApplicantCard): AuditFlag? {
        val age = card.age ?: return null
        val dob = card.dob ?: return null
        val year = Regex("""(\d{4})""").findAll(dob).lastOrNull()?.value?.toIntOrNull()
            ?: return null
        val nowYear = 2026
        val computed = nowYear - year
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

    fun nameTitle(card: ApplicantCard): AuditFlag? {
        val first = card.givenName.trim().lowercase()
        val hit = titles.firstOrNull { first == it || first.startsWith("$it ") }
            ?: return null
        val shown = card.givenName.trim().substringBefore(" ")
        return flag(
            AuditSeverity.HARD,
            "Honorific left in the name field",
            "name_title_prefix · '$shown'",
            "name_title_prefix",
        )
    }

    fun sharedMobile(card: ApplicantCard, courseMates: List<ApplicantCard>): AuditFlag? {
        val mine = national(card.mobile) ?: return null
        if (mine.length < 10) return null
        val other = courseMates.firstOrNull {
            it.id != card.id && national(it.mobile) == mine
        } ?: return null
        return flag(
            AuditSeverity.SOFT,
            "Mobile shared with another applicant",
            "shared_mobile · $mine · also ${other.displayName}",
            "shared_mobile",
        )
    }

    private fun digits(raw: String?): String? =
        raw?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }

    private fun national(raw: String?): String? {
        val d = digits(raw) ?: return null
        return if (d.length >= 10) d.takeLast(10) else d
    }

    private fun flag(sev: AuditSeverity, label: String, detail: String, id: String) =
        AuditFlag(sev, label, detail, id)
}
