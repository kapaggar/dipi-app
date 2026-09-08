package org.dhamma.dipi.staff.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoOrigins
import org.dhamma.dipi.staff.model.PhotoStamp
import org.dhamma.dipi.staff.model.PhotoStamps
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fetch-once loader with a concurrency gate and a byte-bounded, memory-only
 * LRU. Pure Kotlin so the limiter and eviction policy are unit-testable
 * without Android or network.
 *
 * - at most [maxConcurrent] fetches run at once; the rest queue on the gate,
 * - concurrent requests for the same key share one in-flight fetch,
 * - successful values enter the LRU; the total is kept under [maxBytes] by
 *   evicting least-recently-used entries,
 * - a failed fetch resolves to null and is NOT cached, so a later request
 *   retries,
 * - [clear] bumps a generation, drops the LRU, and cancels in-flight work so
 *   a completed fetch cannot repopulate a previous session's cache.
 *
 * Fetches run on [scope], not the caller's coroutine: a scrolled-away grid
 * cell cancelling its composition does not abort a download another cell
 * (or a later scroll-back) will want.
 */
class BoundedLoader<K : Any, V : Any>(
    maxConcurrent: Int,
    private val maxBytes: Long,
    private val scope: CoroutineScope,
    private val sizeOf: (V) -> Long,
    private val cacheable: (V) -> Boolean = { true },
    private val fetch: suspend (K) -> V?,
) {
    private val gate = Semaphore(maxConcurrent)
    private val lock = Mutex()
    private val lru = LinkedHashMap<K, V>(16, 0.75f, true)
    private val inFlight = HashMap<K, Deferred<V?>>()
    private var bytes = 0L
    private var generation = 0L

    suspend fun get(key: K): V? {
        val pending = lock.withLock {
            lru[key]?.let { return it }
            inFlight.getOrPut(key) { startFetch(key) }
        }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            coroutineContext.ensureActive()
            null
        }
    }

    suspend fun invalidate(key: K) {
        lock.withLock { dropLocked(key) }
    }

    suspend fun invalidateMatching(match: (K) -> Boolean) {
        lock.withLock {
            lru.keys.filter(match).forEach { dropLocked(it) }
        }
    }

    suspend fun clear() {
        val jobs = lock.withLock {
            generation += 1
            lru.clear()
            bytes = 0L
            val snapshot = inFlight.values.toList()
            inFlight.clear()
            snapshot
        }
        jobs.forEach { it.cancel() }
    }

    suspend fun cachedBytes(): Long = lock.withLock { bytes }

    private fun startFetch(key: K): Deferred<V?> {
        val captured = generation
        return scope.async {
            val value = try {
                gate.withPermit { fetch(key) }
            } catch (e: CancellationException) {
                lock.withLock { inFlight.remove(key) }
                throw e
            } catch (_: Exception) {
                null
            }
            lock.withLock {
                inFlight.remove(key)
                if (generation != captured) return@async null
                if (value != null && cacheable(value)) putLocked(key, value)
                value
            }
        }
    }

    /** Caller holds [lock]. */
    private fun dropLocked(key: K) {
        lru.remove(key)?.let { bytes -= sizeOf(it) }
    }

    /** Caller holds [lock]. Inserts as most-recent, then evicts oldest-first. */
    private fun putLocked(key: K, value: V) {
        val size = sizeOf(value)
        if (size > maxBytes) return
        lru.remove(key)?.let { bytes -= sizeOf(it) }
        lru[key] = value
        bytes += size
        val iter = lru.entries.iterator()
        while (bytes > maxBytes && iter.hasNext()) {
            val entry = iter.next()
            if (entry.key == key) break
            bytes -= sizeOf(entry.value)
            iter.remove()
        }
    }
}

