package org.dhamma.dipi.staff.desktop.derive

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeskLogicTest {
    private fun card(id: Int, conf: String, status: String = "Confirmed") = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "A$id",
        familyName = "Test",
        gender = Gender.F,
        status = ApplicantStatus(status),
        type = ApplicantType.Student,
        oldStudent = true,
        attended = false,
        confNo = ConfNo.parseOrNull(conf),
        mobile = "9822041783",
    )

    @Test
    fun rollDropsCancelled() {
        val rows = listOf(card(1, "NF1"), card(2, "NF2", "Cancelled"))
        assertEquals(1, deskRoll(rows).size)
    }

    @Test
    fun waNumberAdds91ForIndianMobile() {
        assertEquals("919822041783", deskWaNumber("9822041783"))
        assertEquals("14155552671", deskWaNumber("+1 415 555 2671"))
        assertNull(deskWaNumber(""))
    }

    @Test
    fun dayChipToday() {
        assertEquals("DAY 0 · TODAY", deskDayChip("2026-08-18", LocalDate.of(2026, 8, 18)))
        assertTrue(deskDayChip("2026-08-20", LocalDate.of(2026, 8, 18))!!.contains("STARTS IN"))
    }

    @Test
    fun stripHonorificMechanical() {
        assertEquals("Meera", stripHonorific("Mrs Meera"))
        // Single-word titles only; "Lt Col" is not stripped as a pair (Lt is).
        assertEquals("Col Smith", stripHonorific("Lt Col Smith"))
        assertEquals("Priya", stripHonorific("Priya"))
    }
}
