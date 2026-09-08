package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoStamp
import org.dhamma.dipi.staff.network.PhotoSource
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

sealed class ExportResult {
    data class Completed(val files: Int) : ExportResult()
    data class Cancelled(val files: Int) : ExportResult()
    data class Failed(val files: Int, val message: String) : ExportResult()
}

class PhotoExport(private val renderer: PhotoRenderer = PhotoRenderer()) {
    suspend fun writeOne(source: Bitmap, recipe: PhotoRecipe, output: OutputStream) {
        renderer.writeJpeg(source, recipe, output)
    }

    suspend fun writeZip(
        entries: List<PhotoDraft>,
        openSource: suspend (PhotoKey) -> PhotoSource?,
        output: OutputStream,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): ExportResult {
        var done = 0
        return try {
            ZipOutputStream(output).use { zip ->
                for (draft in entries) {
                    coroutineContext.ensureActive()
                    val source = openSource(draft.key)
                        ?: return ExportResult.Failed(done, "Source missing")
                    if (source.stamp != draft.source) {
                        return ExportResult.Failed(done, "Source changed")
                    }
                    zip.putNextEntry(ZipEntry(fileName(draft.key.applicantId)))
                    try {
                        writeOne(source.bitmap, draft.recipe, zip)
                    } finally {
                        zip.closeEntry()
                    }
                    done += 1
                    onProgress(done, entries.size)
                }
            }
            ExportResult.Completed(done)
        } catch (_: CancellationException) {
            ExportResult.Cancelled(done)
        } catch (e: Exception) {
            ExportResult.Failed(done, e.message ?: "Export failed")
        }
    }

    companion object {
        fun fileName(applicantId: Int): String = "photo-$applicantId-corrected.jpg"

        fun verifyStamp(expected: PhotoStamp, actual: PhotoStamp) {
            check(expected == actual) { "Source changed" }
        }
    }
}
