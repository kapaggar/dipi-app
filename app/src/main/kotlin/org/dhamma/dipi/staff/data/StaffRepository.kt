package org.dhamma.dipi.staff.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.audit.ClientAudit
import org.dhamma.dipi.staff.database.ApplicantDao
import org.dhamma.dipi.staff.database.ApplicantEntity
import org.dhamma.dipi.staff.database.OutboxDao
import org.dhamma.dipi.staff.database.OutboxEntity
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.OutboxReconciler
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.StatusWrite
import org.dhamma.dipi.staff.network.ApplicantDto
import org.dhamma.dipi.staff.network.CropDto
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.LoginBody
import org.dhamma.dipi.staff.network.PhotoUploadBody
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaffRepository @Inject constructor(
    private val auth: DrupalAuthApi,
    private val api: StaffApi,
    private val tokens: TokenStore,
    private val sessionStore: SessionStore,
    private val applicants: ApplicantDao,
    private val outbox: OutboxDao,
    private val json: Json,
) {
    fun observeApplicants(courseId: CourseId): Flow<List<ApplicantCard>> =
        applicants.observe(courseId.value).map { rows ->
            val cards = rows.map { json.decodeFromString(ApplicantDto.serializer(), it.payload).toModel() }
            cards.map { card ->
                card.copy(flags = ClientAudit.merge(ClientAudit.evaluate(card, cards), card.flags))
            }
        }

    fun observeOutbox(): Flow<List<OutboxEntity>> = outbox.observePending()

    suspend fun login(username: String, password: String): Session {
        return runCatching {
            val dto = auth.login(LoginBody(username, password))
            val cookie = "${dto.session_name}=${dto.sessid}"
            val csrf = runCatching { auth.csrfToken().string() }.getOrNull()
                ?: dto.token.takeIf { it.isNotBlank() }
            tokens.saveSession(cookie, csrf)
            val session = api.session().toModel()
            sessionStore.setAccountJson(json.encodeToString(sessionLite(session)))
            session
        }.getOrElse { throw it.toApi() }
    }

    suspend fun restoreSession(): Session? {
        if (tokens.sessionCookie().isNullOrBlank()) return null
        return runCatching { api.session().toModel() }.getOrElse {
            val ex = it.toApi()
            if (ex.unauthorized) {
                logout()
                throw ex
            }
            // Offline: last account snapshot if we have one.
            sessionStore.accountJson()?.let { raw ->
                runCatching { json.decodeFromString(SessionLite.serializer(), raw).toModel() }.getOrNull()
            }
        }
    }

    suspend fun loadCourses(centreId: CentreId): List<Course> = runCatching {
        api.courses(centreId.value, upcoming = 1).items.map { it.toModel() }
    }.getOrElse { throw it.toApi() }

    suspend fun loadStatuses(): List<String> = runCatching {
        api.statuses().items.map { it.value }
    }.getOrDefault(emptyList())

    /**
     * Stale-while-revalidate. Unfiltered fetches replace the course cache.
     * Filtered fetches return a view without wiping the full cache.
     */
    suspend fun refreshApplicants(
        courseId: CourseId,
        status: String? = null,
        q: String? = null,
    ): Pair<List<ApplicantCard>, Map<String, Int>> {
        return runCatching {
            val page = api.applicants(
                courseId.value,
                status = status?.takeIf { it.isNotBlank() },
                q = q?.takeIf { it.isNotBlank() },
            )
            val unfiltered = status.isNullOrBlank() && q.isNullOrBlank()
            if (unfiltered) {
                val entities = page.items.map {
                    ApplicantEntity(it.id, it.courseId, json.encodeToString(ApplicantDto.serializer(), it))
                }
                applicants.upsert(entities)
            }
            sessionStore.setLastSync(Instant.now().toString())
            page.toModel().items to page.counts
        }.getOrElse { throw it.toApi() }
    }

    suspend fun loadCard(id: ApplicantId): ApplicantCard = runCatching {
        val dto = api.applicant(id.value)
        applicants.upsert(
            listOf(ApplicantEntity(dto.id, dto.courseId, json.encodeToString(ApplicantDto.serializer(), dto))),
        )
        dto.toModel()
    }.getOrElse { throw it.toApi() }

    suspend fun cachedCard(id: ApplicantId): ApplicantCard? =
        applicants.get(id.value)?.let {
            json.decodeFromString(ApplicantDto.serializer(), it.payload).toModel()
        }

    suspend fun photoReview(courseId: CourseId): List<PhotoReviewItem> = runCatching {
        api.photoReview(courseId.value).items.map { it.toModel() }
    }.getOrElse { throw it.toApi() }

    suspend fun changeStatus(
        applicantId: ApplicantId,
        status: String,
        comment: String,
        offline: Boolean,
    ): FlushSnack {
        val params = StatusWrite.query(status, letterId = 0, comment = comment)
        echoLocal(applicantId, status, null)
        outbox.insert(
            OutboxEntity(
                applicantId = applicantId.value,
                status = params.getValue("s"),
                letterId = 0,
                comment = params.getValue("c"),
                state = "Pending",
                message = null,
            ),
        )
        return if (offline) {
            FlushSnack("queued: → $status", error = false)
        } else {
            flushOutbox().lastOrNull() ?: FlushSnack("Status updated", error = false)
        }
    }

    suspend fun flushOutbox(): List<FlushSnack> {
        val snacks = mutableListOf<FlushSnack>()
        for (row in outbox.pending()) {
            val sent = runCatching {
                api.changeStatus(row.applicantId, row.status, 0, row.comment).toModel()
            }
            if (sent.isFailure) {
                val e = sent.exceptionOrNull()!!
                val apiEx = e.toApi()
                if (apiEx.unauthorized) throw apiEx
                if (e is java.io.IOException) break
                outbox.updateState(row.rowId, "Failed", apiEx.message)
                snacks += FlushSnack(apiEx.message ?: "Failed", error = true)
                continue
            }
            val result = sent.getOrThrow()
            if (!result.ok) {
                outbox.updateState(row.rowId, "Failed", result.msg)
                snacks += OutboxReconciler.snack(row.status, result, null)
                continue
            }
            outbox.updateState(row.rowId, "Synced", null)
            val server = runCatching { loadCard(ApplicantId(row.applicantId)) }.getOrNull()
            if (server == null && result.confNo != null) {
                echoLocal(ApplicantId(row.applicantId), result.newStatus ?: row.status, result.confNo)
            }
            snacks += OutboxReconciler.snack(row.status, result, server?.status?.value)
        }
        return snacks
    }

    suspend fun uploadPhotos(
        edits: Map<ApplicantId, PhotoEdit>,
    ): Pair<Int, String> {
        val ready = edits.filter { it.value.done && !it.value.uploaded }
        if (ready.isEmpty()) return 0 to "No fixed, un-uploaded photos yet"
        var n = 0
        for ((id, edit) in ready) {
            runCatching {
                api.uploadPhoto(
                    id.value,
                    PhotoUploadBody(
                        rotate = edit.rotate,
                        crop = if (edit.cropped) CropDto() else null,
                    ),
                )
            }.getOrElse { throw it.toApi() }
            n += 1
        }
        return n to "✓ Uploaded $n photo(s), all other fields preserved"
    }

    suspend fun logout() {
        runCatching { auth.logout() }
        applicants.clear()
        outbox.clear()
        sessionStore.clear()
    }

    private suspend fun echoLocal(id: ApplicantId, status: String, confNo: String?) {
        val row = applicants.get(id.value) ?: return
        val dto = json.decodeFromString(ApplicantDto.serializer(), row.payload)
        val next = dto.copy(
            status = status,
            confNo = confNo?.takeIf { it.isNotBlank() } ?: dto.confNo,
        )
        applicants.upsert(
            listOf(row.copy(payload = json.encodeToString(ApplicantDto.serializer(), next))),
        )
    }

    private fun sessionLite(s: Session) = SessionLite(
        uid = s.uid,
        name = s.name,
        displayName = s.displayName,
        centres = s.centres.map { it.id.value to it.name },
        modeTest = s.modeTest,
    )
}

@kotlinx.serialization.Serializable
private data class SessionLite(
    val uid: Int,
    val name: String,
    val displayName: String,
    val centres: List<Pair<Int, String>>,
    val modeTest: Boolean,
) {
    fun toModel() = org.dhamma.dipi.staff.model.Session(
        uid = uid,
        name = name,
        displayName = displayName,
        centres = centres.map {
            org.dhamma.dipi.staff.model.Centre(
                org.dhamma.dipi.staff.model.CentreId(it.first),
                it.second,
            )
        },
        modeTest = modeTest,
    )
}
