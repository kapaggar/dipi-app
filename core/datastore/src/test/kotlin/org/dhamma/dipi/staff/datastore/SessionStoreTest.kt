package org.dhamma.dipi.staff.datastore

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SessionStoreTest {

    private fun store(): SessionStore = SessionStore(RuntimeEnvironment.getApplication())

    @Test
    fun deskGenderDefaultsToBothAndPersistsAcrossWrites() = runBlocking {
        val store = store()
        assertEquals("Both", store.deskGender.first())
        store.setDeskGender("Female")
        assertEquals("Female", store.deskGender.first())
        store.setDeskGender("Male")
        assertEquals("Male", store.deskGender.first())
        store.setDeskGender("Both")
        assertEquals("Both", store.deskGender.first())
    }

    @Test
    fun deskSeniorityDefaultsToBothAndPersistsAcrossWrites() = runBlocking {
        val store = store()
        assertEquals("Both", store.deskSeniority.first())
        store.setDeskSeniority("New")
        assertEquals("New", store.deskSeniority.first())
        store.setDeskSeniority("Old")
        assertEquals("Old", store.deskSeniority.first())
        store.setDeskSeniority("Both")
        assertEquals("Both", store.deskSeniority.first())
    }

    @Test
    fun forceOfflineFlowEmitsLiveWritesOnTheSameStore() = runBlocking {
        val store = store()
        assertFalse(store.forceOfflineOnce())
        val becameOn = async { store.forceOffline.dropWhile { !it }.first() }
        store.setForceOffline(true)
        assertTrue(becameOn.await())
        assertTrue(store.forceOfflineOnce())
        val becameOff = async { store.forceOffline.dropWhile { it }.first() }
        store.setForceOffline(false)
        assertFalse(becameOff.await())
        assertFalse(store.forceOfflineOnce())
    }

    @Test
    fun sheetReadingWidthRoundTripsAndClearsOnErase() = runBlocking {
        val store = store()
        assertTrue(store.sheetReadableNames.first().isEmpty())
        store.setSheetScreenWidth(
            org.dhamma.dipi.staff.model.SheetExport.StudentChit,
            org.dhamma.dipi.staff.model.SheetScreenWidth.READABLE,
        )
        assertEquals(
            setOf(org.dhamma.dipi.staff.model.SheetExport.StudentChit.name),
            store.sheetReadableNames.first(),
        )
        store.setSheetScreenWidth(
            org.dhamma.dipi.staff.model.SheetExport.StudentChit,
            org.dhamma.dipi.staff.model.SheetScreenWidth.FIT,
        )
        assertTrue(store.sheetReadableNames.first().isEmpty())
        store.setSheetScreenWidth(
            org.dhamma.dipi.staff.model.SheetExport.Day0List,
            org.dhamma.dipi.staff.model.SheetScreenWidth.READABLE,
        )
        assertEquals(
            setOf(org.dhamma.dipi.staff.model.SheetExport.Day0List.name),
            store.sheetReadableNames.first(),
        )
    }
}
