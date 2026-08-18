package org.dhamma.dipi.staff.desktop.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.network.TokenStore
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Linux stand-in for EncryptedSharedPreferences + DataStore.
 *
 * Cookies and remember-me live in an AES-GCM blob (`secret.bin`) keyed by a
 * 0600 file. Worklist / outbox / check-ins are public-card JSON only — never
 * NPI. SensitiveInfo is not written here.
 */
class DesktopStore(
    private val dir: File,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : TokenStore {
    private val keyFile = File(dir, ".key")
    private val secretFile = File(dir, "secret.bin")
    private val prefsFile = File(dir, "prefs.json")
    private val worklistFile = File(dir, "worklist.json")
    private val outboxFile = File(dir, "outbox.json")

    @Volatile private var secret = SecretBlob()
    @Volatile private var prefs = PrefsBlob()

    init {
        dir.mkdirs()
        restrict(dir)
        load()
    }

    data class Remembered(val on: Boolean, val username: String, val password: String)

    override suspend fun sessionCookie(): String? = secret.cookie
    override suspend fun csrf(): String? = secret.csrf

    override suspend fun saveSession(cookie: String?, csrf: String?) {
        secret = secret.copy(cookie = cookie, csrf = csrf)
        persistSecret()
    }

    override suspend fun clear() {
        val kept = remembered()
        secret = SecretBlob()
        if (kept.on) secret = secret.copy(remember = true, user = kept.username, pass = kept.password)
        persistSecret()
        prefs = prefs.copy(accountJson = null, lastSync = null)
        persistPrefs()
    }

    fun remembered(): Remembered = Remembered(secret.remember, secret.user, secret.pass)

    fun setRemembered(on: Boolean, username: String, password: String) {
        secret = if (on) {
            secret.copy(remember = true, user = username, pass = password)
        } else {
            secret.copy(remember = false, user = "", pass = "")
        }
        persistSecret()
    }

    suspend fun wipeAll() {
        secret = SecretBlob()
        prefs = PrefsBlob()
        persistSecret()
        persistPrefs()
        worklistFile.delete()
        outboxFile.delete()
    }

    fun accountJson(): String? = prefs.accountJson
    fun setAccountJson(raw: String?) {
        prefs = prefs.copy(accountJson = raw)
        persistPrefs()
    }

    fun lastSync(): String? = prefs.lastSync
    fun setLastSync(iso: String?) {
        prefs = prefs.copy(lastSync = iso)
        persistPrefs()
    }

    fun dark(): Boolean = prefs.dark
    fun setDark(on: Boolean) {
        prefs = prefs.copy(dark = on)
        persistPrefs()
    }

    fun lotus(): Boolean = prefs.lotus
    fun setLotus(on: Boolean) {
        prefs = prefs.copy(lotus = on)
        persistPrefs()
    }

    fun deskGender(): String = prefs.deskGender
    fun setDeskGender(g: String) {
        prefs = prefs.copy(deskGender = g)
        persistPrefs()
    }

    fun deskSeniority(): String = prefs.deskSeniority
    fun setDeskSeniority(s: String) {
        prefs = prefs.copy(deskSeniority = s)
        persistPrefs()
    }

    fun centreOps(): CentreOpsPrefs = prefs.centreOps
    fun setCentreOps(ops: CentreOpsPrefs) {
        prefs = prefs.copy(centreOps = ops)
        persistPrefs()
    }

    fun checkIns(): Map<Int, CheckInRecord> = prefs.checkIns
    fun setCheckIns(records: Map<Int, CheckInRecord>) {
        prefs = prefs.copy(checkIns = records)
        persistPrefs()
    }

    fun callLog(): Map<Int, CallRecord> = prefs.callLog
    fun setCallLog(records: Map<Int, CallRecord>) {
        prefs = prefs.copy(callLog = records)
        persistPrefs()
    }

    fun forceOffline(): Boolean = prefs.forceOffline
    fun setForceOffline(on: Boolean) {
        prefs = prefs.copy(forceOffline = on)
        persistPrefs()
    }

    fun loadWorklist(): List<CachedApplicant> =
        runCatching { json.decodeFromString<List<CachedApplicant>>(worklistFile.readText()) }
            .getOrDefault(emptyList())

    fun saveWorklist(rows: List<CachedApplicant>) {
        atomicWrite(worklistFile, json.encodeToString(rows))
    }

    fun clearWorklist() {
        worklistFile.delete()
    }

    fun loadOutbox(): List<OutboxRow> =
        runCatching { json.decodeFromString<List<OutboxRow>>(outboxFile.readText()) }
            .getOrDefault(emptyList())

    fun saveOutbox(rows: List<OutboxRow>) {
        atomicWrite(outboxFile, json.encodeToString(rows))
    }

    fun clearOutbox() {
        outboxFile.delete()
    }

    private fun load() {
        if (secretFile.exists()) {
            runCatching {
                secret = json.decodeFromString(decrypt(secretFile.readBytes()).decodeToString())
            }
        }
        if (prefsFile.exists()) {
            runCatching { prefs = json.decodeFromString(prefsFile.readText()) }
        }
    }

    private fun persistSecret() {
        val bytes = encrypt(json.encodeToString(secret).encodeToByteArray())
        atomicWriteBytes(secretFile, bytes)
    }

    private fun persistPrefs() {
        atomicWrite(prefsFile, json.encodeToString(prefs))
    }

    private fun keyBytes(): ByteArray {
        if (!keyFile.exists()) {
            val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
            atomicWriteBytes(keyFile, raw)
        }
        return keyFile.readBytes()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(plain)
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes(), "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(blob, 12, blob.size - 12)
    }

    private fun atomicWrite(file: File, text: String) {
        atomicWriteBytes(file, text.encodeToByteArray())
    }

    private fun atomicWriteBytes(file: File, bytes: ByteArray) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeBytes(bytes)
        restrict(tmp)
        if (!tmp.renameTo(file)) {
            file.writeBytes(bytes)
            tmp.delete()
        }
        restrict(file)
    }

    private fun restrict(file: File) {
        runCatching {
            Files.setPosixFilePermissions(
                file.toPath(),
                if (file.isDirectory) {
                    setOf(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.OWNER_EXECUTE,
                    )
                } else {
                    setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
                },
            )
        }
    }
}

@Serializable
private data class SecretBlob(
    val cookie: String? = null,
    val csrf: String? = null,
    val remember: Boolean = false,
    val user: String = "",
    val pass: String = "",
)

@Serializable
private data class PrefsBlob(
    val dark: Boolean = true,
    val lotus: Boolean = true,
    val deskGender: String = "Both",
    val deskSeniority: String = "Both",
    val forceOffline: Boolean = false,
    val lastSync: String? = null,
    val accountJson: String? = null,
    val centreOps: CentreOpsPrefs = CentreOpsPrefs(),
    val checkIns: Map<Int, CheckInRecord> = emptyMap(),
    val callLog: Map<Int, CallRecord> = emptyMap(),
)

@Serializable
data class CachedApplicant(
    val id: Int,
    val courseId: Int,
    val payload: String,
)

@Serializable
data class OutboxRow(
    val rowId: Long,
    val applicantId: Int,
    val status: String,
    val comment: String,
    val state: String,
    val message: String? = null,
)
