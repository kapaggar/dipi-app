package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import org.dhamma.dipi.staff.model.FaceEvidence
import kotlin.coroutines.cancellation.CancellationException
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import org.dhamma.dipi.staff.model.PhotoBox
import org.dhamma.dipi.staff.model.PhotoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
