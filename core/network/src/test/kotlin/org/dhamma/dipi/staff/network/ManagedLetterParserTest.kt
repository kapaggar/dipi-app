package org.dhamma.dipi.staff.network

import org.junit.Assert.*
import org.junit.Test

class ManagedLetterParserTest {
    private val origin = "https://desk.example.test"
    private fun row(id: Int, centre: Int = 91, route: String = "view") =
        """<tr><td>Arrival &amp; instructions</td><td>Reconfirmation</td><td>All</td><td>नमस्ते</td>
            <td><a href="/letters/$centre/$route/$id">View</a></td></tr>"""

    private fun listing(rows: String) = "<table id='table-letters'><tbody>$rows</tbody></table>"
    private fun letter(body: String) = "<html><body><div class='container'><div class='main'><pre>$body</pre></div></div></body></html>"

    @Test fun `reads active letters only and preserves centre metadata`() {
        val parsed = ManagedLetterParser.activeLetters(
            listing(row(44)) + "<table id='table-letters-d'>${row(55)}</table>", origin, 91,
        )
        assertEquals(listOf(44), parsed.map { it.id })
        assertEquals(91, parsed.single().centreId)
        assertEquals("Arrival & instructions", parsed.single().name)
        assertEquals("नमस्ते", parsed.single().subject)
    }

    @Test fun `edit link can identify a letter without visiting edit`() {
        assertEquals(44, ManagedLetterParser.activeLetters(listing(row(44, route = "edit")), origin, 91).single().id)
    }

    @Test fun `empty active table is valid but login and wrong markup are not`() {
        assertTrue(ManagedLetterParser.activeLetters(listing(""), origin, 91).isEmpty())
        rejects { ManagedLetterParser.activeLetters("<form id='user-login'>Sign in</form>", origin, 91) }
        rejects { ManagedLetterParser.activeLetters(listing(row(44)) + listing(row(45)), origin, 91) }
    }

    @Test fun `rejects cross centre foreign origin query parameters and duplicate identities`() {
        rejects { ManagedLetterParser.activeLetters(listing(row(44, 92)), origin, 91) }
        rejects { ManagedLetterParser.activeLetters(listing(row(44).replace("/letters/", "https://other.example/letters/")), origin, 91) }
        rejects { ManagedLetterParser.activeLetters(listing(row(44).replace("view/44", "view/44?r=1")), origin, 91) }
        rejects { ManagedLetterParser.activeLetters(listing(row(44) + row(44)), origin, 91) }
        rejects { ManagedLetterParser.activeLetters(listing(row(44)), "https://user:pass@desk.example.test", 91) }
    }

    @Test fun `preserves Hindi line breaks emoji and complete decoded links`() {
        val message = ManagedLetterParser.renderedLetter(
            letter("नमस्ते मित्र\n\nअपनी पुष्टि करें। &#x1F64F;<br><a href='https://portal.example.test/?a=one&amp;b=two%2Bthree'>Confirm / cancel</a>"),
            123, 44,
        )
        assertEquals("नमस्ते मित्र\n\nअपनी पुष्टि करें। 🙏\nConfirm / cancel (https://portal.example.test/?a=one&b=two%2Bthree)", message.text)
        assertEquals(123, message.applicantId)
        assertFalse(message.toString().contains("one"))
        assertFalse(message.toString().contains("नमस्ते"))
    }

    @Test fun `keeps paragraph and list boundaries without duplicating visible URLs`() {
        val text = ManagedLetterParser.renderedLetter(letter("<p>First</p><p>Second</p><ul><li>Bring ID</li><li>Arrive on time</li></ul><a href='https://example.test'>https://example.test</a>"), 1, 2).text
        assertTrue(text.contains("First\n\n"))
        assertTrue(text.contains("\n• Bring ID\n• Arrive on time"))
        assertEquals(1, Regex("https://example.test").findAll(text).count())
    }

    @Test fun `rejects missing empty unresolved unsafe and oversized bodies`() {
        listOf("", "   ", "Hello [FirstName]", "Hello {name}",
            "<script>alert(1)</script>Hello", "<form>Confirm</form>", "<img src='https://example.test/pixel'>",
            "<a href='javascript:alert(1)'>Confirm</a>", "<a href='/confirm'>Confirm</a>", "x".repeat(16001),
        ).forEach { body -> rejects { ManagedLetterParser.renderedLetter(letter(body), 1, 2) } }
        rejects { ManagedLetterParser.renderedLetter("<pre>Not the portal</pre>", 1, 2) }
        rejects { ManagedLetterParser.renderedLetter(letter("Hello") + letter("Duplicate"), 1, 2) }
    }

    private fun rejects(block: () -> Unit) {
        try { block(); fail("Expected a rejected response") } catch (_: IllegalArgumentException) { }
    }
}
