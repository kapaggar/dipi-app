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
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.photos.PhotoReviewScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
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

    /** Robolectric's legacy shadow lacks the overload ImageBitmap(w, h) uses. */
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
    fun reviewIsHonestAboutLocalQueueAndKeepsPlaceholders() {
        var uploads = 0
        rule.setContent {
            DipiTheme {
                PhotoReviewScreen(
                    people = listOf(card(1), card(2, given = "Arun", family = "Kale")),
                    suggestions = emptyList(),
                    edits = mapOf(ApplicantId(1) to PhotoEdit(rotate = 90, done = true)),
                    filter = "All",
                    onFilter = {},
                    onRotate = { _, _ -> },
                    onCrop = {},
                    onDone = {},
                    onUpload = { uploads++ },
                    pendingUploads = 1,
                    loadPhoto = { null },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Queue upload (1)").assertIsDisplayed().performClick()
        assertEquals(1, uploads)
        rule.onNodeWithText("Fixes stay on this device — live upload isn't available on the desk")
            .assertIsDisplayed()
        rule.onAllNodesWithText("▣").assertCountEquals(2)
    }

    @Test
    fun reviewShowsLivePhotoPerRow() {
        rule.setContent {
            DipiTheme {
                PhotoReviewScreen(
                    people = listOf(card(1)),
                    suggestions = emptyList(),
                    edits = emptyMap(),
                    filter = "All",
                    onFilter = {},
                    onRotate = { _, _ -> },
                    onCrop = {},
                    onDone = {},
                    onUpload = {},
                    pendingUploads = 0,
                    loadPhoto = { testPhoto() },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Photo of Priya Nair").assertIsDisplayed()
        rule.onAllNodesWithText("▣").assertCountEquals(0)
    }
}
