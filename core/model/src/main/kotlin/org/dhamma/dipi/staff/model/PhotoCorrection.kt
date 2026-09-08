package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable
import java.net.URI
import java.security.MessageDigest

/** Scheme + host + non-default port + optional deployment base path. Never a centre ID alone. */
object PhotoOrigins {
    fun canonicalize(raw: String): String {
        val uri = URI(raw.trim())
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) { "origin scheme" }
        require(!uri.host.isNullOrBlank()) { "origin host" }
        require(uri.rawUserInfo == null) { "origin userinfo" }
        require(uri.rawQuery == null) { "origin query" }
        require(uri.rawFragment == null) { "origin fragment" }
        val scheme = uri.scheme.lowercase()
        val host = uri.host.lowercase()
        val port = when {
            uri.port < 0 -> ""
            scheme == "https" && uri.port == 443 -> ""
            scheme == "http" && uri.port == 80 -> ""
            else -> ":${uri.port}"
        }
        val path = uri.rawPath.orEmpty().trimEnd('/').let { if (it.isEmpty() || it == "/") "" else it }
        return "$scheme://$host$port$path"
    }
}

object PhotoStamps {
    private val SHA256_HEX = Regex("^[0-9a-f]{64}$")

    fun isSha256Hex(value: String): Boolean = SHA256_HEX.matches(value)

    fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun ofEncoded(bytes: ByteArray, width: Int, height: Int): PhotoStamp =
        PhotoStamp(sha256Hex(bytes), width, height)
}

@Serializable
data class PhotoScope(
    val origin: String,
    val centreId: Int,
    val courseId: Int,
) {
    init {
        require(centreId > 0 && courseId > 0) { "scope ids must be positive" }
        require(origin == PhotoOrigins.canonicalize(origin)) { "origin must be canonical" }
    }
}

@Serializable
data class PhotoKey(val scope: PhotoScope, val applicantId: Int) {
    init { require(applicantId > 0) { "applicantId must be positive" } }
}

@Serializable
data class PhotoStamp(
    val sha256: String,
    val width: Int,
    val height: Int,
) {
    init {
        require(PhotoStamps.isSha256Hex(sha256)) { "sha256 must be 64 lowercase hex" }
        require(width > 0 && height > 0) { "stamp dimensions must be positive" }
    }
}

@Serializable
data class PhotoCrop(val x: Int, val y: Int, val width: Int, val height: Int)

@Serializable
data class PhotoRecipe(val clockwise: Int = 0, val crop: PhotoCrop? = null)

@Serializable
enum class PhotoReviewState { UNREVIEWED, DRAFT, APPROVED, SOURCE_CHANGED }

@Serializable
enum class PhotoWriteState {
    NOT_STARTED, PREPARING, SUBMITTING, VERIFYING,
    COMMITTED, FAILED, UNKNOWN, SUPERSEDED,
}

@Serializable
data class PhotoDraft(
    val key: PhotoKey,
    val source: PhotoStamp,
    val recipe: PhotoRecipe = PhotoRecipe(),
    val review: PhotoReviewState = PhotoReviewState.UNREVIEWED,
    val editRevision: Long = 0,
    val write: PhotoWriteState = PhotoWriteState.NOT_STARTED,
    val operationId: String? = null,
    val output: PhotoStamp? = null,
)

object PhotoGeometry {
    private val ANGLES = setOf(0, 90, 180, 270)

    fun rotatedSize(width: Int, height: Int, clockwise: Int): Pair<Int, Int> {
        require(width > 0 && height > 0) { "dimensions must be positive" }
        require(clockwise in ANGLES) { "invalid rotation" }
        return if (clockwise == 90 || clockwise == 270) height to width else width to height
    }

    fun isIdentity(recipe: PhotoRecipe): Boolean =
        recipe.clockwise == 0 && recipe.crop == null

    fun validate(stamp: PhotoStamp, recipe: PhotoRecipe): PhotoRecipe {
        require(recipe.clockwise in ANGLES) { "invalid rotation" }
        val (rw, rh) = rotatedSize(stamp.width, stamp.height, recipe.clockwise)
        val crop = recipe.crop ?: return PhotoRecipe(recipe.clockwise, null)
        requireInBounds(crop, rw, rh)
        val fullFrame = crop.x == 0 && crop.y == 0 && crop.width == rw && crop.height == rh
        return PhotoRecipe(recipe.clockwise, if (fullFrame) null else crop)
    }

    fun rotateCrop(crop: PhotoCrop, oldWidth: Int, oldHeight: Int, delta: Int): PhotoCrop {
        require(oldWidth > 0 && oldHeight > 0) { "dimensions must be positive" }
        val turns = turns(delta)
        var c = crop
        var w = oldWidth.toLong()
        var h = oldHeight.toLong()
        repeat(turns) {
            requireInBounds(c, w, h)
            val nx = h - (c.y.toLong() + c.height.toLong())
            val ny = c.x.toLong()
            val nw = c.height.toLong()
            val nh = c.width.toLong()
            c = PhotoCrop(toIntExact(nx), toIntExact(ny), toIntExact(nw), toIntExact(nh))
            val nextW = h
            h = w
            w = nextW
        }
        requireInBounds(c, w, h)
        return c
    }

