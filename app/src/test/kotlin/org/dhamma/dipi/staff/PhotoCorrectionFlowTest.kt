package org.dhamma.dipi.staff

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.datastore.PhotoCorrectionStore
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoDrafts
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamps
import org.dhamma.dipi.staff.model.PhotoWriteState
import org.dhamma.dipi.staff.network.PhotoDeskWriteResult
import org.dhamma.dipi.staff.network.PhotoSource
import org.dhamma.dipi.staff.network.PhotoSourceResult
import org.dhamma.dipi.staff.photos.PhotoDeskWrite
import org.dhamma.dipi.staff.photos.PhotoReviewAction
import org.dhamma.dipi.staff.photos.PhotoReviewController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class PhotoCorrectionFlowTest {
    private val scope = PhotoScope("https://one.example.test", 1, 10)
    private val stamp = PhotoStamps.ofEncoded(byteArrayOf(4, 5, 6), 8, 10)

    private fun card() = ApplicantCard(
        id = ApplicantId(41),
        centreId = CentreId(1),
        courseId = CourseId(10),
        givenName = "Priya",
        familyName = "Nair",
        gender = Gender.F,
        status = ApplicantStatus("Confirmed"),
        type = ApplicantType.Student,
        oldStudent = false,
        attended = false,
    )

    private fun controller(): PhotoReviewController {
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("pc-flow", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val bitmap = Bitmap.createBitmap(8, 10, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return PhotoReviewController(
            store = PhotoCorrectionStore { prefs },
            scope = CoroutineScope(Dispatchers.Main.immediate),
            sources = { _, _ -> PhotoSourceResult.Ready(PhotoSource(stamp, bitmap)) },
        )
    }

    @Test
    fun approvedCorrectionDoesNotMutateWriteStateOrOriginal() = runBlocking {
        val photos = controller()
        photos.dispatch(PhotoReviewAction.Open(scope, listOf(card()), 41))
        photos.dispatch(PhotoReviewAction.Rotate(90))
        photos.dispatch(PhotoReviewAction.KeepAndNext)
        val draft = photos.state.value.drafts.getValue(41)
        assertEquals(PhotoRecipe(90), draft.recipe)
        assertTrue(PhotoDrafts.isReady(draft, stamp))
        assertEquals(PhotoWriteState.NOT_STARTED, draft.write)
        assertNotEquals(PhotoWriteState.COMMITTED, draft.write)
        val original = photos.original(41)!!
        val corrected = photos.corrected(41)!!
        assertEquals(8, original.width)
        assertEquals(10, original.height)
        assertEquals(10, corrected.width)
        assertEquals(8, corrected.height)
        photos.dispatch(PhotoReviewAction.PrepareExport)
        assertTrue(photos.state.value.exportRequest != null)
        assertFalse(photos.state.value.drafts.getValue(41).write == PhotoWriteState.COMMITTED)
    }

    @Test
    fun deskWriteMarksCommitted() = runBlocking {
        val photos = controller()
        var writes = 0
        photos.deskWrite = PhotoDeskWrite { _, jpeg, name ->
            writes += 1
            assertTrue(jpeg.isNotEmpty())
            assertEquals("photo-41-corrected.jpg", name)
            PhotoDeskWriteResult.Committed("Updated the application on the desk")
        }
        photos.dispatch(PhotoReviewAction.Open(scope, listOf(card()), 41))
        photos.dispatch(PhotoReviewAction.Rotate(90))
        photos.dispatch(PhotoReviewAction.Approve)
        photos.dispatch(PhotoReviewAction.PrepareUpdate)
        assertTrue(photos.state.value.confirmUpdate)
        photos.dispatch(PhotoReviewAction.StartUpdate)
        val deadline = System.currentTimeMillis() + 5_000
        while (photos.state.value.updating && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            delay(20)
        }
        assertEquals(1, writes)
        assertEquals(PhotoWriteState.COMMITTED, photos.state.value.drafts.getValue(41).write)
        assertTrue(photos.state.value.snack?.contains("desk") == true)
    }

    @Test
    fun courseOpsCannotUpload() {
        val photos = controller()
        var writes = 0
        photos.allowDeskWrite = false
        photos.deskWrite = PhotoDeskWrite { _, _, _ ->
            writes += 1
            PhotoDeskWriteResult.Committed("Updated the application on the desk")
        }
        photos.dispatch(PhotoReviewAction.Open(scope, listOf(card()), 41))
        photos.dispatch(PhotoReviewAction.Rotate(90))
        photos.dispatch(PhotoReviewAction.Approve)
        photos.dispatch(PhotoReviewAction.PrepareUpdate)
        assertEquals(0, writes)
        assertFalse(photos.state.value.confirmUpdate)
        assertEquals("Course ops cannot update applications", photos.state.value.snack)
        photos.dispatch(PhotoReviewAction.StartUpdate)
        assertEquals(0, writes)
        assertEquals(PhotoWriteState.NOT_STARTED, photos.state.value.drafts.getValue(41).write)
    }

    @Test
    fun identityApprovalIsNotReady() {
        val photos = controller()
        photos.dispatch(PhotoReviewAction.Open(scope, listOf(card()), 41))
        photos.dispatch(PhotoReviewAction.Approve)
        val draft = photos.state.value.drafts.getValue(41)
        assertFalse(PhotoDrafts.isReady(draft, stamp))
        assertEquals(0, photos.state.value.readyCount)
    }
}
