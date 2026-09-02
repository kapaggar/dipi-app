package org.dhamma.dipi.staff.model

/**
 * One `GET /application-view/{id}` parsed for the course-ops student card
 * (spec 2d S1, `dh_manageapp/inc/search.inc:1963-2198`). Deliberately NOT
 * `@Serializable`: persistence is a separate snapshot DTO owned by the
 * course-ops store (owner amendment 2026-09-02 — encrypted at rest, wiped on
 * course change / Erase-all / logout).
 *
 * NPI discipline: by construction there is NO field for Contact,
 * Identification, Background, Emergency Contact, Languages, Other,
 * Children/Teen or Long Course Details — the parser never reads those
 * sections. Health answers live here for on-screen display only; every
 * `toString()` on this type redacts them so an accidental log line never
 * carries a disclosure.
 */
data class ApplicationCard(
    /** Header `<h2>{name} ({conf})</h2>` — name part, verbatim. */
    val name: String,
    /** Header conf number; null when the page prints none. */
    val conf: String? = null,
    /** The `av-status` line, verbatim: `{status} · {course}`. */
    val statusLine: String = "",
    /** A `show-photo/{id}` img is on the page — reuse PhotoLoader when true. */
    val hasPhoto: Boolean = false,
    /**
     * `Personal` section rows verbatim, in page order (Gender, Date of Birth,
     * Age, Nationality, Old / New, Monk / Nun, A-List, Applied On). The
     * page's own `-` stays `-`.
     */
    val personal: List<Pair<String, String>> = emptyList(),
    /**
     * `Course History` counts in SERVER order — [HISTORY_ORDER]. Zeros stay:
     * the shape of the history is the information.
     */
    val historyCounts: List<Pair<String, Int>> = emptyList(),
    val firstCourse: String = "",
    val lastCourse: String = "",
    val practiceDetails: String = "",
    /** `Health` rows, labels verbatim in server order — [HEALTH_ORDER]. */
    val health: List<HealthRow> = emptyList(),
) {
    fun personalValue(key: String): String =
        personal.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second ?: "-"

    fun healthRow(label: String): HealthRow? =
        health.firstOrNull { it.label.equals(label, ignoreCase = true) }

    /** Redacted on purpose — answers must never reach a log line. */
    override fun toString(): String =
        "ApplicationCard(name=$name, conf=$conf, hasPhoto=$hasPhoto, " +
            "personal.keys=${personal.map { it.first }}, health.labels=${health.map { it.label }})"

    companion object {
        /** The ten course-history keys in the page's own order (search.inc). */
        val HISTORY_ORDER = listOf(
            "10-Day", "Teen", "STP", "Special", "TSC",
            "20-Day", "30-Day", "45-Day", "60-Day", "Service",
        )

        /** The six health labels, verbatim, in the page's own order. */
        val HEALTH_ORDER = listOf(
            "Physical", "Mental", "Medication", "Intoxicants",
            "Other Techniques", "Pregnancy",
        )
    }
}

/**
 * One Health question: label verbatim, answer verbatim (`Yes`, `No`,
 * `Yes - 4 (months)`, free text, or the page's `-`).
 */
data class HealthRow(val label: String, val answer: String) {
    /** Something is written here: non-blank and not the page's own `-`. */
    val answered: Boolean get() = answer.isNotBlank() && answer.trim() != "-"

    /** Redacted on purpose — the answer text must never reach a log line. */
    override fun toString(): String = "HealthRow(label=$label, answer=██)"
}

/** The fixed flag order (spec 2d S3). */
val FLAG_ORDER = listOf("HLTH", "MED", "INTOX", "TECH", "PREG", "MONK")

/**
 * Derived FLAGS for the teacher list (spec 2d S3). A flag says only "there
 * is something written here" — never a severity, never a summary, never a
 * colour code. Order is fixed: HLTH MED INTOX TECH PREG MONK.
 *
 * - `HLTH`: Health · Physical or Mental answered (non-empty and != `-`).
 * - `MED`: Medication answered. `INTOX`: Intoxicants. `TECH`: Other Techniques.
 * - `PREG`: Pregnancy answer starts `Yes` — for gender F only; the question
 *   does not apply to male applicants (the card renders an `N/A` tag).
 * - `MONK`: Personal · Monk / Nun == `Yes`.
 */
fun flagsFor(card: ApplicationCard, gender: Gender): List<String> = buildList {
    fun answered(label: String) = card.healthRow(label)?.answered == true
    if (answered("Physical") || answered("Mental")) add("HLTH")
    if (answered("Medication")) add("MED")
    if (answered("Intoxicants")) add("INTOX")
    if (answered("Other Techniques")) add("TECH")
    val pregnancy = card.healthRow("Pregnancy")?.answer?.trim().orEmpty()
    if (gender == Gender.F && pregnancy.startsWith("Yes", ignoreCase = true)) add("PREG")
    if (card.personalValue("Monk / Nun").trim().equals("Yes", ignoreCase = true)) add("MONK")
}
