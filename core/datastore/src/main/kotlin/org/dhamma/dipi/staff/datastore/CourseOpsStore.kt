package org.dhamma.dipi.staff.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.model.ApplicationCard
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.HealthRow
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.RollGroup
import org.dhamma.dipi.staff.model.RollRow
import org.dhamma.dipi.staff.model.RollSeniority
import org.dhamma.dipi.staff.model.SeatKind
import org.dhamma.dipi.staff.model.TeacherRoll
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
 *
 * Course cache (spec 2d, owner amendment 2026-09-02): the same encrypted
 * file also holds the running course's roll snapshot and per-applicant
 * application cards (health answers included) so the hall reads offline
 * across restarts. Keyed by course id; auto-wiped whenever the stored
 * course id differs, on [wipeCourse] (logout), and on [wipeAll] (Erase-all).
 * The `@Serializable` snapshot DTOs are PRIVATE to this file — nothing
 * course-ops touches Room or plain DataStore, and no DTO leaks health text
 * through `toString()`.
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

    /** Erase-all: the whole file goes — PIN and course cache alike. */
    fun wipeAll() {
        prefs.edit().clear().commit()
    }

    /* ── Course cache (spec 2d, owner amendment 2026-09-02) ─────────── */

    /**
     * Logout / course teardown: drop the roll snapshot and every cached
     * application card, keep the device PIN (logout deliberately leaves it).
     */
    fun wipeCourse() {
        val editor = prefs.edit()
        prefs.all.keys
            .filter { it == COURSE_ID || it == COURSE_ROLL || it.startsWith(CARD_PREFIX) }
            .forEach { editor.remove(it) }
        editor.commit()
    }

    /** Persist the (id-mapped) roll for [courseId], wiping a different course's cache first. */
    fun saveRoll(courseId: Int, roll: TeacherRoll) {
        ensureCourse(courseId)
        prefs.edit()
            .putString(COURSE_ROLL, json.encodeToString(RollDto.serializer(), roll.toDto()))
            .commit()
    }

    /** The cached roll — null when nothing is stored or a different course is. */
    fun loadRoll(courseId: Int): TeacherRoll? {
        if (storedCourseId() != courseId) return null
        val raw = prefs.getString(COURSE_ROLL, null) ?: return null
        return runCatching { json.decodeFromString(RollDto.serializer(), raw).toModel() }.getOrNull()
    }

    /** Write-behind for one prefetched application card. */
    fun saveCard(courseId: Int, applicantId: Int, card: ApplicationCard) {
        ensureCourse(courseId)
        prefs.edit()
            .putString("$CARD_PREFIX$applicantId", json.encodeToString(CardDto.serializer(), card.toDto()))
            .commit()
    }

    /** Every cached card for [courseId], by applicant id — empty for any other course. */
    fun loadCards(courseId: Int): Map<Int, ApplicationCard> {
        if (storedCourseId() != courseId) return emptyMap()
        return prefs.all.entries
            .filter { it.key.startsWith(CARD_PREFIX) }
            .mapNotNull { (key, value) ->
                val id = key.removePrefix(CARD_PREFIX).toIntOrNull() ?: return@mapNotNull null
                val card = runCatching {
                    json.decodeFromString(CardDto.serializer(), value.toString()).toModel()
                }.getOrNull() ?: return@mapNotNull null
                id to card
            }
            .toMap()
    }

    private fun storedCourseId(): Int? =
        prefs.getInt(COURSE_ID, -1).takeIf { it >= 0 }

    /** The auto-wipe: a different resolved course drops the previous course's data. */
    private fun ensureCourse(courseId: Int) {
        if (storedCourseId() != courseId) {
            wipeCourse()
            prefs.edit().putInt(COURSE_ID, courseId).commit()
        }
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
        const val COURSE_ID = "course_id"
        const val COURSE_ROLL = "course_roll"
        const val CARD_PREFIX = "card_"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}

