package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.HealthRow

/**
 * `GET /application-view/{id}` (`dh_manageapp/inc/search.inc:1963-2198`) —
 * a full themed Drupal page of `av-sec` sections (`<h3>` titles) holding
 * `av-row` rows (`av-label`/`av-val` spans).
 *
 * NPI discipline (spec 2d S1): the parser extracts ONLY the header, the
 * `Personal`, `Course History` and `Health` sections — by a structural
 * section-title ALLOWLIST. `Contact`, `Identification`, `Background`,
 * `Emergency Contact`, `Languages`, `Other`, `Children/Teen`, `Long Course
 * Details` and the lazy `Loading...` sections are never scanned: rows are
 * only read inside an allowlisted section's own slice of the page, so an
 * Aadhaar/PAN/passport/emergency/mobile/address string can never enter the
 * model. `ApplicationViewParserTest` plants realistic decoys and asserts
 * exactly that.
 */
object ApplicationViewParser {

    /** The ONLY section titles whose rows are ever read. */
    private val SECTION_ALLOWLIST = setOf("personal", "course history", "health")

    private val sectionStartRe = Regex(
        """<div[^>]*\bclass\s*=\s*["'][^"']*av-sec[^"']*["'][^>]*>""",
        RegexOption.IGNORE_CASE,
    )
    private val h3Re = Regex(
        """<h3[^>]*>(.*?)</h3>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val h2Re = Regex(
        """<h2[^>]*>(.*?)</h2>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val statusRe = Regex(
        """<div[^>]*\bclass\s*=\s*["'][^"']*av-status[^"']*["'][^>]*>(.*?)</div>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val photoRe = Regex(
        """<img[^>]*\bsrc\s*=\s*["'][^"']*show-photo/""",
        RegexOption.IGNORE_CASE,
    )
    private val rowRe = Regex(
        """<span[^>]*\bclass\s*=\s*["'][^"']*av-label[^"']*["'][^>]*>(.*?)</span>\s*""" +
            """<span[^>]*\bclass\s*=\s*["'][^"']*av-val[^"']*["'][^>]*>(.*?)</span>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** `{name} ({conf})` — conf optional. */
    private val nameConfRe = Regex("""^(.*?)\s*\(([^()]+)\)\s*$""")

    fun parse(html: String): ApplicationCard {
        val header = h2Re.find(html)?.groupValues?.get(1)?.let { SearchPageParser.stripTags(it) }.orEmpty()
        val nameConf = nameConfRe.find(header)
        val name = nameConf?.groupValues?.get(1)?.trim() ?: header
        val conf = nameConf?.groupValues?.get(2)?.trim()

        val sections = splitSections(html)
        val personal = (sections["personal"]?.let { rows(it) } ?: emptyList())
            // Label allowlist: if a following section's wrapper ever loses its
            // av-sec class, its rows would merge into this slice — Contact is
            // the live page's next section, so unfiltered rows could carry
            // Mobile/Address. Drop anything not one of the eight (2d F1).
            .filter { (label, _) ->
                ApplicationCard.PERSONAL_ALLOWLIST.any { it.equals(label, ignoreCase = true) }
            }
        val historyRows = sections["course history"]?.let { rows(it) } ?: emptyList()
        val healthRows = sections["health"]?.let { rows(it) } ?: emptyList()

        fun historyValue(key: String): String =
            historyRows.firstOrNull { it.first.equals(key, ignoreCase = true) }?.second.orEmpty()

        return ApplicationCard(
            name = name,
            conf = conf,
            statusLine = statusRe.find(html)?.groupValues?.get(1)
                ?.let { SearchPageParser.stripTags(it) }.orEmpty(),
            hasPhoto = photoRe.containsMatchIn(html),
            personal = personal,
            historyCounts = ApplicationCard.HISTORY_ORDER.map { key ->
                key to (historyValue(key).toIntOrNull() ?: 0)
            },
            firstCourse = historyValue("First Course"),
            lastCourse = historyValue("Last Course"),
            practiceDetails = historyValue("Practice Details"),
            // All six labels, verbatim, in server order — a missing row keeps
            // its label with an empty answer (absence is information).
            health = ApplicationCard.HEALTH_ORDER.map { label ->
                HealthRow(
                    label = label,
                    answer = healthRows.firstOrNull { it.first.equals(label, ignoreCase = true) }
                        ?.second.orEmpty(),
                )
            },
        )
    }

    /**
     * Slices the page into `av-sec` blocks and keeps ONLY the allowlisted
     * ones, keyed by lowercase title. Every other section — the NPI ones and
     * the four lazy `Loading...` ones — is dropped here, before any row
     * regex ever sees its markup.
     */
    private fun splitSections(html: String): Map<String, String> {
        val starts = sectionStartRe.findAll(html).map { it.range.first }.toList()
        val out = mutableMapOf<String, String>()
        starts.forEachIndexed { i, start ->
            val end = starts.getOrElse(i + 1) { html.length }
            val block = html.substring(start, end)
            val title = h3Re.find(block)?.groupValues?.get(1)
                ?.let { SearchPageParser.stripTags(it) }?.lowercase() ?: return@forEachIndexed
            if (title in SECTION_ALLOWLIST) out[title] = block
        }
        return out
    }

    /** `av-label`/`av-val` pairs, tag-stripped, verbatim (`-` stays `-`). */
    private fun rows(section: String): List<Pair<String, String>> =
        rowRe.findAll(section).map { m ->
            SearchPageParser.stripTags(m.groupValues[1]) to SearchPageParser.stripTags(m.groupValues[2])
        }.toList()
}
