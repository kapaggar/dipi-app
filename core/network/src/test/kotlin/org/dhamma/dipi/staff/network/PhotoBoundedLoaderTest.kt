package org.dhamma.dipi.staff.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Pure-Kotlin tests for the limiter, dedup and LRU behind [PhotoLoader]. */
class PhotoBoundedLoaderTest {

    private fun scope() = CoroutineScope(Dispatchers.Default)

    @Test
    fun concurrencyNeverExceedsTheGate() = runBlocking {
        val active = AtomicInteger(0)
        val peak = AtomicInteger(0)
        val loader = BoundedLoader<Int, String>(
            maxConcurrent = 6,
            maxBytes = Long.MAX_VALUE,
            scope = scope(),
            sizeOf = { it.length.toLong() },
        ) { id ->
            val now = active.incrementAndGet()
            peak.updateAndGet { maxOf(it, now) }
            delay(40)
            active.decrementAndGet()
            "photo-$id"
        }
        val results = (1..40).map { async { loader.get(it) } }.awaitAll()
        assertEquals(40, results.filterNotNull().size)
        assertTrue("peak ${peak.get()} must be 1..6", peak.get() in 1..6)
    }

    @Test
    fun concurrentRequestsForSameKeyShareOneFetch() = runBlocking {
        val calls = AtomicInteger(0)
        val release = CompletableDeferred<Unit>()
        val loader = BoundedLoader<Int, String>(6, Long.MAX_VALUE, scope(), { 1L }) { id ->
            calls.incrementAndGet()
            release.await()
            "photo-$id"
        }
        val a = async { loader.get(7) }
        val b = async { loader.get(7) }
        delay(50)
        release.complete(Unit)
        assertEquals("photo-7", a.await())
        assertEquals("photo-7", b.await())
        assertEquals(1, calls.get())
    }

    @Test
    fun lruEvictsOldestAndKeepsRecentlyUsed() = runBlocking {
        val fetches = ConcurrentHashMap<Int, Int>()
        val loader = BoundedLoader<Int, String>(1, maxBytes = 2, scope = scope(), sizeOf = { 1L }) { id ->
            fetches.merge(id, 1, Int::plus)
            "v$id"
        }
        loader.get(1)
        loader.get(2)
        loader.get(1) // touch 1 so 2 is the eviction candidate
        loader.get(3) // over budget: evicts 2
        assertEquals(2L, loader.cachedBytes())

        loader.get(1) // still cached
        loader.get(3) // still cached
        loader.get(2) // evicted, refetches
        assertEquals(2, fetches[2])
        assertEquals(1, fetches[1])
        assertEquals(1, fetches[3])
    }

    @Test
    fun oversizedValueIsServedButNeverCached() = runBlocking {
        val calls = AtomicInteger(0)
        val loader = BoundedLoader<Int, String>(1, maxBytes = 4, scope = scope(), sizeOf = { 100L }) { id ->
            calls.incrementAndGet()
            "huge-$id"
        }
        assertEquals("huge-1", loader.get(1))
        assertEquals(0L, loader.cachedBytes())
        assertEquals("huge-1", loader.get(1))
        assertEquals(2, calls.get())
    }

    @Test
    fun failuresResolveNullAndAreRetriedNextTime() = runBlocking {
        val calls = AtomicInteger(0)
        val loader = BoundedLoader<Int, String>(1, Long.MAX_VALUE, scope(), { 1L }) { id ->
            when (calls.incrementAndGet()) {
                1 -> null // 404 — no photo
                2 -> throw IllegalStateException("boom")
                else -> "recovered-$id"
            }
        }
        assertNull(loader.get(5))
        assertEquals(0L, loader.cachedBytes())
        assertNull(loader.get(5)) // the thrown fetch also resolves null
        assertEquals("recovered-5", loader.get(5))
        assertEquals(3, calls.get())
    }
}
