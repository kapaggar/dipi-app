package org.dhamma.dipi.staff

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.photos.PhotoCardUi
import org.dhamma.dipi.staff.photos.PhotoReviewScreen
import org.dhamma.dipi.staff.photos.PhotoReviewUiState
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Live photo loading in the desk detail pane and the phone review screen. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class PhotoPanesTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card(
        id: Int,
        given: String = "Priya",
        family: String = "Nair",
    ) = ApplicantCard(
        id = ApplicantId(id),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = given,
        familyName = family,
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
        confNo = ConfNo("NF$id"),
        mobile = "9876543210",
        city = "Pune",
        age = 34,
    )

    private fun testPhoto(): ImageBitmap =
        Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888).asImageBitmap()

    @Test
    fun detailShowsLivePhotoAndDropsInitials() {
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = listOf(card(1)),
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    loadPhoto = { testPhoto() },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Photo of Priya Nair").assertIsDisplayed()
        rule.onAllNodesWithText("PN").assertCountEquals(0)
    }

    @Test
    fun detailKeepsInitialsWhenPhotoUnavailable() {
        rule.setContent {
            DipiTheme {
                ApplicationsPane(
                    rows = listOf(card(1)),
                    flagsById = emptyMap(),
                    selectedId = ApplicantId(1),
                    onSelect = {},
                    onChangeStatus = {},
                    onDial = {},
                    onEdit = {},
                    loadPhoto = { null },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("PN").assertIsDisplayed()
    }

    @Test
    fun reviewIsHonestAboutLocalExportAndKeepsPlaceholders() {
        val priya = card(1)
        val arun = card(2, given = "Arun", family = "Kale")
        rule.setContent {
            DipiTheme {
                PhotoReviewScreen(
                    state = PhotoReviewUiState(
                        cards = listOf(
                            PhotoCardUi(priya, "Draft"),
                            PhotoCardUi(arun, "Unreviewed"),
                        ),
                        readyCount = 1,
                    ),
                    onAction = {},
                    loadPreview = { null },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Export (1)").assertIsDisplayed()
        rule.onNodeWithText("Update on desk (1)").assertIsDisplayed()
        rule.onNodeWithText("Ready 1").assertIsDisplayed()
        rule.onNodeWithText("Uploaded").assertDoesNotExist()
        rule.onNodeWithText("Queue upload (1)").assertDoesNotExist()
        rule.onNodeWithText(
            "Update on desk posts the current application form with the corrected photo. " +
                "The desk save can still change other fields if it transforms what was echoed.",
            substring = true,
        ).assertIsDisplayed()
        rule.onAllNodesWithText("▣").assertCountEquals(2)
    }

    @Test
    fun reviewShowsLivePhotoPerRow() {
        val priya = card(1)
        rule.setContent {
            DipiTheme {
                PhotoReviewScreen(
                    state = PhotoReviewUiState(
                        cards = listOf(PhotoCardUi(priya, "Unreviewed")),
                    ),
                    onAction = {},
                    loadPreview = { testPhoto() },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Photo of Priya Nair").assertIsDisplayed()
        rule.onAllNodesWithText("▣").assertCountEquals(0)
    }
}
