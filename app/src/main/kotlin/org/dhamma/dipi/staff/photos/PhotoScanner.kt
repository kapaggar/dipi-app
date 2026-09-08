package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import org.dhamma.dipi.staff.model.FaceEvidence
import org.dhamma.dipi.staff.model.PhotoBox
import org.dhamma.dipi.staff.model.PhotoOrientationEvidence
import org.dhamma.dipi.staff.model.PhotoPoint
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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

internal class MlKitPhotoFaceDetect : PhotoFaceDetect, AutoCloseable {
    private val detector: FaceDetector? = runCatching {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build(),
        )
    }.getOrNull()

    @Volatile
    private var closed = false

    override suspend fun detect(bitmap: Bitmap): List<FaceEvidence> {
        val client = detector ?: return emptyList()
        if (closed) return emptyList()
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            client.process(image).awaitSafe().map { face ->
                face.toEvidence(bitmap.width, bitmap.height)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runCatching { detector?.close() }
    }
}

private fun Face.toEvidence(width: Int, height: Int): FaceEvidence {
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)
    val bounds = boundingBox
    return FaceEvidence(
        box = PhotoBox(
            x = bounds.left.toDouble() / w,
            y = bounds.top.toDouble() / h,
            width = bounds.width().toDouble() / w,
            height = bounds.height().toDouble() / h,
        ),
        leftEye = landmarkPoint(FaceLandmark.LEFT_EYE, w, h),
        rightEye = landmarkPoint(FaceLandmark.RIGHT_EYE, w, h),
        nose = landmarkPoint(FaceLandmark.NOSE_BASE, w, h),
        mouth = landmarkPoint(FaceLandmark.MOUTH_BOTTOM, w, h),
    )
}

private fun Face.landmarkPoint(type: Int, width: Int, height: Int): PhotoPoint? {
    val point = getLandmark(type)?.position ?: return null
    return PhotoPoint(point.x.toDouble() / width, point.y.toDouble() / height)
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

private suspend fun <T> Task<T>.awaitSafe(): T =
    suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (!cont.isActive) return@addOnCompleteListener
            val error = task.exception
            if (error != null) {
                cont.resumeWithException(error)
            } else {
                @Suppress("UNCHECKED_CAST")
                cont.resume(task.result as T)
            }
        }
    }
