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
    @Synchronized fun provision(scope: WhatsAppScope, code: String) {
        WhatsAppProvisioningCode.withMaterial(code) { _, _ -> }
        check(prefs.edit().putString("code.${key(scope)}", code.trim())
            .remove("secret.${key(scope)}").remove("iv.${key(scope)}").commit()) { "Could not save provisioning code" }
    }
    /** Compatibility for a tablet provisioned during the two-field pilot. */
    @Synchronized fun provision(scope: WhatsAppScope, secret: String, iv: String) {
        val a = secret.toByteArray(); val b = iv.toByteArray()
        try { provision(scope, WhatsAppProvisioningCode.fromLegacySecrets(a, b)) }
        finally { a.fill(0); b.fill(0) }
    }
    @Synchronized fun configured(scope: WhatsAppScope) = prefs.contains("code.${key(scope)}") ||
        (prefs.contains("secret.${key(scope)}") && prefs.contains("iv.${key(scope)}"))
    @Synchronized fun <T> withMaterial(scope: WhatsAppScope, block: (ByteArray, ByteArray) -> T): T {
        if (!prefs.contains("code.${key(scope)}")) {
            val secret = prefs.getString("secret.${key(scope)}", null) ?: error("Provision the centre's code first")
            val iv = prefs.getString("iv.${key(scope)}", null) ?: error("Provision the centre's code first")
            provision(scope, secret, iv)
        }
        return WhatsAppProvisioningCode.withMaterial(prefs.getString("code.${key(scope)}", null) ?: error("Provision the centre's code first"), block)
    }
    @Synchronized fun pilotResult(scope: WhatsAppScope): String? = prefs.getString("pilot.${key(scope)}", null)
    @Synchronized fun savePilotResult(scope: WhatsAppScope, result: String) {
        check(prefs.edit().putString("pilot.${key(scope)}", result).commit()) { "Could not save device check" }
    }
    @Synchronized fun saveBatch(batch: WhatsAppBatch) {
        val previous = this.batch(batch.scope)
        require(previous == null || previous.courseId == batch.courseId || previous.attempts.all {
            it.state in setOf(WhatsAppAttemptState.SubmissionObserved, WhatsAppAttemptState.Skipped)
        }) { "Another course has unfinished WhatsApp progress. Return to that course and review or discard its batch first." }
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
