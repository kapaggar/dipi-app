package org.dhamma.dipi.staff.network

import android.graphics.Bitmap
import org.dhamma.dipi.staff.model.PhotoStamp

/** Decoded correction source. Not a data class — [toString] must not dump pixels. */
class PhotoSource(val stamp: PhotoStamp, val bitmap: Bitmap) {
    override fun toString(): String = "PhotoSource(${stamp.width}x${stamp.height})"
}

sealed class PhotoSourceResult {
    data class Ready(val source: PhotoSource) : PhotoSourceResult()
    data object Missing : PhotoSourceResult()
    data object AuthenticationRequired : PhotoSourceResult()
    data object UnsupportedImage : PhotoSourceResult()
    data object TooLarge : PhotoSourceResult()
    data object Unavailable : PhotoSourceResult()
}
