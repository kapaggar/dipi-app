package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoDrafts
import org.dhamma.dipi.staff.model.PhotoScope
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Own encrypted file; never Room, plain DataStore, backup, or an export provider. */
@Singleton
class PhotoCorrectionStore constructor(private val provider: () -> SharedPreferences) {
    @Inject constructor(@ApplicationContext context: Context) : this({
        EncryptedSharedPreferences.create(
            "dipi_photo_corrections",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    })

    private val prefs by lazy(provider)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun key(scope: PhotoScope) = MessageDigest.getInstance("SHA-256")
        .digest("${scope.origin}\n${scope.centreId}\n${scope.courseId}".toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun draftsKey(scope: PhotoScope) = "drafts.${key(scope)}"

    @Synchronized
    fun drafts(scope: PhotoScope): List<PhotoDraft> =
        rawDrafts(scope).map { PhotoDrafts.restore(it) }

    @Synchronized
    fun save(draft: PhotoDraft) {
        val scope = draft.key.scope
        val next = rawDrafts(scope).filterNot { it.key.applicantId == draft.key.applicantId } + draft
        check(prefs.edit().putString(draftsKey(scope), json.encodeToString(next)).commit()) {
            "Could not save photo correction"
        }
    }

    @Synchronized
    fun clearCourse(scope: PhotoScope) {
        check(prefs.edit().remove(draftsKey(scope)).commit()) { "Could not clear photo correction" }
    }

    @Synchronized
    fun wipeAll() {
        check(prefs.edit().clear().commit()) { "Could not erase photo corrections" }
    }

    @Synchronized
    fun recoveryNeeded(scope: PhotoScope): Boolean {
        val raw = prefs.getString(draftsKey(scope), null) ?: return false
        return runCatching { json.decodeFromString<List<PhotoDraft>>(raw) }.isFailure
    }

    @Synchronized
    fun markLegacyDiscarded() {
        check(prefs.edit().putBoolean(LEGACY_DISCARDED, true).commit()) {
            "Could not save photo correction"
        }
    }

    @Synchronized
    fun legacyDiscarded(): Boolean = prefs.getBoolean(LEGACY_DISCARDED, false)

    private fun rawDrafts(scope: PhotoScope): List<PhotoDraft> {
        val raw = prefs.getString(draftsKey(scope), null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<PhotoDraft>>(raw) }
            .getOrDefault(emptyList())
            .filter { it.key.scope == scope }
    }

    companion object {
        private const val LEGACY_DISCARDED = "legacy_discarded"
    }
}
