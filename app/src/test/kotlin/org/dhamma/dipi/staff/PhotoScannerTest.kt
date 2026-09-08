package org.dhamma.dipi.staff

import android.graphics.Bitmap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.model.FaceEvidence
import org.dhamma.dipi.staff.model.PhotoBox
import org.dhamma.dipi.staff.photos.PhotoFaceDetect
import org.dhamma.dipi.staff.photos.PhotoScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PhotoScannerTest {
    @Test
    fun retriesAt1024When640FindsNoFace() = runBlocking {
        val sizes = ArrayList<Pair<Int, Int>>()
        var calls = 0
        val detect = PhotoFaceDetect { bitmap ->
            calls++
            sizes += bitmap.width to bitmap.height
            // 5th call is 0° of the 1024 retry; 180° is also 1024x768.
            if (calls == 5 && bitmap.width == 1024 && bitmap.height == 768) {
                listOf(FaceEvidence(PhotoBox(0.3, 0.2, 0.4, 0.5)))
            } else {
                emptyList()
            }
        }
        PhotoScanner(detect).use { scanner ->
            val evidence = scanner.scan(Bitmap.createBitmap(2000, 1500, Bitmap.Config.ARGB_8888))
            assertTrue(sizes.size >= 8)
            assertTrue(sizes.take(4).all { maxOf(it.first, it.second) == 640 })
            assertTrue(sizes.drop(4).take(4).all { maxOf(it.first, it.second) == 1024 })
            val hit = evidence.single { it.faces.isNotEmpty() }
            assertEquals(0, hit.clockwise)
            assertEquals(1, hit.faces.size)
            assertEquals(listOf(90, 180, 270).map { angle ->
                evidence.single { it.clockwise == angle }.faces
            }.all { it.isEmpty() }, true)
        }
    }

    @Test
    fun cancelsBetweenOrientations() = runBlocking {
        var calls = 0
        val firstEntered = CompletableDeferred<Unit>()
        val detect = PhotoFaceDetect {
            calls++
            if (calls == 1) {
                firstEntered.complete(Unit)
                CompletableDeferred<Unit>().await()
            }
            emptyList()
        }
        PhotoScanner(detect).use { scanner ->
            val job = launch {
                scanner.scan(Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888))
            }
            firstEntered.await()
            job.cancelAndJoin()
            assertEquals(1, calls)
        }
    }

    @Test
    fun closeIsIdempotent() {
        val scanner = PhotoScanner(PhotoFaceDetect { emptyList() })
        scanner.close()
        scanner.close()
    }

    @Test
    fun defaultDetectorConstructionIsSafeOnJvm() {
        runCatching {
            PhotoScanner().close()
            PhotoScanner().close()
        }
    }
}
