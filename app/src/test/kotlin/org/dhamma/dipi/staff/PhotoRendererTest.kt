package org.dhamma.dipi.staff

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoGeometry
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.photos.PhotoRenderer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class PhotoRendererTest {
    private val renderer = PhotoRenderer()

    @Test
    fun ninetyAndCropMatchesGoldenBlock() = runBlocking {
        val source = colored2x3()
        val out = renderer.render(source, PhotoRecipe(90, PhotoCrop(1, 0, 2, 2)))
        assertEquals(listOf('C', 'A', 'D', 'B'), lettersOf(out))
        out.recycle()
    }

    @Test
    fun transparentInputBecomesWhiteBackedJpeg() = runBlocking {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.TRANSPARENT)
        val bytes = ByteArrayOutputStream()
        renderer.writeJpeg(source, PhotoRecipe(), bytes)
        val encoded = bytes.toByteArray()
        assertTrue(encoded.isNotEmpty())
        val decoded = BitmapFactory.decodeByteArray(encoded, 0, encoded.size)
        assertTrue(decoded != null && decoded.width == 2 && decoded.height == 2)
        assertOpaqueWhite(decoded.getPixel(0, 0))
        assertOpaqueWhite(decoded.getPixel(1, 1))
        decoded.recycle()
        source.recycle()
    }

    @Test
    fun identityRenderingPreservesDimensions() = runBlocking {
        val source = colored2x3()
        val out = renderer.render(source, PhotoRecipe())
        assertEquals(2, out.width)
        assertEquals(3, out.height)
        assertEquals(PhotoGeometry.goldenLetters(0), lettersOf(out))
        assertNotSame(source, out)
        out.recycle()
    }

    @Test
    fun neverUpscalesIdentityOrCrop() = runBlocking {
        val source = colored2x3()
        val identity = renderer.render(source, PhotoRecipe())
        assertNoUpscale(source, identity, 0)
        identity.recycle()

        val cropped = renderer.render(source, PhotoRecipe(0, PhotoCrop(0, 1, 2, 2)))
        assertNoUpscale(source, cropped, 0)
        assertTrue(cropped.width <= 2 && cropped.height <= 3)
        cropped.recycle()

        val rotated = renderer.render(source, PhotoRecipe(90))
        assertNoUpscale(source, rotated, 90)
        rotated.recycle()

        val rotatedCrop = renderer.render(source, PhotoRecipe(90, PhotoCrop(1, 0, 2, 2)))
        assertNoUpscale(source, rotatedCrop, 90)
        rotatedCrop.recycle()
    }

    @Test
    fun sourcePixelsUnchangedAfterRender() = runBlocking {
        val source = colored2x3()
        val before = IntArray(6)
        source.getPixels(before, 0, 2, 0, 0, 2, 3)
        val out = renderer.render(source, PhotoRecipe(180, PhotoCrop(0, 0, 2, 2)))
        val after = IntArray(6)
        source.getPixels(after, 0, 2, 0, 0, 2, 3)
        assertArrayEquals(before, after)
        assertNotSame(source, out)
        out.recycle()
    }

    @Test
    fun allFourGoldenRotationsMatchLetterVectors() = runBlocking {
        val source = colored2x3()
        for (angle in listOf(0, 90, 180, 270)) {
            val out = renderer.render(source, PhotoRecipe(angle))
            assertEquals(PhotoGeometry.goldenLetters(angle), lettersOf(out))
            out.recycle()
        }
    }

    @Test
    fun cancelDuringRenderPropagatesAndClaimsNoBitmap() = runBlocking(Dispatchers.Default) {
        val source = colored2x3()
        var claimed: Bitmap? = null
        val deferred = async {
            claimed = renderer.render(source, PhotoRecipe(90))
            claimed!!
        }
        yield()
        deferred.cancel()
        try {
            deferred.await()
            fail("render claimed success after cancel")
        } catch (e: CancellationException) {
            assertNull(claimed)
        }
    }

    private fun colored2x3(): Bitmap {
        val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
        var i = 0
        for (y in 0 until 3) {
            for (x in 0 until 2) {
                bitmap.setPixel(x, y, LETTER_COLORS[i++].second)
            }
        }
        return bitmap
    }

    private fun lettersOf(bitmap: Bitmap): List<Char> {
        val out = ArrayList<Char>(bitmap.width * bitmap.height)
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                out += colorToLetter(bitmap.getPixel(x, y))
            }
        }
        return out
    }

    private fun colorToLetter(pixel: Int): Char =
        LETTER_COLORS.first { it.second == pixel }.first

    private fun assertNoUpscale(source: Bitmap, output: Bitmap, clockwise: Int) {
        val (rw, rh) = PhotoGeometry.rotatedSize(source.width, source.height, clockwise)
        assertTrue(output.width <= rw)
        assertTrue(output.height <= rh)
        assertTrue(output.width <= maxOf(source.width, rw))
        assertTrue(output.height <= maxOf(source.height, rh))
    }

    private fun assertOpaqueWhite(pixel: Int) {
        assertEquals(255, Color.alpha(pixel))
        assertEquals(255, Color.red(pixel))
        assertEquals(255, Color.green(pixel))
        assertEquals(255, Color.blue(pixel))
    }

    companion object {
        private val LETTER_COLORS = listOf(
            'A' to 0xFFCC0000.toInt(),
            'B' to 0xFF00AA00.toInt(),
            'C' to 0xFF0033CC.toInt(),
            'D' to 0xFFE6B800.toInt(),
            'E' to 0xFF9900CC.toInt(),
            'F' to 0xFF008888.toInt(),
        )
    }
}
