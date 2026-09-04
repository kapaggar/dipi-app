package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameTest {
    private fun card(given: String, family: String) = ApplicantCard(
        id = ApplicantId(1),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
    )

    @Test
    fun emptySurnameDashIsNotJoinedOntoTheGivenName() {
        assertEquals("Savita", card("Savita", "—").displayName)
        assertEquals("Savita", card("Savita", "-").displayName)
        assertEquals("Savita", card("Savita", "").displayName)
        assertEquals("Savita Kumar", card("Savita", "Kumar").displayName)
    }
}