/**
 * Live applicant photos: `GET {BASE_URL}/show-photo/{applicantId}` — the
 * `dh_manageapp` menu callback `show_application_photo` (access manageapp),
 * authenticated by the Drupal session cookie already in the shared client.
 *
 * Applicant photos are sensitive. They live only in the in-memory LRU —
 * never on disk, never in logs. Any failure (403 dead session, 404, an HTML
 * error page) resolves to null so the caller's initials placeholder stays.
 *
 * [load] is the thumbnail path (6-wide, 32 MiB, long edge 1024).
 * [loadSource] is the single full-resolution correction slot.
 */
@Singleton
class PhotoLoader @Inject constructor(
    private val client: OkHttpClient,
    @Named("useMock") useMock: Boolean,
    @Named("baseUrl") baseUrl: String,
    server: MockWebServer,
) {
    // Lazy: MockWebServer.port must not be touched unless the mock is running.
    private val root: String by lazy {
        when {
            useMock -> "http://127.0.0.1:${server.port}/"
            baseUrl.endsWith("/") -> baseUrl
            else -> "$baseUrl/"
        }
    }

    private val configuredOrigin: String by lazy { PhotoOrigins.canonicalize(root) }
    private val configuredBasePath: String by lazy {
        URI(configuredOrigin).rawPath.orEmpty()
    }

    private val photoClient: OkHttpClient by lazy {
        client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val loader = BoundedLoader<Int, Bitmap>(
        maxConcurrent = MAX_CONCURRENT,
        maxBytes = MAX_CACHE_BYTES,
        scope = scope,
        sizeOf = { it.allocationByteCount.toLong() },
        fetch = { id -> fetchThumbnail(id) },
    )

    private val sourceLoader = BoundedLoader<PhotoKey, PhotoSourceResult>(
        maxConcurrent = 1,
        maxBytes = 1L,
        scope = scope,
        sizeOf = { if (it is PhotoSourceResult.Ready) 1L else 0L },
        cacheable = { it is PhotoSourceResult.Ready },
        fetch = { key -> fetchSource(key) },
    )

    /** Null on any failure — the initials placeholder stays. */
    suspend fun load(applicantId: Int): Bitmap? = loader.get(applicantId)

    suspend fun loadSource(key: PhotoKey, forceRefresh: Boolean = false): PhotoSourceResult {
        if (key.scope.origin != configuredOrigin) return PhotoSourceResult.Unavailable
        if (forceRefresh) sourceLoader.invalidate(key)
        return sourceLoader.get(key) ?: PhotoSourceResult.Unavailable
    }

    fun invalidate(applicantId: Int) {
        runBlocking {
            loader.invalidate(applicantId)
            sourceLoader.invalidateMatching { it.applicantId == applicantId }
        }
    }

    suspend fun clear() {
        loader.clear()
        sourceLoader.clear()
    }

    private suspend fun fetchThumbnail(id: Int): Bitmap? =
        when (val encoded = downloadEncoded(id)) {
            is EncodedPhoto.Bytes -> decodeThumbnail(encoded.bytes)
            is EncodedPhoto.Rejected -> null
        }

    private suspend fun fetchSource(key: PhotoKey): PhotoSourceResult {
        if (key.scope.origin != configuredOrigin) return PhotoSourceResult.Unavailable
        return when (val encoded = downloadEncoded(key.applicantId)) {
            is EncodedPhoto.Rejected -> encoded.result
            is EncodedPhoto.Bytes -> decodeSource(encoded.bytes)
        }
    }

    private suspend fun downloadEncoded(applicantId: Int): EncodedPhoto {
        var url = try {
            "${root}show-photo/$applicantId".toHttpUrl()
        } catch (_: Exception) {
            return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
        }
        repeat(MAX_REDIRECTS + 1) {
            val request = Request.Builder()
                .url(url)
                .header("Accept-Encoding", "identity")
                .get()
                .build()
            val response = try {
                executePhoto(request)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
            }
            try {
                if (response.isRedirect) {
                    val location = response.header("Location")
                        ?: return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
                    val next = response.request.url.resolve(location)
                        ?: return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
                    if (!sameOrigin(next)) {
                        return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
                    }
                    url = next
                    return@repeat
                }
                return readEncoded(response)
            } finally {
                response.close()
            }
        }
        return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
    }

    private fun readEncoded(response: Response): EncodedPhoto {
        when (response.code) {
            403 -> return EncodedPhoto.Rejected(PhotoSourceResult.AuthenticationRequired)
            404 -> return EncodedPhoto.Rejected(PhotoSourceResult.Missing)
            200 -> Unit
            else -> return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
        }
        val body = response.body
            ?: return EncodedPhoto.Rejected(PhotoSourceResult.Unavailable)
        val declared = body.contentLength()
        if (declared > MAX_ENCODED_BYTES) {
            return EncodedPhoto.Rejected(PhotoSourceResult.TooLarge)
        }
        val media = response.header("Content-Type").orEmpty()
            .substringBefore(';').trim().lowercase()
        if (media == "text/html" || !media.startsWith("image/")) {
            return EncodedPhoto.Rejected(PhotoSourceResult.AuthenticationRequired)
        }
        val bytes = streamLimited(body)
            ?: return EncodedPhoto.Rejected(PhotoSourceResult.TooLarge)
        if (looksLikeLoginHtml(bytes)) {
            return EncodedPhoto.Rejected(PhotoSourceResult.AuthenticationRequired)
        }
        return EncodedPhoto.Bytes(bytes)
    }

    private fun streamLimited(body: okhttp3.ResponseBody): ByteArray? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        body.byteStream().use { input ->
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                if (out.size().toLong() + n > MAX_ENCODED_BYTES) return null
                out.write(buf, 0, n)
            }
        }
        return out.toByteArray()
    }

    private fun looksLikeLoginHtml(bytes: ByteArray): Boolean {
        if (bytes.isEmpty() || bytes[0] != '<'.code.toByte()) return false
        val probe = bytes.decodeToString(
            0,
            minOf(bytes.size, 4096),
        ).lowercase()
        return "user-login" in probe ||
            "user_login" in probe ||
            ("name=\"name\"" in probe && "name=\"pass\"" in probe)
    }

    private fun sameOrigin(url: HttpUrl): Boolean {
        val raw = buildString {
            append(url.scheme)
            append("://")
            append(url.host)
            append(':')
            append(url.port)
            append(configuredBasePath)
        }
        return try {
            PhotoOrigins.canonicalize(raw) == configuredOrigin
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun executePhoto(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = photoClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isActive) return
                    if (call.isCanceled()) {
                        cont.cancel()
                    } else {
                        cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        cont.resume(response)
                    } catch (_: IllegalStateException) {
                        response.close()
                    }
                }
            })
        }

    private fun decodeThumbnail(data: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }

    private fun decodeSource(data: ByteArray): PhotoSourceResult {
        val sha256 = PhotoStamps.sha256Hex(data)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return PhotoSourceResult.UnsupportedImage
        }
        val pixels = bounds.outWidth.toLong() * bounds.outHeight.toLong()
        if (pixels > MAX_SOURCE_PIXELS) return PhotoSourceResult.UnsupportedImage
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, opts)
            ?: return PhotoSourceResult.UnsupportedImage
        val stamp = PhotoStamp(sha256, bitmap.width, bitmap.height)
        return PhotoSourceResult.Ready(PhotoSource(stamp, bitmap))
    }

    private sealed class EncodedPhoto {
        class Bytes(val bytes: ByteArray) : EncodedPhoto()
        class Rejected(val result: PhotoSourceResult) : EncodedPhoto()
    }

    companion object {
        /** Photos load 5–10 at a time; the middle keeps the desk snappy. */
        const val MAX_CONCURRENT = 6
        const val MAX_CACHE_BYTES = 32L * 1024 * 1024
        internal const val MAX_ENCODED_BYTES = 10L * 1024 * 1024
        internal const val MAX_SOURCE_PIXELS = 8_000_000L
        private const val MAX_DIMENSION = 1024
        private const val MAX_REDIRECTS = 10
    }
}
