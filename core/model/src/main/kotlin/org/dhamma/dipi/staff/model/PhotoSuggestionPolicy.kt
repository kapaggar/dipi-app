package org.dhamma.dipi.staff.model

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

data class PhotoPoint(val x: Double, val y: Double)

data class PhotoBox(val x: Double, val y: Double, val width: Double, val height: Double)

data class FaceEvidence(
    val box: PhotoBox,
    val leftEye: PhotoPoint? = null,
    val rightEye: PhotoPoint? = null,
    val nose: PhotoPoint? = null,
    val mouth: PhotoPoint? = null,
)

data class PhotoOrientationEvidence(val clockwise: Int, val faces: List<FaceEvidence>)

enum class PhotoSuggestionReason {
    NONE,
    ROTATE,
    CROP,
    ROTATE_AND_CROP,
    NO_FACE,
    MULTIPLE_FACES,
    AMBIGUOUS,
}

data class PhotoSuggestion(
    val recipe: PhotoRecipe?,
    val reason: PhotoSuggestionReason,
    val safeToApply: Boolean,
)

object PhotoSuggestionPolicy {
    const val EYE_LEVEL_TOLERANCE = 0.35
    const val TINY_FACE_AREA = 0.20
    const val HEAD_CHIN_MARGIN = 0.60
    const val CROP_RATIO_WIDTH = 13
    const val CROP_RATIO_HEIGHT = 14

    private val ANGLES = setOf(0, 90, 180, 270)
    private const val CROP_RATIO = CROP_RATIO_WIDTH.toDouble() / CROP_RATIO_HEIGHT

    fun suggest(evidence: List<PhotoOrientationEvidence>, stamp: PhotoStamp): PhotoSuggestion {
        val byAngle = evidence.filter { it.clockwise in ANGLES }
        val withFaces = byAngle.filter { it.faces.isNotEmpty() }
        if (withFaces.isEmpty()) {
            return PhotoSuggestion(null, PhotoSuggestionReason.NO_FACE, safeToApply = false)
        }

        val singles = withFaces.filter { it.faces.size == 1 }
        if (singles.isEmpty()) {
            return PhotoSuggestion(null, PhotoSuggestionReason.MULTIPLE_FACES, safeToApply = false)
        }

        val uprights = singles.filter { isUpright(it.faces.single()) }
        if (uprights.isEmpty()) {
            return PhotoSuggestion(null, PhotoSuggestionReason.AMBIGUOUS, safeToApply = false)
        }

        val chosen = uprights.find { it.clockwise == 0 }
            ?: uprights.singleOrNull()
            ?: return PhotoSuggestion(null, PhotoSuggestionReason.AMBIGUOUS, safeToApply = false)

        return suggestionFor(chosen, stamp)
    }

    internal fun isUpright(face: FaceEvidence): Boolean {
        val left = face.leftEye ?: return false
        val right = face.rightEye ?: return false
        if (face.nose == null && face.mouth == null) return false
        if (!landmarksInside(face, left, right)) return false

        val spacing = hypot(right.x - left.x, right.y - left.y)
        if (spacing < 1e-6) return false
        if (abs(right.y - left.y) / spacing >= EYE_LEVEL_TOLERANCE) return false

        val midY = (left.y + right.y) / 2.0
        val nose = face.nose
        val mouth = face.mouth
        if (nose != null && midY >= nose.y) return false
        if (mouth != null && midY >= mouth.y) return false
        return true
    }

    private fun landmarksInside(face: FaceEvidence, left: PhotoPoint, right: PhotoPoint): Boolean {
        if (!face.box.containsPlausibly(left) || !face.box.containsPlausibly(right)) return false
        val nose = face.nose
        val mouth = face.mouth
        if (nose != null && !face.box.containsPlausibly(nose)) return false
        if (mouth != null && !face.box.containsPlausibly(mouth)) return false
        return true
    }

