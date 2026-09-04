package org.dhamma.dipi.staff.network

/**
 * Shared Drupal-form scrapers. Attribute order varies (`name` before or
 * after `value`); both shapes are accepted, matching [CourseReportFormParser].
 */
internal object HtmlForms {
    fun inputValue(html: String, name: String): String? {
        val n = Regex.escape(name)
        return Regex("""<input\b[^>]*\bname\s*=\s*["']$n["'][^>]*\bvalue\s*=\s*["']([^"']*)["']""")
            .find(html)?.groupValues?.get(1)
            ?: Regex("""<input\b[^>]*\bvalue\s*=\s*["']([^"']*)["'][^>]*\bname\s*=\s*["']$n["']""")
                .find(html)?.groupValues?.get(1)
    }

    fun textarea(html: String, name: String): String? {
        val n = Regex.escape(name)
        val m = Regex(
            """<textarea\b[^>]*\bname\s*=\s*["']$n["'][^>]*>(.*?)</textarea>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html) ?: return null
        return unescape(m.groupValues[1]).trim()
    }

    /** The checked radio for [name], or null when none is selected. */
    fun radioValue(html: String, name: String): String? {
        val n = Regex.escape(name)
        val tags = Regex("""<input\b[^>]*\btype\s*=\s*["']radio["'][^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(html)
        for (tag in tags) {
            val open = tag.value
            if (!open.contains(Regex("""name\s*=\s*["']$n["']""", RegexOption.IGNORE_CASE))) continue
            if (!open.contains("checked", ignoreCase = true)) continue
            return Regex("""value\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                .find(open)?.groupValues?.get(1)
        }
        return null
    }

    fun yesNo(html: String, name: String): Boolean? = when (radioValue(html, name)) {
        "1" -> true
        "0" -> false
        else -> null
    }

    fun formAction(html: String): String? =
        Regex("""<form\b[^>]*\baction\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.replace("&amp;", "&")

    private fun unescape(raw: String): String = HtmlEntities.unescape(raw)
}

/**
 * Drupal `theme('table')` rows. Prefers `<tbody>`; otherwise walks every
 * `<tr>` that has `<td>` cells so a header-only table yields nothing.
 */
internal object HtmlTables {
    fun rowsById(html: String, id: String): List<List<String>> {
        val block = extractElementById(html, id) ?: return emptyList()
        return rows(block)
    }

    fun firstTableRows(html: String): List<List<String>> {
        val open = Regex("""<table\b[^>]*>""", RegexOption.IGNORE_CASE).find(html) ?: return emptyList()
        val start = open.range.first
        val end = html.indexOf("</table>", start, ignoreCase = true)
        if (end < 0) return emptyList()
        return rows(html.substring(start, end + "</table>".length))
    }

    fun rows(tableHtml: String): List<List<String>> {
        val body = Regex(
            """<tbody\b[^>]*>(.*?)</tbody>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(tableHtml)?.groupValues?.get(1) ?: tableHtml
        return Regex(
            """<tr\b[^>]*>(.*?)</tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).findAll(body).mapNotNull { tr ->
            val cells = Regex(
                """<td\b[^>]*>(.*?)</td>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ).findAll(tr.groupValues[1]).map {
                SearchPageParser.stripTags(it.groupValues[1])
            }.toList()
            cells.takeIf { it.isNotEmpty() }
        }.toList()
    }
}
