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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.network.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {
    // Lazy: the keystore master key only exists on a device; unit tests exercise
    // the DataStore prefs without touching EncryptedSharedPreferences.
    private val secure: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            "dipi_staff_secure",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

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
            .commit()
    }

    data class Remembered(val on: Boolean, val username: String, val password: String)

    fun remembered(): Remembered = Remembered(
        on = secure.getBoolean(REMEMBER, false),
        username = secure.getString(REMEMBER_USER, "").orEmpty(),
        password = secure.getString(REMEMBER_PASS, "").orEmpty(),
    )

    fun setRemembered(on: Boolean, username: String, password: String) {
        secure.edit()
            .putBoolean(REMEMBER, on)
            .apply {
                if (on) {
                    putString(REMEMBER_USER, username)
                    putString(REMEMBER_PASS, password)
                } else {
                    remove(REMEMBER_USER)
                    remove(REMEMBER_PASS)
                }
            }
            .commit()
    }

    override suspend fun clear() {
        val kept = remembered()
        secure.edit().clear().commit()
        if (kept.on) setRemembered(true, kept.username, kept.password)
        ds.edit { it.clear() }
    }

    /** Factory reset: drop cookies, remember-me, theme, and account. */
    suspend fun wipeAll() {
        secure.edit().clear().commit()
        ds.edit { it.clear() }
    }

    suspend fun setTheme(dark: Boolean) {
        ds.edit { it[DARK] = dark }
    }

    val darkTheme: Flow<Boolean> = ds.data.map { it[DARK] ?: false }

    /** Desk skin key ("steel" … "still"). Device-local UI preference — wiped by Erase-all. */
    suspend fun setSkin(key: String) {
        ds.edit { it[SKIN] = key }
    }

    val skin: Flow<String> = ds.data.map { it[SKIN] ?: "steel" }

    /** Lotus decoration (sign-in hero + desk watermark). One switch governs both. */
    suspend fun setLotus(on: Boolean) {
        ds.edit { it[LOTUS] = on }
    }

    val lotus: Flow<Boolean> = ds.data.map { it[LOTUS] ?: true }

    suspend fun setLastSync(iso: String) {
        ds.edit { it[SYNC] = iso }
    }

    val lastSync: Flow<String?> = ds.data.map { it[SYNC] }

    suspend fun setForceOffline(value: Boolean) {
        ds.edit { it[OFFLINE] = value }
    }

    val forceOffline: Flow<Boolean> = ds.data.map { it[OFFLINE] ?: false }

    val centreOps: Flow<CentreOpsPrefs> = ds.data.map { decodeCentreOps(it[CENTRE_OPS]) }

    suspend fun setCentreOps(prefs: CentreOpsPrefs) {
        ds.edit { it[CENTRE_OPS] = opsJson.encodeToString(CentreOpsPrefs.serializer(), prefs) }
    }

    suspend fun centreOpsOnce(): CentreOpsPrefs = decodeCentreOps(ds.data.first()[CENTRE_OPS])

    private fun decodeCentreOps(raw: String?): CentreOpsPrefs {
        if (raw.isNullOrBlank()) return CentreOpsPrefs()
        return runCatching { opsJson.decodeFromString(CentreOpsPrefs.serializer(), raw) }
            .getOrDefault(CentreOpsPrefs())
    }


    /** Zero-day desk gender filter ("Both"/"Male"/"Female") — which desk this tablet sits on. */
    suspend fun setDeskGender(value: String) {
        ds.edit { it[DESK_GENDER] = value }
    }

    val deskGender: Flow<String> = ds.data.map { it[DESK_GENDER] ?: "Both" }

    /** Day 0 check-in records, keyed by applicant id. Local truth until a server endpoint exists. */
    val checkIns: Flow<Map<Int, CheckInRecord>> = ds.data.map { decodeCheckIns(it[CHECK_INS]) }

    suspend fun setCheckIns(records: Map<Int, CheckInRecord>) {
        ds.edit { it[CHECK_INS] = opsJson.encodeToString(records) }
    }

    /** One-shot read for the room-allocation sync's read-modify-write marks. */
    suspend fun checkInsOnce(): Map<Int, CheckInRecord> = decodeCheckIns(ds.data.first()[CHECK_INS])

    private fun decodeCheckIns(raw: String?): Map<Int, CheckInRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { opsJson.decodeFromString<Map<Int, CheckInRecord>>(raw) }
            .getOrDefault(emptyMap())
    }

    /** Call-round log, keyed by applicant id. Device-local truth — never sent to the server. */
    val callLog: Flow<Map<Int, CallRecord>> = ds.data.map { decodeCallLog(it[CALL_LOG]) }

    suspend fun setCallLog(records: Map<Int, CallRecord>) {
        ds.edit { it[CALL_LOG] = opsJson.encodeToString(records) }
    }

    private fun decodeCallLog(raw: String?): Map<Int, CallRecord> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching { opsJson.decodeFromString<Map<Int, CallRecord>>(raw) }
            .getOrDefault(emptyMap())
    }

    suspend fun setAccountJson(json: String?) {
        ds.edit {
            if (json == null) it.remove(ACCOUNT) else it[ACCOUNT] = json
        }
    }

    suspend fun accountJson(): String? = ds.data.first()[ACCOUNT]

    companion object {
        private const val COOKIE = "cookie"
        private const val CSRF = "csrf"
        private const val REMEMBER = "remember"
        private const val REMEMBER_USER = "remember_user"
        private const val REMEMBER_PASS = "remember_pass"
        private val DARK = booleanPreferencesKey("dark")
        private val SKIN = stringPreferencesKey("skin")
        private val LOTUS = booleanPreferencesKey("lotus")
        private val SYNC = stringPreferencesKey("sync")
        private val OFFLINE = booleanPreferencesKey("offline")
        private val ACCOUNT = stringPreferencesKey("account")
        private val CENTRE_OPS = stringPreferencesKey("centre_ops")
        private val CHECK_INS = stringPreferencesKey("check_ins")
        private val DESK_GENDER = stringPreferencesKey("desk_gender")
        private val CALL_LOG = stringPreferencesKey("call_log")
        private val opsJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

