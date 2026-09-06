package org.dhamma.dipi.staff.datastore

import android.content.Context
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
        store.withSecrets(a) { key,_ -> held=key; assertEquals("synthetic-key",String(key)) }
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
        val recovered=restarted.batch(centre)!!
        assertTrue(recovered.paused); assertEquals(WhatsAppAttemptState.OutcomeUnknown,recovered.attempts.single().state)
        assertNull(restarted.batch(WhatsAppScope("https://one.example.test",92)))
        restarted.remove(centre); assertNull(restarted.batch(centre))
        assertFalse(prefs.all.keys.any { it.contains("body") || it.contains("url") })
    }
}
