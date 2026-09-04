package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPageParserTest {
    private val html = """
        <form>
        <input type="hidden" name="form_build_id" value="form-abc" />
        <input type="hidden" name="form_token" value="tok-1" />
        <input type="hidden" name="form_id" value="dh_manageapp_search_form" />
        <select id="edit-course" name="course">
          <option value="">Choose</option>
          <option value="42">Dhamma Sudha / 10-Day / 20 Aug 2026 - 31 Aug 2026</option>
        </select>
        <select id="edit-app-status" name="status">
          <option value="">Choose</option>
          <option value="Pending">Pending</option>
          <option value="Confirmed">Confirmed</option>
        </select>
        </form>
        <script>
        (function () {
            var dataset = [{"aid":99,"name":"<a href=\"/app/99/edit\">Meera Deshpande</a> (PDF)","gender":"Female","o_n":"Old<br>Female","courseid":42,"centreid":1,"app_status":"Confirmed","confno":"NF128","city":"Pune","state":"Maharashtra","country":"India","age":34,"contact_mobile":"+91 98220 41783","contact_email":"m@x.com","type":"student","aadhar":"1234","passport":"X","emergency_name":"Ravi Deshpande","emergency_num":"098220 41783"}];
            var letters = [];
        })();
        </script>
    """.trimIndent()

    @Test
    fun extractsTokensCoursesAndDatasetWithoutNpi() {
        val page = SearchPageParser.parse(html, pathCentreId = 1)
        assertEquals("form-abc", page.tokens!!.formBuildId)
        assertEquals("tok-1", page.tokens!!.formToken)
        assertEquals(1, page.courses.size)
        assertEquals(42, page.courses[0].id)
        assertTrue(page.statuses.contains("Confirmed"))
        assertEquals(1, page.dataset.size)
        val a = page.dataset[0]
        assertEquals(99, a.id)
        assertEquals("Meera", a.givenName)
        assertEquals("Deshpande", a.familyName)
        assertEquals("Confirmed", a.status)
        assertEquals("NF128", a.confNo)
        assertEquals("F", a.gender)
        assertTrue(a.oldStudent)
        assertFalse(a.toString().contains("1234"))
        assertNotNull(a.email)
        // Presence/equality booleans computed at parse time, raw values dropped.
        assertEquals(true, a.idPresent)
        assertEquals(true, a.emergencyPresent)
        assertEquals(true, a.emergencyNamePresent)
        // Emergency number is the own mobile (last 10 digits match).
        assertEquals(true, a.emergencyEqSelf)
        assertFalse(a.toString().contains("Ravi"))
    }

    @Test
    fun presenceBooleansWhenIdAndEmergencyDiffer() {
        val html2 = """
            <script>
            var dataset = [{"aid":7,"name":"Priya Nair","gender":"Female","courseid":42,"centreid":1,
            "app_status":"Expected","contact_mobile":"+91 98220 41783",
            "emergency_name":"Anil Nair","emergency_num":"9000011122"}];
            </script>
        """.trimIndent()
        val a = SearchPageParser.parse(html2).dataset.single()
        assertEquals(false, a.idPresent)
        assertEquals(true, a.emergencyPresent)
        assertEquals(true, a.emergencyNamePresent)
        assertEquals(false, a.emergencyEqSelf)
        // The emergency number itself must never survive parsing.
        assertFalse(a.toString().contains("9000011122"))
    }

    @Test
    fun presenceBooleansWhenEmergencyAbsent() {
        val html3 = """
            <script>
            var dataset = [{"aid":8,"name":"Rohan Kulkarni","gender":"Male","courseid":42,"centreid":1,
            "app_status":"Expected","contact_mobile":"+91 98220 41783","voterid":"ZZZ"}];
            </script>
        """.trimIndent()
        val a = SearchPageParser.parse(html3).dataset.single()
        assertEquals(true, a.idPresent)
        assertEquals(false, a.emergencyPresent)
        assertEquals(false, a.emergencyNamePresent)
        assertEquals(false, a.emergencyEqSelf)
        assertFalse(a.toString().contains("ZZZ"))
    }

    @Test
    fun displayNameDropsThePdfLinkRemnantInAnySpacingOrCase() {
        val html = """
            <script>
            var dataset = [
              {"aid":1,"name":"<a href=\"/app/1/edit\">Meera Deshpande</a> ( PDF )","gender":"Female","courseid":42,"centreid":1,"app_status":"Confirmed"},
              {"aid":2,"name":"Arun Kale (PDF)","gender":"Male","courseid":42,"centreid":1,"app_status":"Pending"},
              {"aid":3,"name":"Priya Nair (  pdf  )","gender":"Female","courseid":42,"centreid":1,"app_status":"Expected"},
              {"aid":4,"name":"Rohan Kulkarni","gender":"Male","courseid":42,"centreid":1,"app_status":"Expected"},
              {"aid":5,"name":"<a href=\"/application-view/5\">Sunita Rao</a> ( View )","gender":"Female","courseid":42,"centreid":1,"app_status":"Confirmed"},
              {"aid":6,"name":"Kiran Joshi (View)","gender":"Male","courseid":42,"centreid":1,"app_status":"Pending"},
              {"aid":7,"name":"Anil Patil (  view  )","gender":"Male","courseid":42,"centreid":1,"app_status":"Expected"},
              {"aid":8,"name":"Latha Iyer ( PDF ) ( View )","gender":"Female","courseid":42,"centreid":1,"app_status":"Confirmed"}
            ];
            </script>
        """.trimIndent()
        val rows = SearchPageParser.parse(html).dataset
        assertEquals(
            listOf("Meera", "Arun", "Priya", "Rohan", "Sunita", "Kiran", "Anil", "Latha"),
            rows.map { it.givenName },
        )
        assertEquals(
            listOf("Deshpande", "Kale", "Nair", "Kulkarni", "Rao", "Joshi", "Patil", "Iyer"),
            rows.map { it.familyName },
        )
        rows.forEach { assertFalse(it.familyName.contains("PDF", ignoreCase = true)) }
        rows.forEach { assertFalse(it.familyName.contains("View", ignoreCase = true)) }
    }

    @Test
    fun stripTagsDecodesNumericApostrophe() {
        assertEquals("Master's", SearchPageParser.stripTags("Master&#039;s"))
        assertEquals("Master's", SearchPageParser.stripTags("Master&apos;s"))
        assertEquals("say \"hi\"", SearchPageParser.stripTags("say &quot;hi&quot;"))
        assertEquals("say \"hi\"", SearchPageParser.stripTags("say &#34;hi&#34;"))
        assertEquals("Master's", SearchPageParser.stripTags("Master&#x27;s"))
        assertEquals("A & B < C > D", SearchPageParser.stripTags("A &amp; B &lt; C &gt; D"))
    }

    @Test
    fun cleanPersonNameDropsATrailingSurnameDash() {
        assertEquals("Savita", cleanPersonName("Savita —"))
        assertEquals("Savita", cleanPersonName("Savita -"))
        assertEquals("Savita Kumar", cleanPersonName("Savita Kumar"))
        val html = """
            <script>
            var dataset = [{"aid":11,"name":"Savita —","gender":"Female","courseid":42,"centreid":1,"app_status":"Confirmed"}];
            </script>
        """.trimIndent()
        val a = SearchPageParser.parse(html).dataset.single()
        assertEquals("Savita", a.givenName)
        assertEquals("", a.familyName)
    }

    @Test
    fun centreIdFromPath() {
        assertEquals(7, SearchPageParser.centreIdFromPath("/search-app/7"))
        assertEquals(91, SearchPageParser.centreIdFromPath("/centre/91"))
        assertEquals(null, SearchPageParser.centreIdFromPath("/search-app"))
    }

    @Test
    fun loginBlockAndDashboardCourses() {
        val loginHtml = """
            <form action="/home?destination=home" method="post" id="user-login-form">
            <input type="hidden" name="form_build_id" value="form-XYZ" />
            <input type="hidden" name="form_id" value="user_login_block" />
            </form>
        """.trimIndent()
        val block = SearchPageParser.loginBlock(loginHtml)!!
        assertEquals("form-XYZ", block.formBuildId)
        assertEquals("user_login_block", block.formId)
        assertEquals("/home?destination=home", block.action)

        val dash = """
            <title>Manage Dhamma Ganga | Dīpi</title>
            <h1>Manage Dhamma Ganga</h1>
            <h2>Upcoming Courses</h2>
            <div class="table-heading"><a href="/course/91/68669">Dhamma Ganga / STP / 2026 / 19th-Aug to 27th-Aug</a></div>
            <div class="table-heading"><a href="/course/91/68670">Dhamma Ganga / 10 Day / 2026 / 2nd-Sep to 13th-Sep</a></div>
        """.trimIndent()
        assertEquals("Dhamma Ganga", SearchPageParser.centreName(dash))
        val courses = SearchPageParser.coursesFromDashboard(dash)
        assertEquals(listOf(68669, 68670), courses.map { it.id })
        assertTrue(courses[0].label.contains("STP"))
    }

    @Test
    fun loginBlockFromLive403Snippet() {
        val html = """
            <title>Access denied | Dīpi</title>
            <h1>Access denied</h1>
            <form action="/home?destination=home" method="post" id="user-login-form" accept-charset="UTF-8">
            <input type="hidden" name="form_build_id" value="form-9l-lBifyxxW8im7VDQskA8QTxthgW4IyYdqrvwz5EcM" />
            <input type="hidden" name="form_id" value="user_login_block" />
            </form>
        """.trimIndent()
        val block = SearchPageParser.loginBlock(html)!!
        assertTrue(block.formBuildId.startsWith("form-9l"))
        assertEquals("user_login_block", block.formId)
        assertEquals("/home?destination=home", block.action)
    }

    @Test
    fun loginBlockFromUserLoginPage() {
        val html = """
            <title>User account | Dīpi</title>
            <form action="/user/login" method="post" id="user-login" accept-charset="UTF-8">
            <input type="hidden" name="form_build_id" value="form-user-login-page" />
            <input type="hidden" name="form_id" value="user_login" />
            <button type="submit" name="op" value="Log in">Log in</button>
            </form>
        """.trimIndent()
        val block = SearchPageParser.loginBlock(html)!!
        assertEquals("form-user-login-page", block.formBuildId)
        assertEquals("user_login", block.formId)
        assertEquals("/user/login", block.action)
    }
}
