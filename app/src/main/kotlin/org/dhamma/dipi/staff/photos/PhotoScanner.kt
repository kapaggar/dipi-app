package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import android.graphics.Canvas
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.dhamma.dipi.staff.model.FaceEvidence
import org.dhamma.dipi.staff.model.PhotoOrientationEvidence
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

fun interface PhotoFaceDetect {
    suspend fun detect(bitmap: Bitmap): List<FaceEvidence>
}

class PhotoScanner(
    private val detect: PhotoFaceDetect = MlKitPhotoFaceDetect(),
) : AutoCloseable {
    suspend fun scan(source: Bitmap): List<PhotoOrientationEvidence> {
        val longest = max(source.width, source.height)
        val firstEdge = minOf(SCAN_EDGE, longest)
        val first = scanAt(source, firstEdge)
        if (first.any { it.faces.isNotEmpty() }) return first
        val retryEdge = minOf(RESCAN_EDGE, longest)
        if (retryEdge > firstEdge) {
            currentCoroutineContext().ensureActive()
            return scanAt(source, retryEdge)
        }
        return first
    }

    override fun close() {
        (detect as? AutoCloseable)?.close()
    }

    private suspend fun scanAt(source: Bitmap, maxEdge: Int): List<PhotoOrientationEvidence> {
        val working = scaleToLongestEdge(source, maxEdge)
        try {
            val results = ArrayList<PhotoOrientationEvidence>(ANGLES.size)
            for (clockwise in ANGLES) {
                currentCoroutineContext().ensureActive()
                val oriented = rotateClockwise(working, clockwise)
                try {
                    val faces = try {
                        detect.detect(oriented)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        emptyList()
                    }
                    results += PhotoOrientationEvidence(clockwise, faces)
                } finally {
                    if (oriented !== working) oriented.recycle()
                }
            }
            return results
        } finally {
            if (working !== source) working.recycle()
        }
    }

    private companion object {
        const val SCAN_EDGE = 640
        const val RESCAN_EDGE = 1024
        val ANGLES = listOf(0, 90, 180, 270)
    }
}

private fun scaleToLongestEdge(source: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(source.width, source.height)
    if (longest <= maxEdge) return source
    val width = (source.width.toLong() * maxEdge / longest).toInt().coerceAtLeast(1)
    val height = (source.height.toLong() * maxEdge / longest).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, width, height, true)
}

private fun rotateClockwise(source: Bitmap, clockwise: Int): Bitmap {
    if (clockwise == 0) return source
    val (rw, rh) = if (clockwise == 90 || clockwise == 270) {
        source.height to source.width
    } else {
        source.width to source.height
    }
    val out = Bitmap.createBitmap(rw, rh, source.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    when (clockwise) {
        90 -> {
            canvas.translate(rw.toFloat(), 0f)
            canvas.rotate(90f)
        }
        180 -> {
            canvas.translate(rw.toFloat(), rh.toFloat())
            canvas.rotate(180f)
        }
        270 -> {
            canvas.translate(0f, rh.toFloat())
            canvas.rotate(270f)
        }
    }
    canvas.drawBitmap(source, 0f, 0f, null)
    return out
}
