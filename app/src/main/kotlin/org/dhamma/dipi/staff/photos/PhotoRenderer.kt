package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoGeometry
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoStamp
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/**
 * Applies a [PhotoRecipe] as real pixels: integer right-angle rotation, then
 * an optional half-open crop in the post-rotation source.
 *
 * Ownership:
 * - The caller owns the source [Bitmap] and the [OutputStream] passed to
 *   [writeJpeg]. This class never recycles or closes them.
 * - The renderer owns every intermediate bitmap it allocates and recycles
 *   those copies after the current caller finishes — including when work is
 *   cancelled — and never while another coroutine still holds them.
 * - The [Bitmap] returned by [render] is transferred to the caller, who must
 *   recycle it.
 *
 * Preview and JPEG export share [renderUnlocked]. JPEG quality is 92, with
 * alpha flattened onto white before encode. Expensive work runs on
 * [Dispatchers.Default] under one process-wide correction-work [Semaphore].
 */
class PhotoRenderer {
    /**
     * Returns a new corrected bitmap. The caller owns the result. The source
     * is read only; its pixels are copied before any transform.
     */
    suspend fun render(source: Bitmap, recipe: PhotoRecipe): Bitmap =
        withCorrectionWork {
            val rendered = renderUnlocked(source, recipe)
            try {
                coroutineContext.ensureActive()
                rendered
            } catch (cancelled: CancellationException) {
                if (!rendered.isRecycled) rendered.recycle()
                throw cancelled
            }
        }

    /**
     * Encodes the same pixels [render] would produce as a quality-92 JPEG.
     * Transparent pixels are composited onto white before compression.
     */
    suspend fun writeJpeg(
        source: Bitmap,
        recipe: PhotoRecipe,
        destination: OutputStream,
    ) {
        withCorrectionWork {
            val rendered = renderUnlocked(source, recipe)
            var flat: Bitmap? = null
            try {
                coroutineContext.ensureActive()
                flat = flattenOntoWhite(rendered)
                coroutineContext.ensureActive()
                check(flat.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, destination)) {
                    "jpeg compress failed"
                }
            } finally {
                if (!rendered.isRecycled) rendered.recycle()
                val owned = flat
                if (owned != null && !owned.isRecycled) owned.recycle()
            }
        }
    }

    private suspend fun renderUnlocked(source: Bitmap, recipe: PhotoRecipe): Bitmap {
        require(!source.isRecycled) { "source recycled" }
        val stamp = PhotoStamp(UNUSED_HASH, source.width, source.height)
        val canonical = PhotoGeometry.validate(stamp, recipe)
        val srcW = source.width
        val srcH = source.height
        val src = IntArray(srcW * srcH)
        source.getPixels(src, 0, srcW, 0, 0, srcW, srcH)
        coroutineContext.ensureActive()
        val rotated = rotatePixels(src, srcW, srcH, canonical.clockwise)
        val cropped = canonical.crop?.let { cropPixels(rotated, it) } ?: rotated
        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        out.setPixels(cropped.pixels, 0, cropped.width, 0, 0, cropped.width, cropped.height)
        return out
    }

    private suspend fun rotatePixels(
        src: IntArray,
        width: Int,
        height: Int,
        clockwise: Int,
    ): PixelBuffer {
        if (clockwise == 0) return PixelBuffer(src, width, height)
        val (rw, rh) = PhotoGeometry.rotatedSize(width, height, clockwise)
        val dst = IntArray(rw * rh)
        for (y in 0 until height) {
            if (y and 31 == 0) coroutineContext.ensureActive()
            val row = y * width
            for (x in 0 until width) {
                val (nx, ny) = mapPoint(x, y, width, height, clockwise)
                dst[ny * rw + nx] = src[row + x]
            }
        }
        return PixelBuffer(dst, rw, rh)
    }

    private fun cropPixels(buf: PixelBuffer, crop: PhotoCrop): PixelBuffer {
        val out = IntArray(crop.width * crop.height)
        for (row in 0 until crop.height) {
            val srcOff = (crop.y + row) * buf.width + crop.x
            System.arraycopy(buf.pixels, srcOff, out, row * crop.width, crop.width)
        }
        return PixelBuffer(out, crop.width, crop.height)
    }

    private fun flattenOntoWhite(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = p ushr 24
            if (a == 255) continue
            if (a == 0) {
                pixels[i] = Color.WHITE
                continue
            }
            val inv = 255 - a
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val nr = (r * a + 255 * inv) / 255
            val ng = (g * a + 255 * inv) / 255
            val nb = (b * a + 255 * inv) / 255
            pixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }
        val flat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        flat.setPixels(pixels, 0, w, 0, 0, w, h)
        flat.setHasAlpha(false)
        return flat
    }

    private suspend fun <T> withCorrectionWork(block: suspend () -> T): T =
        withContext(Dispatchers.Default) {
            correctionWork.withPermit {
                coroutineContext.ensureActive()
                yield()
                block()
            }
        }

    private class PixelBuffer(val pixels: IntArray, val width: Int, val height: Int)

    companion object {
        const val JPEG_QUALITY = 92
        private val correctionWork = Semaphore(1)
        private val UNUSED_HASH = "0".repeat(64)

        private fun mapPoint(x: Int, y: Int, width: Int, height: Int, clockwise: Int): Pair<Int, Int> =
            when (clockwise) {
                0 -> x to y
                90 -> (height - 1 - y) to x
                180 -> (width - 1 - x) to (height - 1 - y)
                270 -> y to (width - 1 - x)
                else -> throw IllegalArgumentException("invalid rotation")
            }
    }
}
