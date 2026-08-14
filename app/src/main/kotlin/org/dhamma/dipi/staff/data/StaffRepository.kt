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
import org.dhamma.dipi.staff.network.FormTokens
import org.dhamma.dipi.staff.network.LoginBody
import org.dhamma.dipi.staff.network.PhotoUploadBody
import org.dhamma.dipi.staff.network.SearchPageParser
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named
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
    private val cookies: SessionCookieJar,
    @Named("useMock") private val useMock: Boolean,
    @Named("baseUrl") private val baseUrl: String,
) {
    @Volatile private var lastCentreId: Int? = null
    @Volatile private var lastTokens: FormTokens? = null
    @Volatile private var lastStatuses: List<String> = emptyList()
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
            if (dto.sessid.isNotBlank() && dto.session_name.isNotBlank()) {
                tokens.saveSession("${dto.session_name}=${dto.sessid}", dto.token.ifBlank { null })
            }
            val csrf = runCatching { auth.csrfToken().string() }.getOrNull()
                ?: dto.token.takeIf { it.isNotBlank() }
            if (!csrf.isNullOrBlank()) tokens.saveSession(tokens.sessionCookie(), csrf)
            val session = if (useMock) {
                api.session().toModel()
            } else {
                liveSession(dto.user.uid, dto.user.name)
            }
            sessionStore.setAccountJson(json.encodeToString(sessionLite(session)))
            session
        }.getOrElse { throw it.toApi() }
    }

    suspend fun restoreSession(): Session? {
        if (tokens.sessionCookie().isNullOrBlank()) return null
        return runCatching {
            if (useMock) api.session().toModel() else liveSession(0, "")
        }.getOrElse {
            val ex = it.toApi()
            if (ex.unauthorized) {
                logout()
                throw ex
            }
            sessionStore.accountJson()?.let { raw ->
                runCatching { json.decodeFromString(SessionLite.serializer(), raw).toModel() }.getOrNull()
            }
        }
    }

    suspend fun loadCourses(centreId: CentreId): List<Course> = runCatching {
        lastCentreId = centreId.value
        if (useMock) {
            return@runCatching api.courses(centreId.value, upcoming = 1).items.map { it.toModel() }
        }
        val fromJson = runCatching {
            api.getCourses(centreId.value).map {
                Course(org.dhamma.dipi.staff.model.CourseId(it.id), centreId, it.name, "", "")
            }
        }.getOrNull()
        if (!fromJson.isNullOrEmpty()) return@runCatching fromJson
        val page = fetchSearchForm(centreId.value)
        page.courses.map {
            Course(org.dhamma.dipi.staff.model.CourseId(it.id), centreId, it.label, "", "")
        }
    }.getOrElse { throw it.toApi() }

    suspend fun loadStatuses(): List<String> = runCatching {
        if (useMock) return@runCatching api.statuses().items.map { it.value }
        lastStatuses.ifEmpty {
            lastCentreId?.let { fetchSearchForm(it).statuses }.orEmpty()
        }
    }.getOrDefault(emptyList())

    /**
     * Stale-while-revalidate. Unfiltered fetches replace the course cache.
     * Filtered fetches return a view without wiping the full cache.
     */
    suspend fun refreshApplicants(
        courseId: CourseId,
        status: String? = null,
        q: String? = null,
        centreId: CentreId? = null,
    ): Pair<List<ApplicantCard>, Map<String, Int>> {
        return runCatching {
            if (useMock) {
                val page = api.applicants(
                    courseId.value,
                    status = status?.takeIf { it.isNotBlank() },
                    q = q?.takeIf { it.isNotBlank() },
                )
                val unfiltered = status.isNullOrBlank() && q.isNullOrBlank()
                if (unfiltered) persist(page.items)
                sessionStore.setLastSync(Instant.now().toString())
                return@runCatching page.toModel().items to page.counts
            }
            val cid = centreId?.value ?: lastCentreId
                ?: throw ApiException("No centre on the session")
            lastCentreId = cid
            val form = fetchSearchForm(cid)
            val tokens = form.tokens ?: throw ApiException("Could not read the desk search form")
            lastTokens = tokens
            val fields = linkedMapOf(
                "form_build_id" to tokens.formBuildId,
                "form_token" to tokens.formToken,
                "form_id" to tokens.formId,
                "course" to courseId.value.toString(),
                "type" to "Both",
                "op" to "Search",
            )
            val posted = api.searchAppSubmit(cid, fields)
            val html = posted.body()?.string().orEmpty()
            if (html.contains("Access denied", ignoreCase = true)) {
                throw ApiException("Access denied")
            }
            val result = SearchPageParser.parse(html, cid, baseUrl)
            val rows = result.dataset.ifEmpty { form.dataset }.filter {
                it.courseId == 0 || it.courseId == courseId.value
            }
            persist(rows)
            sessionStore.setLastSync(Instant.now().toString())
            val counts = linkedMapOf("All" to rows.size)
            rows.groupingBy { it.status }.eachCount().forEach { (k, v) ->
                if (k.isNotBlank()) counts[k] = v
            }
            rows.map { it.toModel() } to counts
        }.getOrElse { throw it.toApi() }
    }

    suspend fun loadCard(id: ApplicantId): ApplicantCard = runCatching {
        if (!useMock) {
            return@runCatching cachedCard(id) ?: throw ApiException("Applicant is not in the cached worklist")
        }
        val dto = api.applicant(id.value)
        persist(listOf(dto))
        dto.toModel()
    }.getOrElse { throw it.toApi() }

    suspend fun cachedCard(id: ApplicantId): ApplicantCard? =
        applicants.get(id.value)?.let {
            json.decodeFromString(ApplicantDto.serializer(), it.payload).toModel()
        }

    suspend fun photoReview(courseId: CourseId): List<org.dhamma.dipi.staff.model.PhotoReviewItem> = runCatching {
        if (useMock) return@runCatching api.photoReview(courseId.value).items.map { it.toModel() }
        emptyList()
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
        if (!useMock) return 0 to "Photo upload is not exposed on the live desk"
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
        cookies.clear()
        applicants.clear()
        outbox.clear()
        sessionStore.clear()
        lastCentreId = null
        lastTokens = null
    }

    private suspend fun liveSession(uid: Int, name: String): Session {
        val landing = api.searchAppLanding()
        val html = landing.body()?.string().orEmpty()
        if (html.contains("Access denied", ignoreCase = true) || landing.code() == 403) {
            throw ApiException("Access denied")
        }
        val pathCid = SearchPageParser.centreIdFromPath(landing.raw().request.url.encodedPath)
        val page = SearchPageParser.parse(html, pathCid, baseUrl)
        lastTokens = page.tokens
        lastStatuses = page.statuses
        val centres = page.centres.ifEmpty {
            pathCid?.let {
                listOf(org.dhamma.dipi.staff.network.SelectOption(it, "Centre $it"))
            }.orEmpty()
        }
        if (centres.isEmpty()) {
            throw ApiException("Could not read your centre from the desk. Check that this account can open /search-app.")
        }
        lastCentreId = centres.first().id
        val display = name.ifBlank {
            SearchPageParser.stripTags(
                Regex("""<title>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1).orEmpty(),
            ).ifBlank { "registrar" }
        }
        return Session(
            uid = uid,
            name = display,
            displayName = display,
            centres = centres.map {
                org.dhamma.dipi.staff.model.Centre(
                    org.dhamma.dipi.staff.model.CentreId(it.id),
                    it.label,
                )
            },
            modeTest = false,
        )
    }

    private suspend fun fetchSearchForm(centreId: Int) : org.dhamma.dipi.staff.network.SearchPage {
        val resp = api.searchAppForm(centreId)
        val html = resp.body()?.string().orEmpty()
        if (html.contains("Access denied", ignoreCase = true) || resp.code() == 403) {
            throw ApiException("Access denied")
        }
        val page = SearchPageParser.parse(html, centreId, baseUrl)
        lastTokens = page.tokens ?: lastTokens
        if (page.statuses.isNotEmpty()) lastStatuses = page.statuses
        return page
    }

    private suspend fun persist(rows: List<ApplicantDto>) {
        applicants.upsert(
            rows.map {
                ApplicantEntity(it.id, it.courseId, json.encodeToString(ApplicantDto.serializer(), it))
            },
        )
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
