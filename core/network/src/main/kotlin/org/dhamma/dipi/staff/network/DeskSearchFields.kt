package org.dhamma.dipi.staff.network

/**
 * POST body for `dh_manageapp_search_form` (`search.inc:1396`).
 * Only name / conf / optional status — never bulk-mail schedule fields.
 */
object DeskSearchFields {
    const val SUBMIT = "Search"
    private val CONF = Regex("""^[A-Za-z]{1,4}\d+[A-Za-z0-9]*$""")

    fun of(html: String, q: String, status: String? = null): Map<String, String>? {
        val tokens = SearchPageParser.tokens(html) ?: return null
        val trimmed = q.trim()
        if (trimmed.isEmpty()) return null
        return buildMap {
            if (CONF.matches(trimmed)) {
                put("conf_no", trimmed)
            } else {
                val parts = trimmed.split(Regex("\\s+"), limit = 2)
                put("f_name", parts[0])
                if (parts.size > 1) put("l_name", parts[1])
            }
            put("type", "Both")
            if (!status.isNullOrBlank()) put("status[]", status)
            put("form_build_id", tokens.formBuildId)
            put("form_token", tokens.formToken)
            put("form_id", tokens.formId)
            put("op", SUBMIT)
        }
    }
}
