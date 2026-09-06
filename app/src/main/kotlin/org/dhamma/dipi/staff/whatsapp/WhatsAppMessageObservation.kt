package org.dhamma.dipi.staff.whatsapp

/** WhatsApp exposes bold text without its paired delimiters. URLs stay byte-exact. */
private fun displayedLetter(prepared: String): String {
    val url = Regex("https?://\\S+")
    val bold = Regex("(?<![\\p{L}\\p{N}])\\*(?=\\S)([^*]+?)(?<=\\S)\\*(?![\\p{L}\\p{N}])")
    val result = StringBuilder()
    var start = 0
    for (link in url.findAll(prepared)) {
        result.append(bold.replace(prepared.substring(start, link.range.first)) { it.groupValues[1] })
        result.append(link.value)
        start = link.range.last + 1
    }
    return result.append(bold.replace(prepared.substring(start)) { it.groupValues[1] }).toString()
}

internal fun outgoingTextMatches(visible: String, prepared: String): Boolean =
    visible == prepared || visible == displayedLetter(prepared)

/** A prefix only identifies a row to expand. It must NEVER prove submission. */
internal fun collapsedLetterCandidate(visible: String, prepared: String, hasReadMore: Boolean): Boolean {
    if (!hasReadMore) return false
    val prefix = visible.trimEnd().removeSuffix("Read more").trimEnd()
        .removeSuffix("…").removeSuffix("...").trimEnd()
    return prefix.length >= 100 && (prepared.startsWith(prefix) || displayedLetter(prepared).startsWith(prefix))
}

/** Existing similar collapsed rows make the new row ambiguous; leave it unknown. */
internal fun canExpandNewCandidate(composer: String, candidates: Int, baseline: Int): Boolean =
    composer in setOf("", "Message") && candidates == 1 && baseline == 0
