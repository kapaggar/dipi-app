package org.dhamma.dipi.staff.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.PhotoEdit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoEditStore @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json,
) {
    private val ds = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("dipi_photo_edits")
    }

    val edits: Flow<Map<ApplicantId, PhotoEdit>> = ds.data.map { prefs ->
        decode(prefs[KEY])
    }

    suspend fun snapshot(): Map<ApplicantId, PhotoEdit> = decode(ds.data.first()[KEY])

    suspend fun put(id: ApplicantId, edit: PhotoEdit) {
        ds.edit { prefs ->
            val cur = decode(prefs[KEY]).toMutableMap()
            cur[id] = edit
            prefs[KEY] = json.encodeToString(cur.mapKeys { it.key.value })
        }
    }

    suspend fun clear() {
        ds.edit { it.clear() }
    }

    /** Drops unscoped rotate/cropped/done/uploaded flags. Never treats them as approved or committed. */
    suspend fun discardUnscopedLegacy(): Boolean {
        if (snapshot().isEmpty()) return false
        ds.edit { prefs ->
            prefs[LEGACY_DISCARDED] = true
            prefs[LEGACY_NOTICE] = NOTICE
            prefs.remove(KEY)
        }
        return true
    }

    suspend fun consumeLegacyNotice(): String? {
        if (ds.data.first()[LEGACY_NOTICE].isNullOrBlank()) return null
        ds.edit { it.remove(LEGACY_NOTICE) }
        return NOTICE
    }

    private fun decode(raw: String?): Map<ApplicantId, PhotoEdit> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<Int, PhotoEdit>>(raw).mapKeys { ApplicantId(it.key) }
        }.getOrDefault(emptyMap())
    }

    companion object {
        private val KEY = stringPreferencesKey("edits")
        private val LEGACY_DISCARDED = booleanPreferencesKey("legacy_discarded")
        private val LEGACY_NOTICE = stringPreferencesKey("legacy_notice")
        private const val NOTICE = "Review photo edits again"
    }
}
