package org.dhamma.dipi.staff

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoReviewState
import org.dhamma.dipi.staff.photos.PhotoEditor
import org.dhamma.dipi.staff.photos.PhotoEditorUi
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PhotoEditorTest {
    @get:Rule
    val rule = createComposeRule()

    private val editor = PhotoEditorUi(
        applicantId = 9,
        name = "Priya Nair",
        recipe = PhotoRecipe(),
        review = PhotoReviewState.UNREVIEWED,
        sourceWidth = 8,
        sourceHeight = 10,
    )

    private fun show() {
        val bitmap = Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888).asImageBitmap()
        rule.setContent {
            DipiTheme {
                PhotoEditor(
                    editor = editor,
                    onAction = {},
                    loadOriginal = { bitmap },
                    loadCorrected = { bitmap },
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun landscapeKeepsRotateAndKeepReachable() {
        show()
        rule.onNodeWithContentDescription("Rotate right").assertIsDisplayed()
        rule.onNodeWithText("Keep & next").assertIsDisplayed()
        rule.onNodeWithContentDescription("Crop frame").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun narrowPortraitKeepsActionsReachable() {
        show()
        rule.onNodeWithContentDescription("Rotate left").assertIsDisplayed()
        rule.onNodeWithText("Save draft").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port", fontScale = 1.3f)
    fun largeFontKeepsActionsReachable() {
        show()
        rule.onNodeWithContentDescription("Rotate 180").assertIsDisplayed()
        rule.onNodeWithText("Keep & next").assertIsDisplayed()
    }
}
