package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Course-ops device PIN (spec 2a S3, owner decision 2026-09-02): 4 digits,
 * collected when the mode is enabled, salted-SHA-256 in its OWN
 * EncryptedSharedPreferences file `dipi_course_ops`. The PIN gates entering
 * Settings from course ops — which also covers Logout and Erase-all.
 *
 * Lifecycle: NOT wiped by logout (the mode key dies with the session prefs,
 * the PIN does not); wiped by Erase-all via [wipeAll]. The raw digits are
 * hashed immediately and never stored, logged, or kept beyond the call.
 */
@Singleton
class CourseOpsStore
@VisibleForTesting
constructor(prefsProvider: () -> SharedPreferences) {

    @Inject
    constructor(@ApplicationContext context: Context) : this({
        EncryptedSharedPreferences.create(
            "dipi_course_ops",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    })

    // Lazy for the same reason SessionStore is: the keystore master key only
    // exists on a device; unit tests hand in a plain in-memory prefs file.
    private val prefs: SharedPreferences by lazy(prefsProvider)

    fun isPinSet(): Boolean = prefs.getString(PIN_HASH, null) != null

    /** Stores sha256(salt + pin) with a fresh random salt. Digits never persist raw. */
    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(PIN_SALT, salt.toHex())
            .putString(PIN_HASH, hash(salt, pin))
            .commit()
    }

    fun checkPin(pin: String): Boolean {
        val salt = prefs.getString(PIN_SALT, null)?.fromHex() ?: return false
        val stored = prefs.getString(PIN_HASH, null) ?: return false
        return MessageDigest.isEqual(stored.toByteArray(), hash(salt, pin).toByteArray())
    }

    fun clearPin() {
        prefs.edit().remove(PIN_SALT).remove(PIN_HASH).commit()
    }

    /** Erase-all: the whole file goes. */
    fun wipeAll() {
        prefs.edit().clear().commit()
    }

    private fun hash(salt: ByteArray, pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private companion object {
        const val PIN_SALT = "pin_salt"
        const val PIN_HASH = "pin_hash"
        const val SALT_BYTES = 16
    }
}
