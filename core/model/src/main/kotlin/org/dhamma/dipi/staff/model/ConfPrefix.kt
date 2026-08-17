package org.dhamma.dipi.staff.model

/**
 * Display metadata parsed from a confirmation / roll number.
 *
 * Desk + PHP encode the first two letters as `{n|o}{m|f}` plus a serial:
 * `nf1`, `of12`, `nm1`, `om12`. Sevak prefixes (`sm`/`sf`) yield a known
 * gender and unknown seniority. Anything else is unknown on that axis —
 * still visible when the axis is "all", hidden when a specific value is on.
 */
enum class ConfSeniority { NEW, OLD, UNKNOWN }

data class ConfPrefix(
    val seniority: ConfSeniority = ConfSeniority.UNKNOWN,
    val gender: Gender? = null,
) {
    fun matches(gender: Gender?, seniority: ConfSeniority?): Boolean =
        (gender == null || this.gender == gender) &&
            (seniority == null || this.seniority == seniority)

    companion object {
        val UNKNOWN = ConfPrefix()

        fun parse(raw: String?): ConfPrefix {
            val t = raw?.trim().orEmpty()
            if (t.length < 2) return UNKNOWN
            val seniority = when (t[0].lowercaseChar()) {
                'n' -> ConfSeniority.NEW
                'o' -> ConfSeniority.OLD
                else -> ConfSeniority.UNKNOWN
            }
            val gender = when (t[1].lowercaseChar()) {
                'm' -> Gender.M
                'f' -> Gender.F
                else -> null
            }
            return ConfPrefix(seniority, gender)
        }
    }
}
