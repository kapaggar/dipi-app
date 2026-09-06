package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.*
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Own encrypted file; never Room, plain DataStore, backup, or an export provider. */
@Singleton
class WhatsAppStore constructor(private val provider: () -> SharedPreferences) {
    @Inject constructor(@ApplicationContext context: Context) : this({
        EncryptedSharedPreferences.create("dipi_whatsapp", MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC), context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    })
    private val prefs by lazy(provider)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun key(scope: WhatsAppScope) = MessageDigest.getInstance("SHA-256")
        .digest("${scope.origin}\n${scope.centreId}".toByteArray()).joinToString("") { "%02x".format(it) }
    @Synchronized fun profile(scope: WhatsAppScope): CentreWhatsAppProfile =
        prefs.getString("profile.${key(scope)}", null)?.let { runCatching { json.decodeFromString<CentreWhatsAppProfile>(it) }.getOrNull() }
            ?.takeIf { it.scope == scope } ?: CentreWhatsAppProfile(scope)
    @Synchronized fun save(profile: CentreWhatsAppProfile) {
        check(prefs.edit().putString("profile.${key(profile.scope)}", json.encodeToString(profile)).commit()) { "Could not save WhatsApp settings" }
    }
    @Synchronized fun provision(scope: WhatsAppScope, key: String, iv: String) {
        require(key.isNotBlank() && iv.isNotBlank() && key.length <= 4096 && iv.length <= 4096) { "Enter both encryption values" }
        check(prefs.edit().putString("secret.${this.key(scope)}", key).putString("iv.${this.key(scope)}", iv).commit()) { "Could not save letter key" }
    }
    @Synchronized fun configured(scope: WhatsAppScope) = prefs.contains("secret.${key(scope)}") && prefs.contains("iv.${key(scope)}")
    @Synchronized fun <T> withSecrets(scope: WhatsAppScope, block: (ByteArray, ByteArray) -> T): T {
        val secret = prefs.getString("secret.${key(scope)}", null)?.toByteArray() ?: error("Provision the centre letter key first")
        val iv = prefs.getString("iv.${key(scope)}", null)?.toByteArray() ?: error("Provision the centre letter key first")
        try { return block(secret, iv) } finally { secret.fill(0); iv.fill(0) }
    }
    @Synchronized fun saveBatch(batch: WhatsAppBatch) {
        check(prefs.edit().putString("batch.${key(batch.scope)}", json.encodeToString(batch)).commit()) { "Could not save messaging progress" }
    }
    @Synchronized fun batch(scope: WhatsAppScope): WhatsAppBatch? =
        prefs.getString("batch.${key(scope)}", null)?.let { runCatching { json.decodeFromString<WhatsAppBatch>(it) }.getOrNull() }
            ?.takeIf { it.scope == scope }?.interrupted()
    @Synchronized fun clearBatch(scope: WhatsAppScope) { check(prefs.edit().remove("batch.${key(scope)}").commit()) }
    @Synchronized fun remove(scope: WhatsAppScope) {
        val suffix = ".${key(scope)}"
        val edit = prefs.edit()
        prefs.all.keys.filter { it.endsWith(suffix) }.forEach { edit.remove(it) }
        check(edit.commit())
    }
    @Synchronized fun wipeAll() { check(prefs.edit().clear().commit()) }
}
