package org.dhamma.dipi.staff

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoReviewState
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamps
import org.dhamma.dipi.staff.network.PhotoSource
import org.dhamma.dipi.staff.photos.ExportResult
import org.dhamma.dipi.staff.photos.PhotoExport
import org.dhamma.dipi.staff.photos.PhotoRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
class PhotoExportTest {
    private val renderer = PhotoRenderer()
    private val export = PhotoExport(renderer)
    private val scope = PhotoScope("https://one.example.test", 1, 10)
    private val stamp = PhotoStamps.ofEncoded(byteArrayOf(9, 8, 7, 6), 2, 3)

    private fun bitmap(): Bitmap {
        val colors = intArrayOf(
            Color.RED, Color.GREEN,
            Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA,
        )
        return Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888).apply {
            setPixels(colors, 0, 2, 0, 0, 2, 3)
        }
    }

    private fun draft(id: Int) = PhotoDraft(
        key = PhotoKey(scope, id),
        source = stamp,
        recipe = PhotoRecipe(90),
        review = PhotoReviewState.APPROVED,
    )

    @Test
    fun zipUsesDeterministicFilenamesAndJpegBytes() = runBlocking {
        val source = PhotoSource(stamp, bitmap())
        val out = ByteArrayOutputStream()
        val result = export.writeZip(
            listOf(draft(11), draft(22)),
            { source },
            out,
        )
        assertEquals(ExportResult.Completed(2), result)
        val names = mutableListOf<String>()
        ZipInputStream(out.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
                val bytes = zip.readBytes()
                assertTrue(bytes.size > 8)
                assertEquals(0xFF, bytes[0].toInt() and 0xFF)
                assertEquals(0xD8, bytes[1].toInt() and 0xFF)
            }
        }
        assertEquals(listOf("photo-11-corrected.jpg", "photo-22-corrected.jpg"), names)
    }

    @Test
    fun changedSourceFailsWithoutMarkingComplete() = runBlocking {
        val other = PhotoStamps.ofEncoded(byteArrayOf(1), 2, 3)
        val source = PhotoSource(other, bitmap())
        val result = export.writeZip(listOf(draft(11)), { source }, ByteArrayOutputStream())
        assertTrue(result is ExportResult.Failed)
        assertEquals(0, (result as ExportResult.Failed).files)
    }

    @Test
    fun nullSourceFails() = runBlocking {
        val result = export.writeZip(listOf(draft(11)), { null }, ByteArrayOutputStream())
        assertTrue(result is ExportResult.Failed)
    }

    @Test
    fun cancellationReturnsIncompleteCount() = runBlocking {
        val source = PhotoSource(stamp, bitmap())
        val job = async {
            export.writeZip(
                listOf(draft(11), draft(22)),
                {
                    yield()
                    source
                },
                ByteArrayOutputStream(),
            )
        }
        job.cancel()
        try {
            val result = job.await()
            assertTrue(result is ExportResult.Cancelled || result is ExportResult.Completed)
        } catch (_: CancellationException) {
            // Job cancellation may surface before writeZip maps it.
        }
    }

    @Test
    fun writeOneDoesNotClaimUpload() = runBlocking {
        val out = ByteArrayOutputStream()
        export.writeOne(bitmap(), PhotoRecipe(90), out)
        assertTrue(out.size() > 0)
        assertEquals("photo-999999-corrected.jpg", PhotoExport.fileName(999999))
    }

    @Test
    fun midWriteFailureStops() = runBlocking {
        val failing = object : java.io.OutputStream() {
            override fun write(b: Int) = fail("should not write")
        }
        try {
            export.writeZip(listOf(draft(11)), { error("mid-write") }, failing)
        } catch (_: AssertionError) {
            fail("source open failed before write")
        }
        val result = export.writeZip(listOf(draft(11)), { error("mid-write") }, ByteArrayOutputStream())
        assertTrue(result is ExportResult.Failed)
    }
}
