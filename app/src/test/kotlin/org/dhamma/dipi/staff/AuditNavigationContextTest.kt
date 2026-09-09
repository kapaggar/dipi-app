package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.desk.AuditOpenContext
import org.dhamma.dipi.staff.desk.deskApplicationList
import org.dhamma.dipi.staff.desk.deskSelectedApplicant
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.ui.DeskUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditNavigationContextTest {
    private fun card(id: Int, name: String, gender: Gender = Gender.F) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = name,
        familyName = "Synthetic",
        gender = gender,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
        confNo = ConfNo("NF$id"),
    )

    @Test
    fun pinKeepsEachOpenedApplicantEvenOutsideScope() {
        val a = card(11, "Ada")
        val b = card(22, "Bea", Gender.M)
        val scopedFemale = listOf(a)
        val selectedA = deskSelectedApplicant(scopedFemale, a.id, pinnedCard = a)
        val selectedB = deskSelectedApplicant(scopedFemale, b.id, pinnedCard = b)
        assertEquals(11, selectedA?.id?.value)
        assertEquals(22, selectedB?.id?.value)
        assertNotEquals(selectedA?.id, selectedB?.id)
        val list = deskApplicationList(scopedFemale, selectedB)
        assertTrue(list.any { it.id == b.id })
    }

    @Test
    fun auditContextCarriesRuleNotEvidence() {
        val ctx = AuditOpenContext("conf_no_duplicate", ApplicantId(22))
        val state = DeskUiState(auditOpenContext = ctx, deskAppPinned = true)
        assertEquals("conf_no_duplicate", state.auditOpenContext?.ruleId)
        assertEquals(22, state.auditOpenContext?.applicantId?.value)
        assertTrue(state.deskAppPinned)
    }
}