    private fun suggestionFor(chosen: PhotoOrientationEvidence, stamp: PhotoStamp): PhotoSuggestion {
        val face = chosen.faces.single()
        val area = face.box.width * face.box.height
        val needsCrop = area < TINY_FACE_AREA
        if (!needsCrop) {
            val recipe = PhotoGeometry.validate(stamp, PhotoRecipe(chosen.clockwise, null))
            return if (chosen.clockwise == 0) {
                PhotoSuggestion(recipe, PhotoSuggestionReason.NONE, safeToApply = false)
            } else {
                PhotoSuggestion(recipe, PhotoSuggestionReason.ROTATE, safeToApply = true)
            }
        }

        val (crop, marginsFit) = portraitCrop(face.box, stamp, chosen.clockwise)
        val recipe = PhotoGeometry.validate(stamp, PhotoRecipe(chosen.clockwise, crop))
        val reason = when {
            chosen.clockwise == 0 && recipe.crop != null -> PhotoSuggestionReason.CROP
            chosen.clockwise != 0 && recipe.crop != null -> PhotoSuggestionReason.ROTATE_AND_CROP
            chosen.clockwise != 0 -> PhotoSuggestionReason.ROTATE
            else -> PhotoSuggestionReason.NONE
        }
        val safe = marginsFit && recipe.crop != null && !PhotoGeometry.isIdentity(recipe)
        return PhotoSuggestion(recipe, reason, safe)
    }

    private fun portraitCrop(
        box: PhotoBox,
        stamp: PhotoStamp,
        clockwise: Int,
    ): Pair<PhotoCrop, Boolean> {
        val (rw, rh) = PhotoGeometry.rotatedSize(stamp.width, stamp.height, clockwise)
        val faceX = box.x * rw
        val faceY = box.y * rh
        val faceW = box.width * rw
        val faceH = box.height * rh
        val head = faceH * HEAD_CHIN_MARGIN
        val desiredH = faceH + 2.0 * head
        val desiredW = desiredH * CROP_RATIO
        val desiredX = faceX + faceW / 2.0 - desiredW / 2.0
        val desiredY = faceY - head
        val marginsFit = desiredW <= rw + 1e-9 &&
            desiredH <= rh + 1e-9 &&
            desiredX >= -1e-9 &&
            desiredY >= -1e-9 &&
            desiredX + desiredW <= rw + 1e-9 &&
            desiredY + desiredH <= rh + 1e-9

        var h = desiredH
        var w = desiredW
        if (h > rh) {
            w *= rh / h
            h = rh.toDouble()
        }
        if (w > rw) {
            h *= rw / w
            w = rw.toDouble()
        }
        if (h > rh) {
            w *= rh / h
            h = rh.toDouble()
        }
        val maxX = (rw - w).coerceAtLeast(0.0)
        val maxY = (rh - h).coerceAtLeast(0.0)
        val x = (faceX + faceW / 2.0 - w / 2.0).coerceIn(0.0, maxX)
        val y = (faceY - head).coerceIn(0.0, maxY)
        return snapCrop(x, y, w, h, rw, rh) to marginsFit
    }

    private fun snapCrop(x: Double, y: Double, w: Double, h: Double, rw: Int, rh: Int): PhotoCrop {
        var ih = h.roundToInt().coerceIn(1, rh)
        var iw = (ih * CROP_RATIO).roundToInt().coerceIn(1, rw)
        var ix = x.roundToInt()
        var iy = y.roundToInt()
        if (ix < 0) ix = 0
        if (iy < 0) iy = 0
        if (ix + iw > rw) {
            ix = (rw - iw).coerceAtLeast(0)
            if (ix + iw > rw) iw = (rw - ix).coerceAtLeast(1)
        }
        if (iy + ih > rh) {
            iy = (rh - ih).coerceAtLeast(0)
            if (iy + ih > rh) ih = (rh - iy).coerceAtLeast(1)
        }
        return PhotoCrop(ix, iy, iw, ih)
    }
}

private fun PhotoBox.containsPlausibly(point: PhotoPoint): Boolean {
    val padX = width * 0.15
    val padY = height * 0.15
    return point.x >= x - padX &&
        point.x <= x + width + padX &&
        point.y >= y - padY &&
        point.y <= y + height + padY
}
