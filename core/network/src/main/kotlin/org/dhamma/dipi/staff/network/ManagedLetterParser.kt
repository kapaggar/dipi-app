package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.ManagedLetter
import org.dhamma.dipi.staff.model.RenderedLetter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.net.URI

/** Only the existing active-letter table and applicant portal's letter container. */
object ManagedLetterParser {
    private const val MAX_HTML_LENGTH = 512_000
    private const val MAX_MESSAGE_LENGTH = 16_000
    private val unresolved = Regex("\\[[\\p{L}_][\\p{L}\\p{N}_ -]*\\]|\\{[\\p{L}_][\\p{L}\\p{N}_]*\\}")

    fun activeLetters(html: String, serverOrigin: String, centreId: Int): List<ManagedLetter> {
        require(centreId > 0) { "Invalid centre" }
        val origin = httpsOrigin(serverOrigin)
        val document = parse(html)
        val tables = document.select("table#table-letters")
        require(tables.size == 1) { "Active letter listing is unavailable" }
        val route = Regex("/letters/$centreId/(?:view|edit)/(\\d+)")
        val letters = tables.single().select("tr").mapNotNull { row ->
            val cells = row.children().filter { it.tagName() == "td" }
            if (cells.isEmpty()) return@mapNotNull null
            require(cells.size >= 5) { "Unsupported letter listing" }
            val ids = cells.drop(4).flatMap { it.select("a[href]") }.mapNotNull { link ->
                val url = runCatching { origin.resolve(link.attr("href")) }.getOrNull()
                    ?: return@mapNotNull null
                if (url.scheme != origin.scheme || url.rawAuthority != origin.rawAuthority ||
                    url.rawQuery != null || url.rawFragment != null) return@mapNotNull null
                route.matchEntire(url.path)?.groupValues?.get(1)?.toIntOrNull()
            }.distinct()
            require(ids.size == 1 && ids.single() > 0) { "Letter identity is unavailable" }
            require(cells[0].text().isNotBlank()) { "Letter name is unavailable" }
            ManagedLetter(ids.single(), centreId, cells[0].text(), cells[1].text(), cells[2].text(), cells[3].text())
        }
        require(letters.map { it.id }.distinct().size == letters.size) { "Duplicate letter identities" }
        return letters
    }

    /** No network, WebView, scripts, images, attachment downloads or link visits. */
    fun renderedLetter(html: String, applicantId: Int, letterId: Int): RenderedLetter {
        require(applicantId > 0 && letterId > 0) { "Invalid letter identifiers" }
        val document = parse(html)
        val containers = document.select("div.container > div.main > pre")
        require(containers.size == 1) { "Personalised letter is unavailable" }
        val container = containers.single()
        require(container.select("script, iframe, object, embed, form, input, img, audio, video").isEmpty()) {
            "Letter contains unsupported content"
        }
        val text = render(container).replace("\r\n", "\n").replace('\r', '\n')
            .replace('\u00a0', ' ').trim()
        require(text.isNotBlank()) { "Personalised letter is empty" }
        require(text.length <= MAX_MESSAGE_LENGTH) { "Letter is too long for automation" }
        require(!unresolved.containsMatchIn(text)) { "Letter contains unresolved fields" }
        require(text.none { it.code < 32 && it != '\n' && it != '\t' }) { "Letter contains unsupported characters" }
        return RenderedLetter(applicantId, letterId, text)
    }

    private fun render(node: Node): String = when (node) {
        is TextNode -> node.wholeText
        is Element -> {
            val inner = node.childNodes().joinToString("") { render(it) }
            when (node.tagName()) {
                "style", "noscript" -> ""
                "br" -> "\n"
                "a" -> {
                    val href = node.attr("href")
                    val uri = runCatching { URI(href) }.getOrNull()
                    require(uri != null && uri.scheme in setOf("https", "http") &&
                        !uri.host.isNullOrBlank() && uri.rawUserInfo == null) { "Letter contains an unsupported link" }
                    if (inner.trim() == href) href else "$inner ($href)"
                }
                "p", "div", "h1", "h2", "h3", "blockquote" -> "\n\n$inner\n\n"
                "li" -> "\n• $inner"
                "tr" -> "$inner\n"
                "td", "th" -> "$inner\t"
                else -> inner
            }
        }
        else -> ""
    }

    private fun parse(html: String): org.jsoup.nodes.Document {
        require(html.length <= MAX_HTML_LENGTH) { "Letter response is too large" }
        return Jsoup.parse(html)
    }

    private fun httpsOrigin(value: String): URI {
        val uri = runCatching { URI(value) }.getOrNull()
        require(uri != null && uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.path in setOf("", "/")) { "Invalid desk origin" }
        return URI("${uri.scheme}://${uri.rawAuthority}/")
    }
}
