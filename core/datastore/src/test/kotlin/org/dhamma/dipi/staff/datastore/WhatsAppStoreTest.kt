package org.dhamma.dipi.staff.datastore

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.dhamma.dipi.staff.model.*

@RunWith(RobolectricTestRunner::class)
class WhatsAppStoreTest {
    @Test fun `isolates keys by origin and centre and wipes all on erase`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("wa-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store=WhatsAppStore { prefs }
        val a=WhatsAppScope("https://one.example.test",91)
        val b=WhatsAppScope("https://one.example.test",92)
        val c=WhatsAppScope("https://two.example.test",91)
        assertFalse(store.profile(a).enabled)
        store.provision(a,"synthetic-key","synthetic-iv")
        store.save(CentreWhatsAppProfile(a,enabled=true))
        assertTrue(store.configured(a)); assertFalse(store.configured(b)); assertFalse(store.configured(c))
        assertFalse(store.profile(b).enabled)
        var held: ByteArray?=null
        store.withMaterial(a) { key,_ -> held=key; assertEquals(32,key.size) }
        assertTrue(held!!.all { it==0.toByte() })
        store.wipeAll(); assertFalse(store.configured(a)); assertFalse(store.profile(a).enabled)
    }
    @Test fun `restart pauses saved batch and protects uncertain attempts`() {
        val prefs=RuntimeEnvironment.getApplication().getSharedPreferences("wa-recovery",Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val first=WhatsAppStore { prefs }
        val centre=WhatsAppScope("https://one.example.test",91)
        first.saveBatch(WhatsAppBatch(centre,100,44,listOf(WhatsAppAttempt(1,"919000000001",WhatsAppAttemptState.SendStarted)),paused=false))
        val restarted=WhatsAppStore { prefs }
        val recovered=restarted.batch(centre, 100)!!
        assertTrue(recovered.paused); assertEquals(WhatsAppAttemptState.OutcomeUnknown,recovered.attempts.single().state)
        assertNull(restarted.batch(WhatsAppScope("https://one.example.test",92), 100))
        restarted.remove(centre); assertNull(restarted.batch(centre, 100))
        assertFalse(prefs.all.keys.any { it.contains("body") || it.contains("url") })
    }
    @Test fun `migrates pilot secrets to one code and removes legacy entries`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("wa-migration", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val centre = WhatsAppScope("https://one.example.test", 91)
        val suffix = java.security.MessageDigest.getInstance("SHA-256")
            .digest("${centre.origin}\n${centre.centreId}".toByteArray()).joinToString("") { "%02x".format(it) }
        prefs.edit().putString("secret.$suffix", "synthetic-key").putString("iv.$suffix", "synthetic-iv").commit()
        val store = WhatsAppStore { prefs }
        assertTrue(store.configured(centre))
        store.withMaterial(centre) { key, iv -> assertEquals(32, key.size); assertEquals(16, iv.size) }
        assertFalse(prefs.contains("secret.$suffix")); assertFalse(prefs.contains("iv.$suffix"))
        assertTrue(prefs.contains("code.$suffix"))
        val before = prefs.all.toMap()
        try { store.provision(centre, "mistyped-code"); fail("Invalid code accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(before, prefs.all)
    }

    @Test fun `another course can start while earlier progress is unresolved`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("wa-course-conflict", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = WhatsAppStore { prefs }
        val centre = WhatsAppScope("https://one.example.test", 91)
        store.saveBatch(WhatsAppBatch(centre, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.OutcomeUnknown))))
        store.saveBatch(WhatsAppBatch(centre, 101, 44, listOf(WhatsAppAttempt(2, "919000000002"))))
    }
    @Test fun `independent course batches survive restart and targeted discard`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("wa-separated", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = WhatsAppStore { prefs }
        val centre = WhatsAppScope("https://one.example.test", 91)
        val otherCentre = centre.copy(centreId = 92)
        val otherServer = centre.copy(origin = "https://two.example.test")
        val old = WhatsAppBatch(centre, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.OutcomeUnknown)))
        val next = WhatsAppBatch(centre, 101, 44, listOf(WhatsAppAttempt(2, "919000000002")))
        store.saveBatch(old); store.saveBatch(next)
        store.saveBatch(old.copy(scope = otherCentre)); store.saveBatch(old.copy(scope = otherServer))
        val restarted = WhatsAppStore { prefs }
        assertEquals(old, restarted.batch(centre, 100)); assertEquals(next, restarted.batch(centre, 101))
        restarted.clearBatch(centre, 101)
        assertNull(restarted.batch(centre, 101)); assertEquals(old, restarted.batch(centre, 100))
        restarted.remove(centre)
        assertNull(restarted.batch(centre, 100))
        assertNotNull(restarted.batch(otherCentre, 100)); assertNotNull(restarted.batch(otherServer, 100))
        restarted.wipeAll(); assertNull(restarted.batch(otherCentre, 100)); assertNull(restarted.batch(otherServer, 100))
    }

    @Test fun `legacy batch migrates atomically when another course opens first`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("wa-batch-migration", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val centre = WhatsAppScope("https://one.example.test", 91)
        val old = WhatsAppBatch(centre, 100, 44, listOf(WhatsAppAttempt(1, "919000000001", WhatsAppAttemptState.SendStarted)))
        val suffix = java.security.MessageDigest.getInstance("SHA-256")
            .digest("${centre.origin}\n${centre.centreId}".toByteArray()).joinToString("") { "%02x".format(it) }
        prefs.edit().putString("batch.$suffix", Json.encodeToString(old)).commit()
        val store = WhatsAppStore { prefs }
        assertNull(store.batch(centre, 101))
        assertFalse(prefs.contains("batch.$suffix"))
        assertTrue(prefs.contains("batch.100.$suffix"))
        assertEquals(WhatsAppAttemptState.OutcomeUnknown, store.batch(centre, 100)!!.attempts.single().state)
        store.saveBatch(WhatsAppBatch(centre, 101, 44, listOf(WhatsAppAttempt(2, "919000000002"))))
        assertNotNull(store.batch(centre, 100)); assertNotNull(store.batch(centre, 101))
        store.clearBatch(centre, 100)
        assertNull(WhatsAppStore { prefs }.batch(centre, 100))
        assertNotNull(store.batch(centre, 101))
    }

}
