package org.dhamma.dipi.staff

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.desk.AuditPane
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.AuditFlag
import org.dhamma.dipi.staff.model.AuditSeverity
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Portrait desk: 840dp screen leaves a 650dp pane after the 190dp rail. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w840dp-h1280dp-port")
class Desk22DetailsTest {
    @get:Rule val rule = createComposeRule()
    private var backDispatcher: OnBackPressedDispatcher? = null

    @Test
    fun compactApplicationsStartAsAListThenBackReturnsToThatList() {
        rule.setContent {
            var selected by remember { mutableStateOf<ApplicantId?>(null) }
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            DipiTheme {
                Box(Modifier.width(650.dp).height(900.dp)) {
                    ApplicationsPane(
                        rows = listOf(card(1, "Priya Nair")),
                        flagsById = emptyMap(),
                        selectedId = selected,
                        onSelect = { selected = it.id },
                        onChangeStatus = {}, onDial = {}, onEdit = {},
                    )
                }
            }
        }

        rule.onNodeWithText("ID VERIFICATION").assertDoesNotExist()
        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithText("← Back to list").assertIsDisplayed()
        rule.runOnIdle { backDispatcher!!.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithText("ID VERIFICATION").assertDoesNotExist()
        rule.onNodeWithText("Priya Nair").assertIsDisplayed()
    }

    @Test
    fun applicantIdIsMaskedUntilRevealAndResetsWhenNavigationSelectsAnotherApplicant() {
        val firstId = "9999 1234 5678"
        val secondId = "1234 5678 9012"
        rule.setContent {
            var selected by remember { mutableStateOf<ApplicantId?>(null) }
            DipiTheme {
                Box(Modifier.width(650.dp).height(900.dp)) { ApplicationsPane(
                    rows = listOf(card(1, "Priya Nair"), card(2, "Meera Shah")),
                    flagsById = emptyMap(), selectedId = selected,
                    onSelect = { selected = it.id },
                    onChangeStatus = {}, onDial = {}, onEdit = {},
                    sensitiveById = mapOf(
                        ApplicantId(1) to SensitiveInfo("Aadhaar", firstId),
                        ApplicantId(2) to SensitiveInfo("Aadhaar", secondId),
                    ),
                ) }
            }
        }

        rule.onNodeWithText("Priya Nair").performClick()
        rule.onNodeWithText(firstId).assertDoesNotExist()
        rule.onNodeWithText("Reveal").performClick()
        rule.onNodeWithText("Hide").assertIsDisplayed()
        rule.onNodeWithText(firstId).assertIsDisplayed()
        rule.onNodeWithText("← Back to list").performClick()
        rule.onNodeWithText("Meera Shah").performClick()
        rule.onNodeWithText("Hide").assertDoesNotExist()
        rule.onNodeWithText("Reveal").assertIsDisplayed()
        rule.onNodeWithText(secondId).assertDoesNotExist()
    }

    @Test
    fun compactAuditStartsAsAListAndBackDismissesItsDetail() {
        val flagged = card(1, "Priya Nair").copy(flags = listOf(
            AuditFlag(AuditSeverity.HARD, "Mobile number missing", "Add a number", "missing_mobile"),
        ))
        rule.setContent {
            var code by remember { mutableStateOf<String?>(null) }
            DipiTheme {
                Box(Modifier.width(650.dp).height(900.dp)) { AuditPane(
                    flagged = listOf(flagged), selectedCode = code,
                    onSelect = { code = it }, onBatch = { _, _ -> }, onOpen = {},
                ) }
            }
        }

        rule.onNodeWithText("← Back to list").assertDoesNotExist()
        rule.onNodeWithText("Mobile number missing").performClick()
        rule.onNodeWithText("← Back to list").assertIsDisplayed().performClick()
        rule.onNodeWithText("Mobile number missing").assertIsDisplayed()
    }

    @Test
    fun externalDetailTargetOpensWithoutClearingListFilters() {
        val target = card(7, "Outside Applicant")
        rule.setContent {
            DipiTheme {
                Box(Modifier.width(650.dp).height(900.dp)) {
                    ApplicationsPane(rows = emptyList(), flagsById = emptyMap(), selectedId = target.id,
                        onSelect = {}, onChangeStatus = {}, onDial = {}, onEdit = {},
                        gender = "Male", detailTarget = target, openDetailRequest = 1)
                }
            }
        }
        rule.onNodeWithText("Outside Applicant").assertIsDisplayed()
        rule.onNodeWithText("Outside current list filters · opened from another view").assertIsDisplayed()
        rule.onNodeWithText("← Back to list").performClick()
        rule.onNodeWithText("No applications match these filters.").assertIsDisplayed()
    }

    private fun card(id: Int, name: String) = ApplicantCard(
        id = ApplicantId(id), centreId = CentreId(1), courseId = CourseId(1),
        givenName = name.substringBefore(' '), familyName = name.substringAfter(' '),
        gender = Gender.F, status = ApplicantStatus("Confirmed"), type = ApplicantType.Student,
        oldStudent = false, attended = false, confNo = ConfNo("NF$id"), age = 30,
    )
}
