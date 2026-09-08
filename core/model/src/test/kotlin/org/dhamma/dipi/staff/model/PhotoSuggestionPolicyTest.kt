package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoSuggestionPolicyTest {
    private val stamp = PhotoStamp("0".repeat(64), 1000, 1000)

    @Test
    fun noFaceIsNotSafe() {
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, emptyList()),
                PhotoOrientationEvidence(90, emptyList()),
                PhotoOrientationEvidence(180, emptyList()),
                PhotoOrientationEvidence(270, emptyList()),
            ),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.NO_FACE, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertNull(suggestion.recipe)
    }

    @Test
    fun multipleFacesAtAnAngleAreNotSafe() {
        val faces = listOf(
            FaceEvidence(PhotoBox(0.05, 0.10, 0.35, 0.40)),
            FaceEvidence(PhotoBox(0.55, 0.10, 0.35, 0.40)),
        )
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(PhotoOrientationEvidence(90, faces)),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.MULTIPLE_FACES, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertNull(suggestion.recipe)
    }

    @Test
    fun uprightZeroKeepsIdentityAndDoesNotRotate() {
        val upright = uprightFace(PhotoBox(0.25, 0.20, 0.50, 0.50))
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, listOf(upright)),
                PhotoOrientationEvidence(90, listOf(upright)),
                PhotoOrientationEvidence(180, listOf(sidewaysFace())),
                PhotoOrientationEvidence(270, emptyList()),
            ),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.NONE, suggestion.reason)
        assertNotEquals(PhotoSuggestionReason.ROTATE, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertEquals(PhotoRecipe(0, null), suggestion.recipe)
    }

    @Test
    fun oneClearlyUprightAlternateIsSafeRotate() {
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, listOf(sidewaysFace())),
                PhotoOrientationEvidence(90, listOf(uprightFace(PhotoBox(0.25, 0.20, 0.50, 0.50)))),
                PhotoOrientationEvidence(180, emptyList()),
                PhotoOrientationEvidence(270, emptyList()),
            ),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.ROTATE, suggestion.reason)
        assertTrue(suggestion.safeToApply)
        assertEquals(90, suggestion.recipe?.clockwise)
        assertNull(suggestion.recipe?.crop)
    }

    @Test
    fun ambiguousTwoUprightAlternatesAreNotSafe() {
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, listOf(sidewaysFace())),
                PhotoOrientationEvidence(90, listOf(uprightFace())),
                PhotoOrientationEvidence(180, emptyList()),
                PhotoOrientationEvidence(270, listOf(uprightFace())),
            ),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.AMBIGUOUS, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertNull(suggestion.recipe)
    }

    @Test
    fun upsideDownWithoutMouthOrNoseIsNotConfidentUpright() {
        val eyesOnly = FaceEvidence(
            box = PhotoBox(0.25, 0.20, 0.50, 0.50),
            leftEye = PhotoPoint(0.35, 0.38),
            rightEye = PhotoPoint(0.65, 0.38),
        )
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, listOf(eyesOnly)),
                PhotoOrientationEvidence(180, listOf(eyesOnly)),
            ),
            stamp,
        )
        assertFalse(PhotoSuggestionPolicy.isUpright(eyesOnly))
        assertEquals(PhotoSuggestionReason.AMBIGUOUS, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertNotEquals(PhotoSuggestionReason.ROTATE, suggestion.reason)
        assertNotEquals(PhotoSuggestionReason.ROTATE_AND_CROP, suggestion.reason)
    }

    @Test
    fun edgeClippedHeadCropIsNotSafeToApply() {
        val clipped = uprightFace(PhotoBox(0.35, 0.0, 0.30, 0.30))
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(PhotoOrientationEvidence(0, listOf(clipped))),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.CROP, suggestion.reason)
        assertFalse(suggestion.safeToApply)
        assertNotNull(suggestion.recipe?.crop)
    }

    @Test
    fun smallCentredFaceGetsSafeIntegerCrop() {
        val small = uprightFace(PhotoBox(0.35, 0.35, 0.30, 0.30))
        assertTrue(small.box.width * small.box.height < PhotoSuggestionPolicy.TINY_FACE_AREA)
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(PhotoOrientationEvidence(0, listOf(small))),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.CROP, suggestion.reason)
        assertTrue(suggestion.safeToApply)
        val crop = suggestion.recipe?.crop
        assertNotNull(crop)
        val (rw, rh) = PhotoGeometry.rotatedSize(stamp.width, stamp.height, 0)
        assertTrue(crop!!.x >= 0 && crop.y >= 0)
        assertTrue(crop.width > 0 && crop.height > 0)
        assertTrue(crop.x + crop.width <= rw)
        assertTrue(crop.y + crop.height <= rh)
        assertEquals(13.0 / 14.0, crop.width.toDouble() / crop.height, 0.05)
        PhotoGeometry.validate(stamp, suggestion.recipe!!)
    }

    @Test
    fun smallUprightAlternateGetsSafeRotateAndCrop() {
        val small = uprightFace(PhotoBox(0.35, 0.35, 0.30, 0.30))
        val suggestion = PhotoSuggestionPolicy.suggest(
            listOf(
                PhotoOrientationEvidence(0, listOf(sidewaysFace())),
                PhotoOrientationEvidence(90, listOf(small)),
            ),
            stamp,
        )
        assertEquals(PhotoSuggestionReason.ROTATE_AND_CROP, suggestion.reason)
        assertTrue(suggestion.safeToApply)
        assertEquals(90, suggestion.recipe?.clockwise)
        val crop = suggestion.recipe?.crop
        assertNotNull(crop)
        val (rw, rh) = PhotoGeometry.rotatedSize(stamp.width, stamp.height, 90)
        assertTrue(crop!!.x + crop.width <= rw)
        assertTrue(crop.y + crop.height <= rh)
    }

    private fun uprightFace(
        box: PhotoBox = PhotoBox(0.25, 0.20, 0.50, 0.50),
    ): FaceEvidence {
        val midX = box.x + box.width / 2.0
        val eyeY = box.y + box.height * 0.32
        val span = box.width * 0.28
        return FaceEvidence(
            box = box,
            leftEye = PhotoPoint(midX - span, eyeY),
            rightEye = PhotoPoint(midX + span, eyeY),
            nose = PhotoPoint(midX, box.y + box.height * 0.55),
            mouth = PhotoPoint(midX, box.y + box.height * 0.75),
        )
    }

    private fun sidewaysFace(
        box: PhotoBox = PhotoBox(0.25, 0.20, 0.50, 0.50),
    ): FaceEvidence = FaceEvidence(
        box = box,
        leftEye = PhotoPoint(box.x + box.width * 0.32, box.y + box.height * 0.25),
        rightEye = PhotoPoint(box.x + box.width * 0.38, box.y + box.height * 0.75),
        nose = PhotoPoint(box.x + box.width * 0.55, box.y + box.height * 0.50),
        mouth = PhotoPoint(box.x + box.width * 0.78, box.y + box.height * 0.50),
    )
}
