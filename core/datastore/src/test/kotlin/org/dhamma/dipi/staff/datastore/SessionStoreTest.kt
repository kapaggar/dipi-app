package org.dhamma.dipi.staff.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
