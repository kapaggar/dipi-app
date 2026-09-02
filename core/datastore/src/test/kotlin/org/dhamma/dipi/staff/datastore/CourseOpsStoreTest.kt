package org.dhamma.dipi.staff.datastore

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.model.TabletMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Spec 2a S1/S3 — the tablet-mode key and the device-PIN store. The PIN file
 * is EncryptedSharedPreferences on a device; tests hand in a plain prefs file
 * through the seam because Robolectric has no keystore (same pattern as
 * SessionStore's lazy `secure`).
 */
@RunWith(RobolectricTestRunner::class)
class CourseOpsStoreTest {

    private fun prefs() = RuntimeEnvironment.getApplication()
        .getSharedPreferences("test_course_ops", Context.MODE_PRIVATE)

    private fun store() = CourseOpsStore { prefs() }

    @Test
    fun pinRoundTripSaltedNotRaw() {
        val store = store()
        assertFalse(store.isPinSet())
        store.setPin("4271")
        assertTrue(store.isPinSet())
        assertTrue(store.checkPin("4271"))
        assertFalse(store.checkPin("0000"))
        assertFalse(store.checkPin(""))
        // The raw digits never persist — every stored value is salt/hash hex.
        prefs().all.values.forEach { value ->
            assertFalse(value.toString().contains("4271"))
        }
    }

    @Test
    fun setPinReplacesSaltAndHash() {
        val store = store()
        store.setPin("1111")
        val firstHash = prefs().getString("pin_hash", null)
        store.setPin("2222")
        assertFalse(store.checkPin("1111"))
        assertTrue(store.checkPin("2222"))
        assertFalse(firstHash == prefs().getString("pin_hash", null))
    }

    @Test
    fun samePinHashesDifferentlyPerSalt() {
        val a = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test_course_ops_a", Context.MODE_PRIVATE)
        val b = RuntimeEnvironment.getApplication()
            .getSharedPreferences("test_course_ops_b", Context.MODE_PRIVATE)
        CourseOpsStore { a }.setPin("4271")
        CourseOpsStore { b }.setPin("4271")
        assertFalse(a.getString("pin_hash", null) == b.getString("pin_hash", null))
    }

    @Test
    fun clearPinAndWipeAllDropTheHash() {
        val store = store()
        store.setPin("4271")
        store.clearPin()
        assertFalse(store.isPinSet())
        assertFalse(store.checkPin("4271"))

        store.setPin("4271")
        store.wipeAll()
        assertFalse(store.isPinSet())
        assertNull(prefs().getString("pin_hash", null))
        assertNull(prefs().getString("pin_salt", null))
    }

    // ---- SessionStore.tablet_mode (the skin pattern)

    @Test
    fun tabletModeDefaultsToDeskAndPersists() = runBlocking {
        val session = SessionStore(RuntimeEnvironment.getApplication())
        assertEquals(TabletMode.DESK, session.tabletMode.first())
        assertEquals(TabletMode.DESK, session.tabletModeOnce())
        session.setTabletMode(TabletMode.COURSE_OPS)
        assertEquals(TabletMode.COURSE_OPS, session.tabletMode.first())
        assertEquals(TabletMode.COURSE_OPS, session.tabletModeOnce())
        session.setTabletMode(TabletMode.DESK)
        assertEquals(TabletMode.DESK, session.tabletMode.first())
    }

    // Erase-all coverage: tablet_mode lives in the same DataStore prefs file
    // wipeAll() clears (the skin pattern); wipeAll itself touches
    // EncryptedSharedPreferences and cannot run without a device keystore.
}
