package org.dhamma.dipi.staff.network

/**
 * Named + numeric/hex HTML entities for Kotlin-parsed desk HTML.
 * WebView sheets decode natively — keep this off that path.
 *
 * Covers the four named entities [SearchPageParser.stripTags] already
 * handled, plus `&apos;`, `&quot;`, and `&#…;` / `&#x…;` (Drupal emits
 * `&#039;` for an apostrophe). Unknown named entities stay verbatim.
 */
internal object HtmlEntities {
    private val NAMED = mapOf(
        "nbsp" to " ",
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
    )
    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#\d+|[a-zA-Z]+);""")

    fun unescape(raw: String): String = ENTITY.replace(raw) { m ->
        val body = m.groupValues[1]
        when {
            body.startsWith("#x", ignoreCase = true) ->
                body.drop(2).toIntOrNull(16)?.takeIf { it in 0..0xFFFF }
                    ?.toChar()?.toString() ?: m.value
            body.startsWith("#") ->
                body.drop(1).toIntOrNull()?.takeIf { it in 0..0xFFFF }
                    ?.toChar()?.toString() ?: m.value
            else -> NAMED[body.lowercase()] ?: m.value
        }
    }
}

/**
 * Standalone em-dash / en-dash / hyphen tokens are empty-surname
 * placeholders, not a family name. Drop them so "Savita —" reads "Savita".
 */
internal val DASH_ONLY_TOKEN = Regex("""^[—–−-]+$""")

internal fun cleanPersonName(raw: String): String =
    raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() && !DASH_ONLY_TOKEN.matches(it) }
        .joinToString(" ")
