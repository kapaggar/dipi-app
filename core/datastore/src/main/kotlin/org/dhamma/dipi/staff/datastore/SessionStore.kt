package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.dhamma.dipi.staff.network.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {
    private val secure: SharedPreferences = EncryptedSharedPreferences.create(
        "dipi_staff_secure",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val ds = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("dipi_staff_prefs")
    }

    override suspend fun sessionCookie(): String? = secure.getString(COOKIE, null)

    override suspend fun csrf(): String? = secure.getString(CSRF, null)

    override suspend fun saveSession(cookie: String?, csrf: String?) {
        secure.edit()
            .apply {
                if (cookie == null) remove(COOKIE) else putString(COOKIE, cookie)
                if (csrf == null) remove(CSRF) else putString(CSRF, csrf)
            }
            .apply()
    }

    override suspend fun clear() {
        secure.edit().clear().apply()
        ds.edit { it.clear() }
    }

    suspend fun setTheme(dark: Boolean) {
        ds.edit { it[DARK] = dark }
    }

    val darkTheme: Flow<Boolean> = ds.data.map { it[DARK] ?: false }

    suspend fun setLastSync(iso: String) {
        ds.edit { it[SYNC] = iso }
    }

    val lastSync: Flow<String?> = ds.data.map { it[SYNC] }

    suspend fun setForceOffline(value: Boolean) {
        ds.edit { it[OFFLINE] = value }
    }

    val forceOffline: Flow<Boolean> = ds.data.map { it[OFFLINE] ?: false }

    suspend fun setAccountJson(json: String?) {
        ds.edit {
            if (json == null) it.remove(ACCOUNT) else it[ACCOUNT] = json
        }
    }

    suspend fun accountJson(): String? = ds.data.first()[ACCOUNT]

    companion object {
        private const val COOKIE = "cookie"
        private const val CSRF = "csrf"
        private val DARK = booleanPreferencesKey("dark")
        private val SYNC = stringPreferencesKey("sync")
        private val OFFLINE = booleanPreferencesKey("offline")
        private val ACCOUNT = stringPreferencesKey("account")
    }
}
