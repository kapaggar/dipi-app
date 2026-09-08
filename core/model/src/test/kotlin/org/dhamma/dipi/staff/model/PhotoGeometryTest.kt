package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoGeometryTest {
    private val hash = "0".repeat(64)
    private val source = PhotoStamp(hash, 2, 3)

    @Test
    fun rotatedSizesMatchRightAngles() {
        assertEquals(2 to 3, PhotoGeometry.rotatedSize(2, 3, 0))
        assertEquals(3 to 2, PhotoGeometry.rotatedSize(2, 3, 90))
        assertEquals(2 to 3, PhotoGeometry.rotatedSize(2, 3, 180))
        assertEquals(3 to 2, PhotoGeometry.rotatedSize(2, 3, 270))
    }

    @Test
    fun goldenVectorsCoverAllClockwiseAngles() {
        assertEquals(listOf('A', 'B', 'C', 'D', 'E', 'F'), PhotoGeometry.goldenLetters(0))
        assertEquals(listOf('E', 'C', 'A', 'F', 'D', 'B'), PhotoGeometry.goldenLetters(90))
        assertEquals(listOf('F', 'E', 'D', 'C', 'B', 'A'), PhotoGeometry.goldenLetters(180))
        assertEquals(listOf('B', 'D', 'F', 'A', 'C', 'E'), PhotoGeometry.goldenLetters(270))
    }

    @Test
    fun cropAfterNinetyMatchesGoldenBlock() {
        val rotated = PhotoGeometry.goldenLetters(90)
        val cropped = PhotoGeometry.cropLetters(rotated, 3, 2, PhotoCrop(1, 0, 2, 2))
        assertEquals(listOf('C', 'A', 'D', 'B'), cropped)
    }

    @Test
    fun fullFrameCropCanonicalizesToNull() {
        val identity = PhotoGeometry.validate(source, PhotoRecipe(0, PhotoCrop(0, 0, 2, 3)))
        assertTrue(PhotoGeometry.isIdentity(identity))
        assertNull(identity.crop)
        val rotated = PhotoGeometry.validate(source, PhotoRecipe(90, PhotoCrop(0, 0, 3, 2)))
        assertEquals(PhotoRecipe(90, null), rotated)
        assertFalse(PhotoGeometry.isIdentity(rotated))
    }

    @Test
    fun rejectsCropOutsideRotatedSource() {
        val bad = PhotoRecipe(90, PhotoCrop(2, 0, 2, 2))
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometry.validate(source, bad)
        }
    }

    @Test
    fun rejectsInvalidRotationsAndOverflowingCrops() {
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometry.validate(source, PhotoRecipe(45, null))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometry.rotatedSize(2, 3, 360)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometry.validate(source, PhotoRecipe(0, PhotoCrop(Int.MAX_VALUE, 0, 2, 1)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoGeometry.validate(source, PhotoRecipe(0, PhotoCrop(0, 0, 0, 1)))
        }
    }

    @Test
    fun mapsExistingCropThroughFurtherRotation() {
        val after90 = PhotoGeometry.rotateCrop(PhotoCrop(0, 1, 2, 2), 2, 3, 90)
        assertEquals(PhotoCrop(0, 0, 2, 2), after90)
        val after180 = PhotoGeometry.rotateCrop(PhotoCrop(0, 1, 2, 2), 2, 3, 180)
        assertEquals(PhotoCrop(0, 0, 2, 2), after180)
        val after270 = PhotoGeometry.rotateCrop(PhotoCrop(0, 1, 2, 2), 2, 3, 270)
        assertEquals(PhotoCrop(1, 0, 2, 2), after270)
        val composed = PhotoGeometry.applyRotation(source, PhotoRecipe(90, PhotoCrop(1, 0, 2, 2)), 90)
        assertEquals(PhotoRecipe(180, PhotoCrop(0, 1, 2, 2)), composed)
    }

    @Test
    fun originCanonicalizesSchemeHostPortAndBasePath() {
        assertEquals("https://dipi.vridhamma.org", PhotoOrigins.canonicalize("https://dipi.vridhamma.org:443/"))
        assertEquals("https://dipi.vridhamma.org/desk", PhotoOrigins.canonicalize("HTTPS://DIPI.VRIDHAMMA.ORG/desk/"))
        assertEquals("http://127.0.0.1:8080", PhotoOrigins.canonicalize("http://127.0.0.1:8080/"))
        assertThrows(IllegalArgumentException::class.java) {
            PhotoOrigins.canonicalize("https://user:pass@dipi.vridhamma.org")
        }
        PhotoScope("https://one.example.test", 1, 2)
        PhotoScope("http://127.0.0.1:9", 1, 2)
        assertThrows(IllegalArgumentException::class.java) {
            PhotoScope("https://one.example.test/", 1, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoScope("https://one.example.test", 0, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PhotoKey(PhotoScope("https://one.example.test", 1, 2), 0)
        }
    }

    @Test
    fun editsRejectSourceMismatchAndIdentityApprovalIsNotReady() {
        val key = PhotoKey(PhotoScope("https://one.example.test", 1, 10), 99)
        val other = PhotoStamp("1".repeat(64), 2, 3)
        val draft = PhotoDraft(key, source)
        val rotated = PhotoDrafts.applyRecipe(draft, source, PhotoRecipe(90))
        assertEquals(1L, rotated.editRevision)
        assertEquals(PhotoReviewState.DRAFT, rotated.review)
        assertThrows(IllegalArgumentException::class.java) {
            PhotoDrafts.applyRecipe(draft, other, PhotoRecipe(90))
        }
        val approvedIdentity = PhotoDrafts.approve(draft, source)
        assertEquals(PhotoReviewState.APPROVED, approvedIdentity.review)
        assertFalse(PhotoDrafts.isReady(approvedIdentity, source))
        val approved = PhotoDrafts.approve(rotated, source)
        assertTrue(PhotoDrafts.isReady(approved, source))
        val edited = PhotoDrafts.applyRecipe(
            approved.copy(write = PhotoWriteState.COMMITTED),
            source,
            PhotoRecipe(180),
        )
        assertEquals(PhotoReviewState.DRAFT, edited.review)
        assertEquals(2L, edited.editRevision)
        assertEquals(PhotoWriteState.NOT_STARTED, edited.write)
        assertFalse(PhotoDrafts.isReady(edited, source))
        assertEquals(1, PhotoDrafts.readyCount(listOf(approved, approvedIdentity), mapOf(key to source)))
        assertEquals(PhotoWriteState.UNKNOWN, PhotoDrafts.restoreWrite(PhotoWriteState.SUBMITTING))
        assertEquals(PhotoWriteState.UNKNOWN, PhotoDrafts.restoreWrite(PhotoWriteState.VERIFYING))
        assertEquals(PhotoWriteState.COMMITTED, PhotoDrafts.restoreWrite(PhotoWriteState.COMMITTED))
        val rebound = PhotoDrafts.bindSource(approved, other)
        assertEquals(PhotoReviewState.SOURCE_CHANGED, rebound.review)
        assertTrue(PhotoGeometry.isIdentity(rebound.recipe))
    }

    @Test
    fun hashesEncodedBytesNotDecodedPixels() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val stamp = PhotoStamps.ofEncoded(bytes, 2, 2)
        assertEquals(64, stamp.sha256.length)
        assertTrue(PhotoStamps.isSha256Hex(stamp.sha256))
        assertEquals(PhotoStamps.sha256Hex(bytes), stamp.sha256)
        assertThrows(IllegalArgumentException::class.java) {
            PhotoStamp("A".repeat(64), 2, 2)
        }
    }
}
