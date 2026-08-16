package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorklistFilterTest {
    private fun card(
        id: Int,
        given: String,
        family: String,
        status: String,
        conf: String? = null,
        mobile: String? = null,
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.F,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
        confNo = ConfNo.parseOrNull(conf),
        mobile = mobile,
    )

    private val rows = listOf(
        card(1, "Meera", "Deshpande", "Confirmed", "NF128", "+91 98220 41783"),
        card(2, "Rakesh", "Iyer", "Pending", null, "+91 50031 55402"),
        card(3, "Ananya", "Bhosale", "Received", "NF131"),
    )

    @Test
    fun allWhenNoChips() {
        assertEquals(3, WorklistFilter.visible(rows, emptySet(), "").size)
    }

    @Test
    fun multiSelectStatus() {
        val v = WorklistFilter.visible(rows, setOf("Pending", "Received"), "")
        assertEquals(listOf(2, 3), v.map { it.id.value })
    }

    @Test
    fun searchNameAndConfAndPhone() {
        assertEquals(1, WorklistFilter.visible(rows, emptySet(), "meera").size)
        assertEquals(1, WorklistFilter.visible(rows, emptySet(), "NF131").size)
        assertEquals(1, WorklistFilter.visible(rows, emptySet(), "50031").size)
    }

    @Test
    fun emptyWhenNothingMatches() {
        assertTrue(WorklistFilter.visible(rows, setOf("Cancelled"), "meera").isEmpty())
    }
}