/*
 * The encrypted snapshot DTOs — PRIVATE to this file by design (spec 2d S1):
 * the in-memory models stay non-serializable so nothing else in the app can
 * accidentally persist them. Health text lives in [CardDto]/[KvDto]; both
 * redact `toString()` so a log line can never carry an answer (the JSON
 * encoding above is separate from toString and lands only in the encrypted
 * file).
 */

@Serializable
private data class KvDto(val k: String, val v: String) {
    override fun toString(): String = "KvDto(k=$k, v=██)"
}

@Serializable
private data class CountDto(val k: String, val n: Int)

@Serializable
private data class CardDto(
    val name: String,
    val conf: String? = null,
    val statusLine: String = "",
    val hasPhoto: Boolean = false,
    val personal: List<KvDto> = emptyList(),
    val counts: List<CountDto> = emptyList(),
    val firstCourse: String = "",
    val lastCourse: String = "",
    val practiceDetails: String = "",
    val health: List<KvDto> = emptyList(),
) {
    override fun toString(): String = "CardDto(name=$name, conf=$conf, health.keys=${health.map { it.k }})"

    fun toModel() = ApplicationCard(
        name = name,
        conf = conf,
        statusLine = statusLine,
        hasPhoto = hasPhoto,
        personal = personal.map { it.k to it.v },
        historyCounts = counts.map { it.k to it.n },
        firstCourse = firstCourse,
        lastCourse = lastCourse,
        practiceDetails = practiceDetails,
        health = health.map { HealthRow(it.k, it.v) },
    )
}

private fun ApplicationCard.toDto() = CardDto(
    name = name,
    conf = conf,
    statusLine = statusLine,
    hasPhoto = hasPhoto,
    personal = personal.map { (k, v) -> KvDto(k, v) },
    counts = historyCounts.map { (k, n) -> CountDto(k, n) },
    firstCourse = firstCourse,
    lastCourse = lastCourse,
    practiceDetails = practiceDetails,
    health = health.map { KvDto(it.label, it.answer) },
)

@Serializable
private data class RollDto(val groups: List<GroupDto>) {
    fun toModel() = TeacherRoll(groups.map { it.toModel() })
}

@Serializable
private data class GroupDto(
    val at: String,
    val code: String? = null,
    val gender: Gender,
    val seniority: RollSeniority,
    val group: String,
    val total: Int,
    val rows: List<RowDto> = emptyList(),
) {
    fun toModel() = RollGroup(
        at = at, code = code, gender = gender, seniority = seniority,
        group = group, total = total, rows = rows.map { it.toModel() },
    )
}

@Serializable
private data class RowDto(
    val sn: Int,
    val applicantId: Int? = null,
    val name: String,
    val roleTag: String? = null,
    val room: String,
    val age: String,
    val city: String,
    val courses: List<CountDto> = emptyList(),
    val cell: String,
    val seat: String,
    val seatKind: SeatKind,
    val backrest: Boolean,
    val occupation: String,
    val education: String,
    val languages: String,
) {
    fun toModel() = RollRow(
        sn = sn,
        applicantId = applicantId?.let { ApplicantId(it) },
        name = name,
        roleTag = roleTag,
        room = room,
        age = age,
        city = city,
        courses = courses.map { it.k to it.n },
        cell = cell,
        seat = seat,
        seatKind = seatKind,
        backrest = backrest,
        occupation = occupation,
        education = education,
        languages = languages,
    )
}

private fun TeacherRoll.toDto() = RollDto(
    groups.map { g ->
        GroupDto(
            at = g.at, code = g.code, gender = g.gender, seniority = g.seniority,
            group = g.group, total = g.total,
            rows = g.rows.map { r ->
                RowDto(
                    sn = r.sn,
                    applicantId = r.applicantId?.value,
                    name = r.name,
                    roleTag = r.roleTag,
                    room = r.room,
                    age = r.age,
                    city = r.city,
                    courses = r.courses.map { (k, n) -> CountDto(k, n) },
                    cell = r.cell,
                    seat = r.seat,
                    seatKind = r.seatKind,
                    backrest = r.backrest,
                    occupation = r.occupation,
                    education = r.education,
                    languages = r.languages,
                )
            },
        )
    },
)