    fun applyRotation(stamp: PhotoStamp, recipe: PhotoRecipe, delta: Int): PhotoRecipe {
        val current = validate(stamp, recipe)
        val clockwise = ((current.clockwise + normalizeDelta(delta)) % 360)
        val crop = current.crop?.let { existing ->
            val (rw, rh) = rotatedSize(stamp.width, stamp.height, current.clockwise)
            rotateCrop(existing, rw, rh, delta)
        }
        return validate(stamp, PhotoRecipe(clockwise, crop))
    }

    fun goldenLetters(clockwise: Int): List<Char> {
        val source = listOf('A', 'B', 'C', 'D', 'E', 'F')
        return rotateLetters(source, 2, 3, clockwise)
    }

    fun cropLetters(letters: List<Char>, width: Int, height: Int, crop: PhotoCrop): List<Char> {
        requireInBounds(crop, width.toLong(), height.toLong())
        val out = ArrayList<Char>(crop.width * crop.height)
        for (row in crop.y until crop.y + crop.height) {
            for (col in crop.x until crop.x + crop.width) {
                out += letters[row * width + col]
            }
        }
        return out
    }

    private fun rotateLetters(source: List<Char>, width: Int, height: Int, clockwise: Int): List<Char> {
        val (rw, rh) = rotatedSize(width, height, clockwise)
        val out = MutableList(rw * rh) { ' ' }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val (nx, ny) = mapPoint(x, y, width, height, clockwise)
                out[ny * rw + nx] = source[y * width + x]
            }
        }
        return out
    }

    private fun mapPoint(x: Int, y: Int, width: Int, height: Int, clockwise: Int): Pair<Int, Int> =
        when (clockwise) {
            0 -> x to y
            90 -> (height - 1 - y) to x
            180 -> (width - 1 - x) to (height - 1 - y)
            270 -> y to (width - 1 - x)
            else -> throw IllegalArgumentException("invalid rotation")
        }

    private fun requireInBounds(crop: PhotoCrop, width: Int, height: Int) =
        requireInBounds(crop, width.toLong(), height.toLong())

    private fun requireInBounds(crop: PhotoCrop, width: Long, height: Long) {
        val x = crop.x.toLong()
        val y = crop.y.toLong()
        val w = crop.width.toLong()
        val h = crop.height.toLong()
        require(w > 0 && h > 0) { "crop size must be positive" }
        require(x >= 0 && y >= 0) { "crop origin must be in-bounds" }
        require(x + w <= width && y + h <= height) { "crop outside rotated source" }
    }

    private fun turns(delta: Int): Int = normalizeDelta(delta) / 90

    private fun normalizeDelta(delta: Int): Int {
        val normalized = ((delta % 360) + 360) % 360
        require(normalized % 90 == 0) { "invalid rotation" }
        return normalized
    }

    private fun toIntExact(value: Long): Int {
        require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) { "integer overflow" }
        return value.toInt()
    }
}

object PhotoDrafts {
    fun applyRecipe(draft: PhotoDraft, stamp: PhotoStamp, recipe: PhotoRecipe): PhotoDraft {
        require(draft.source == stamp) { "source mismatch" }
        val canonical = PhotoGeometry.validate(stamp, recipe)
        return draft.copy(
            recipe = canonical,
            review = PhotoReviewState.DRAFT,
            editRevision = draft.editRevision + 1,
            write = PhotoWriteState.NOT_STARTED,
        )
    }

    fun approve(draft: PhotoDraft, stamp: PhotoStamp): PhotoDraft {
        require(draft.source == stamp) { "source mismatch" }
        PhotoGeometry.validate(stamp, draft.recipe)
        return draft.copy(review = PhotoReviewState.APPROVED)
    }

    fun saveDraft(draft: PhotoDraft, stamp: PhotoStamp, recipe: PhotoRecipe): PhotoDraft =
        applyRecipe(draft, stamp, recipe)

    fun bindSource(draft: PhotoDraft, stamp: PhotoStamp): PhotoDraft {
        if (draft.source == stamp) return draft
        return draft.copy(
            source = stamp,
            recipe = PhotoRecipe(),
            review = PhotoReviewState.SOURCE_CHANGED,
            editRevision = draft.editRevision + 1,
            output = null,
            write = PhotoWriteState.NOT_STARTED,
        )
    }

    fun isReady(draft: PhotoDraft, stamp: PhotoStamp): Boolean =
        draft.source == stamp &&
            draft.review == PhotoReviewState.APPROVED &&
            !PhotoGeometry.isIdentity(draft.recipe) &&
            !hasUnresolvedWrite(draft.write)

    fun readyCount(drafts: Iterable<PhotoDraft>, stamps: Map<PhotoKey, PhotoStamp>): Int =
        drafts.count { draft -> stamps[draft.key]?.let { isReady(draft, it) } == true }

    fun restoreWrite(write: PhotoWriteState): PhotoWriteState =
        when (write) {
            PhotoWriteState.SUBMITTING, PhotoWriteState.VERIFYING -> PhotoWriteState.UNKNOWN
            else -> write
        }

    fun restore(draft: PhotoDraft): PhotoDraft = draft.copy(write = restoreWrite(draft.write))

    private fun hasUnresolvedWrite(write: PhotoWriteState): Boolean =
        write == PhotoWriteState.PREPARING ||
            write == PhotoWriteState.SUBMITTING ||
            write == PhotoWriteState.VERIFYING ||
            write == PhotoWriteState.UNKNOWN
}
