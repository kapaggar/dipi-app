package org.dhamma.dipi.staff.datastore

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HealthRow
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TabletMode
import org.dhamma.dipi.staff.model.TeacherRoll
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

    // ---- Course cache (spec 2d, owner amendment 2026-09-02)

    private val healthText = "Insulin-dependent diabetes, takes olanzapine nightly"

    private fun sampleCard() = ApplicationCard(
        name = "Suresh Nair",
        conf = "OM42",
        statusLine = "Confirmed · 10 Day",
        hasPhoto = true,
        personal = listOf("Gender" to "Male", "Monk / Nun" to "No"),
        historyCounts = ApplicationCard.HISTORY_ORDER.map { it to if (it == "10-Day") 11 else 0 },
        firstCourse = "2015-1-15, Dhamma sota sohna",
        lastCourse = "2025-12-12, Dhamma Sudha",
        practiceDetails = "1 hr daily",
        health = ApplicationCard.HEALTH_ORDER.map {
            HealthRow(it, if (it == "Medication") healthText else "-")
        },
    )

    private fun sampleRoll() = TeacherRoll(
        listOf(
            RollGroup(
                at = "Trainee-A-M Teacher", code = "TAM", gender = Gender.M,
                seniority = RollSeniority.OLD, group = "1", total = 1,
                rows = listOf(
                    RollRow(
                        sn = 1, applicantId = ApplicantId(4), name = "Suresh Nair",
                        roleTag = "Sevak", room = "Mbk-8", age = "51", city = "Kochi",
                        courses = listOf("10D" to 11, "STP" to 3), cell = "",
                        seat = "CW-A3", seatKind = SeatKind.CELL, backrest = true,
                        occupation = "Retired Teacher", education = "B.Ed",
                        languages = "Malayalam, English",
                    ),
                ),
            ),
        ),
    )

    @Test
    fun rollAndCardRoundTripForTheStoredCourse() {
        val store = store()
        store.saveRoll(10, sampleRoll())
        store.saveCard(10, 4, sampleCard())

        assertEquals(sampleRoll(), store.loadRoll(10))
        assertEquals(mapOf(4 to sampleCard()), store.loadCards(10))
        // A different course reads nothing.
        assertNull(store.loadRoll(11))
        assertTrue(store.loadCards(11).isEmpty())
    }

    @Test
    fun savingADifferentCourseWipesThePreviousOne() {
        val store = store()
        store.saveRoll(10, sampleRoll())
        store.saveCard(10, 4, sampleCard())

        store.saveRoll(11, sampleRoll())
        // Course 10 is gone entirely — cards included.
        assertNull(store.loadRoll(10))
        assertTrue(store.loadCards(10).isEmpty())
        assertTrue(prefs().all.keys.none { it == "card_4" })
        assertEquals(sampleRoll(), store.loadRoll(11))
    }

    @Test
    fun wipeCourseDropsTheCacheButKeepsThePin() {
        val store = store()
        store.setPin("4271")
        store.saveRoll(10, sampleRoll())
        store.saveCard(10, 4, sampleCard())

        store.wipeCourse()
        assertNull(store.loadRoll(10))
        assertTrue(store.loadCards(10).isEmpty())
        // Logout keeps the device PIN (spec 2a) — only Erase-all removes it.
        assertTrue(store.isPinSet())
        assertTrue(store.checkPin("4271"))
    }

    @Test
    fun wipeAllDropsTheCourseCacheToo() {
        val store = store()
        store.saveRoll(10, sampleRoll())
        store.saveCard(10, 4, sampleCard())
        store.wipeAll()
        assertNull(store.loadRoll(10))
        assertTrue(store.loadCards(10).isEmpty())
        assertTrue(prefs().all.isEmpty())
    }

    @Test
    fun rawHealthTextNeverAppearsInAnyToString() {
        val store = store()
        store.saveCard(10, 4, sampleCard())
        val loaded = store.loadCards(10)
        // The map's toString goes through ApplicationCard/HealthRow toString —
        // both redact; the answer text must never surface.
        assertFalse(loaded.toString().contains(healthText))
        assertFalse(loaded.getValue(4).toString().contains(healthText))
        assertFalse(loaded.getValue(4).health.joinToString { it.toString() }.contains(healthText))
        // But the answer itself is intact for on-screen display.
        assertEquals(healthText, loaded.getValue(4).healthRow("Medication")?.answer)
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
