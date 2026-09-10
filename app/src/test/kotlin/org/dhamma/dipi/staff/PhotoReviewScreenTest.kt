package org.dhamma.dipi.staff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.dhamma.dipi.staff.datastore.PhotoCorrectionStore
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.ConfNo
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamps
import org.dhamma.dipi.staff.network.PhotoSource
import org.dhamma.dipi.staff.network.PhotoSourceResult
import org.dhamma.dipi.staff.photos.PhotoReviewAction
import org.dhamma.dipi.staff.photos.PhotoReviewController
import org.dhamma.dipi.staff.photos.PhotoReviewScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land")
class PhotoReviewScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun card(id: Int, given: String, family: String) = ApplicantCard(
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
    )

    private fun controller(): PhotoReviewController {
        val app = RuntimeEnvironment.getApplication()
        val prefs = app.getSharedPreferences("pc-review-ui", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val bitmap = Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val stamp = PhotoStamps.ofEncoded(byteArrayOf(1, 2, 3, 4), 8, 10)
        val source = PhotoSource(stamp, bitmap)
        return PhotoReviewController(
            store = PhotoCorrectionStore { prefs },
            scope = CoroutineScope(Dispatchers.Main.immediate),
            sources = { _, _ -> PhotoSourceResult.Ready(source) },
            enabled = true,
        )
    }

    @Test
    fun rotateThenKeepAndNextMarksReadyWithoutUploaded() {
        val photos = controller()
        val people = listOf(card(1, "Priya", "Nair"), card(2, "Arun", "Kale"))
        val scope = PhotoScope("https://one.example.test", 1, 10)
        photos.dispatch(PhotoReviewAction.Open(scope, people, focusApplicantId = 1))
        rule.setContent {
            val state by photos.state.collectAsState()
            DipiTheme {
                PhotoReviewScreen(
                    state = state,
                    onAction = photos::dispatch,
                    loadPreview = { photos.preview(it) },
                    loadOriginal = photos::original,
                    loadCorrected = photos::corrected,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Rotate right").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Keep & next").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Ready 1").assertIsDisplayed()
        rule.onNodeWithText("Uploaded").assertDoesNotExist()
    }

    @Test
    fun unreviewedLabelAndMissingPlaceholder() {
        val photos = controller()
        val people = listOf(card(3, "No", "Photo"))
        photos.dispatch(PhotoReviewAction.Open(PhotoScope("https://one.example.test", 1, 10), people))
        rule.setContent {
            DipiTheme {
                PhotoReviewScreen(
                    state = photos.state.value,
                    onAction = {},
                    loadPreview = { null },
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Unreviewed").assertIsDisplayed()
        rule.onNodeWithText("▣").assertIsDisplayed()
        rule.onNodeWithText("Uploaded").assertDoesNotExist()
    }
}
