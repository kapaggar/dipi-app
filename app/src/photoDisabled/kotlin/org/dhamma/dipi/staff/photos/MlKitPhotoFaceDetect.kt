package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import org.dhamma.dipi.staff.model.FaceEvidence

/** Compact builds contain no detector, model, or model download path. */
internal class MlKitPhotoFaceDetect : PhotoFaceDetect {
    override suspend fun detect(bitmap: Bitmap): List<FaceEvidence> = emptyList()
}
