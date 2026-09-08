package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoDrafts
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoReviewState
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamp
import org.dhamma.dipi.staff.model.PhotoWriteState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class PhotoCorrectionStoreTest {
    private val source = PhotoStamp("ab".repeat(32), 120, 160)
    private val output = PhotoStamp("cd".repeat(32), 40, 50)
    private val recipe = PhotoRecipe(90, PhotoCrop(5, 6, 40, 50))

    @Test
    fun `same applicant stays isolated across origins centres and courses`() {
        val prefs = prefs("pc-scope")
        val store = PhotoCorrectionStore { prefs }
        val originA = PhotoScope("https://one.example.test", 91, 100)
        val originB = PhotoScope("https://two.example.test", 91, 100)
        val centreB = PhotoScope("https://one.example.test", 92, 100)
        val courseB = PhotoScope("https://one.example.test", 91, 101)
        val scopes = listOf(originA, originB, centreB, courseB)
        scopes.forEach { store.save(draft(it, review = PhotoReviewState.DRAFT)) }

        store.clearCourse(originA)
        assertTrue(store.drafts(originA).isEmpty())
        assertEquals(listOf(draft(originB, review = PhotoReviewState.DRAFT)), store.drafts(originB))
        assertEquals(listOf(draft(centreB, review = PhotoReviewState.DRAFT)), store.drafts(centreB))
        assertEquals(listOf(draft(courseB, review = PhotoReviewState.DRAFT)), store.drafts(courseB))

        assertFalse(prefs.all.keys.any { it.contains("Ravi") || it.contains("name", ignoreCase = true) })
        prefs.all.keys.filter { it.startsWith("drafts.") }.forEach { key ->
            assertTrue(key.matches(Regex("drafts\\.[0-9a-f]{64}")))
        }
    }

    @Test
    fun `approved geometry operation id and source stamp round trip`() {
        val prefs = prefs("pc-roundtrip")
        val first = PhotoCorrectionStore { prefs }
        val scope = PhotoScope("https://one.example.test", 91, 100)
        val saved = draft(scope).copy(
            review = PhotoReviewState.APPROVED,
            editRevision = 4,
            write = PhotoWriteState.NOT_STARTED,
            operationId = "op-44",
            output = output,
        )
        first.save(saved)

        val restarted = PhotoCorrectionStore { prefs }
        assertEquals(saved, restarted.drafts(scope).single())
        assertEquals(source, restarted.drafts(scope).single().source)
        assertEquals("op-44", restarted.drafts(scope).single().operationId)
        assertEquals(recipe, restarted.drafts(scope).single().recipe)
        assertEquals(PhotoReviewState.APPROVED, restarted.drafts(scope).single().review)

        val suffix = MessageDigest.getInstance("SHA-256")
            .digest("${scope.origin}\n${scope.centreId}\n${scope.courseId}".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertTrue(prefs.contains("drafts.$suffix"))
        assertFalse(prefs.all.values.filterIsInstance<String>().any { it.contains("Ravi") })
    }

    @Test
    fun `submitting and verifying restore as unknown not committed`() {
        val prefs = prefs("pc-restore")
        val store = PhotoCorrectionStore { prefs }
        val scope = PhotoScope("https://one.example.test", 91, 100)

        store.save(draft(scope).copy(write = PhotoWriteState.SUBMITTING, operationId = "op-sub"))
        val submitting = store.drafts(scope).single()
        assertEquals(PhotoWriteState.UNKNOWN, submitting.write)
        assertEquals(PhotoDrafts.restoreWrite(PhotoWriteState.SUBMITTING), submitting.write)
        assertNotEquals(PhotoWriteState.COMMITTED, submitting.write)
        assertEquals(PhotoDrafts.restore(draft(scope).copy(write = PhotoWriteState.SUBMITTING, operationId = "op-sub")), submitting)

        store.save(draft(scope).copy(write = PhotoWriteState.VERIFYING, operationId = "op-ver"))
        val verifying = store.drafts(scope).single()
        assertEquals(PhotoWriteState.UNKNOWN, verifying.write)
        assertNotEquals(PhotoWriteState.COMMITTED, verifying.write)

        store.save(draft(scope).copy(write = PhotoWriteState.COMMITTED, operationId = "op-ok"))
        assertEquals(PhotoWriteState.COMMITTED, store.drafts(scope).single().write)
    }

    @Test
    fun `corrupted json is empty recovery not committed`() {
        val prefs = prefs("pc-corrupt")
        val store = PhotoCorrectionStore { prefs }
        val scope = PhotoScope("https://one.example.test", 91, 100)
        store.save(draft(scope).copy(write = PhotoWriteState.COMMITTED, operationId = "op-lost"))
        val key = prefs.all.keys.single { it.startsWith("drafts.") }
        prefs.edit().putString(key, "{not-json").commit()

        assertTrue(store.drafts(scope).isEmpty())
        assertTrue(store.recoveryNeeded(scope))
        assertTrue(store.drafts(scope).none { it.write == PhotoWriteState.COMMITTED })
        assertFalse(store.legacyDiscarded())
    }

    @Test
    fun `wipeAll removes every scope`() {
        val prefs = prefs("pc-wipe")
        val store = PhotoCorrectionStore { prefs }
        val a = PhotoScope("https://one.example.test", 91, 100)
        val b = PhotoScope("https://two.example.test", 92, 101)
        store.save(draft(a))
        store.save(draft(b, applicantId = 45))
        store.markLegacyDiscarded()
        assertTrue(store.legacyDiscarded())

        store.wipeAll()
        assertTrue(store.drafts(a).isEmpty())
        assertTrue(store.drafts(b).isEmpty())
        assertFalse(store.legacyDiscarded())
        assertTrue(prefs.all.isEmpty())
    }

    @Test
    fun `save throws when commit fails`() {
        val inner = prefs("pc-fail")
        val failing = object : SharedPreferences by inner {
            override fun edit(): SharedPreferences.Editor {
                val real = inner.edit()
                return object : SharedPreferences.Editor {
                    override fun putString(key: String?, value: String?) = apply { real.putString(key, value) }
                    override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { real.putStringSet(key, values) }
                    override fun putInt(key: String?, value: Int) = apply { real.putInt(key, value) }
                    override fun putLong(key: String?, value: Long) = apply { real.putLong(key, value) }
                    override fun putFloat(key: String?, value: Float) = apply { real.putFloat(key, value) }
                    override fun putBoolean(key: String?, value: Boolean) = apply { real.putBoolean(key, value) }
                    override fun remove(key: String?) = apply { real.remove(key) }
                    override fun clear() = apply { real.clear() }
                    override fun commit(): Boolean = false
                    override fun apply() = Unit
                }
            }
        }
        val store = PhotoCorrectionStore { failing }
        val scope = PhotoScope("https://one.example.test", 91, 100)
        assertThrows(IllegalStateException::class.java) { store.save(draft(scope)) }
        assertTrue(PhotoCorrectionStore { inner }.drafts(scope).isEmpty())
    }

    private fun prefs(name: String): SharedPreferences {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        return prefs
    }

    private fun draft(
        scope: PhotoScope,
        applicantId: Int = 44,
        review: PhotoReviewState = PhotoReviewState.APPROVED,
    ) = PhotoDraft(
        key = PhotoKey(scope, applicantId),
        source = source,
        recipe = recipe,
        review = review,
        editRevision = 3,
        write = PhotoWriteState.NOT_STARTED,
        operationId = "op-44",
        output = output,
    )
}
