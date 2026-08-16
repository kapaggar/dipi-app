package org.dhamma.dipi.staff.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

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
 *   retries.
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
    private val fetch: suspend (K) -> V?,
) {
    private val gate = Semaphore(maxConcurrent)
    private val lock = Mutex()
    private val lru = LinkedHashMap<K, V>(16, 0.75f, true)
    private val inFlight = HashMap<K, Deferred<V?>>()
    private var bytes = 0L

    suspend fun get(key: K): V? {
        val pending = lock.withLock {
            lru[key]?.let { return it }
            inFlight.getOrPut(key) {
                scope.async {
                    val value = try {
                        gate.withPermit { fetch(key) }
                    } catch (_: Exception) {
                        null
                    }
                    lock.withLock {
                        inFlight.remove(key)
                        if (value != null) putLocked(key, value)
                    }
                    value
                }
            }
        }
        return pending.await()
    }

    suspend fun cachedBytes(): Long = lock.withLock { bytes }

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

    private val loader = BoundedLoader<Int, Bitmap>(
        maxConcurrent = MAX_CONCURRENT,
        maxBytes = MAX_CACHE_BYTES,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        sizeOf = { it.allocationByteCount.toLong() },
        fetch = { id -> fetch(id) },
    )

    /** Null on any failure — the initials placeholder stays. */
    suspend fun load(applicantId: Int): Bitmap? = loader.get(applicantId)

    private suspend fun fetch(id: Int): Bitmap? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("${root}show-photo/$id").get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            // A dead session answers 200 with the login page — only decode images.
            val type = resp.header("Content-Type").orEmpty()
            if (!type.startsWith("image/")) return@use null
            decode(resp.body?.bytes() ?: return@use null)
        }
    }

    private fun decode(data: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(data, 0, data.size, opts)
    }

    companion object {
        /** Photos load 5–10 at a time; the middle keeps the desk snappy. */
        const val MAX_CONCURRENT = 6
        const val MAX_CACHE_BYTES = 32L * 1024 * 1024
        private const val MAX_DIMENSION = 1024
    }
}
