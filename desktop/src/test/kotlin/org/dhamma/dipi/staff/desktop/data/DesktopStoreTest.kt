package org.dhamma.dipi.staff.desktop.data

import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.model.CheckInRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun rememberMeSurvivesClearButNotWipe() = runBlocking {
        val dir = tmp.newFolder()
        val store = DesktopStore(dir)
        store.setRemembered(true, "sudha.user", "secret")
        store.saveSession("SESS=abc", "csrf")
        assertEquals("SESS=abc", store.sessionCookie())
        store.clear()
        assertNull(store.sessionCookie())
        val kept = store.remembered()
        assertTrue(kept.on)
        assertEquals("sudha.user", kept.username)
        store.wipeAll()
        assertFalse(store.remembered().on)
        assertEquals("", store.remembered().password)
    }

    @Test
    fun checkInsPersistWithoutNpiKeys() {
        val dir = tmp.newFolder()
        val store = DesktopStore(dir)
        store.setCheckIns(mapOf(12 to CheckInRecord(checkedIn = true, room = "Mbk 8")))
        val again = DesktopStore(dir)
        assertEquals("Mbk 8", again.checkIns()[12]?.room)
        val prefs = dir.listFiles()!!.joinToString { it.readText() }
        assertFalse(prefs.contains("aadhar", ignoreCase = true))
        assertFalse(prefs.contains("passport", ignoreCase = true))
    }
}
