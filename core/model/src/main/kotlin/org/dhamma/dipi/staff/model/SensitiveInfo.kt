package org.dhamma.dipi.staff.model

/**
 * Display-only ID and health disclosures for one applicant, parsed from the
 * live worklist so the desk admin can visually verify the physical document
 * and review disclosures with the applicant (owner-approved amendment,
 * 2026-08-16).
 *
 * NEVER persisted or logged: not @Serializable, no Room column, no DataStore
 * key, no ApplicantDto field. Held only in a session-scoped in-memory map
 * that is cleared when a new course worklist replaces the rows and on
 * logout / erase-all-local-data.
 *
 * [health] holds only the fields that survive the course-audit noise filter
 * (field label → disclosed text).
 */
data class SensitiveInfo(
    val idLabel: String? = null,
    val idNumber: String? = null,
    val health: Map<String, String> = emptyMap(),
) {
    /** Redacted on purpose — an accidental log line must never carry the values. */
    override fun toString(): String =
        "SensitiveInfo(idLabel=$idLabel, idNumber=██, health.keys=${health.keys})"
}
