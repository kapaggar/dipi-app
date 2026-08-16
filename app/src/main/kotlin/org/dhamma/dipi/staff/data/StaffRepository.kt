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
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.OutboxReconciler
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.model.RoomAllocSync
import org.dhamma.dipi.staff.model.RoomPostOutcome
import org.dhamma.dipi.staff.model.RoomSyncResult
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.StatusWrite
import org.dhamma.dipi.staff.model.UserCentreMap
import org.dhamma.dipi.staff.network.AccoHandlerParser
import org.dhamma.dipi.staff.network.ApplicantDto
import org.dhamma.dipi.staff.network.CentrePageParser
import org.dhamma.dipi.staff.network.CropDto
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.LoginBody
import org.dhamma.dipi.staff.network.PhotoUploadBody
import org.dhamma.dipi.staff.network.SearchPageParser
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.dhamma.dipi.staff.network.html
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
    @Volatile private var lastStatuses: List<String> = emptyList()

    /**
     * Session-scoped ID + health disclosures by applicant id — in memory
     * ONLY (owner-approved display amendment, 2026-08-16). Never written to
     * Room/DataStore/logs. Replaced when an unfiltered worklist fetch
     * replaces the course cache; wiped on logout and erase-all.
     */
    private val sensitive = java.util.concurrent.ConcurrentHashMap<Int, SensitiveInfo>()

    fun sensitiveSnapshot(): Map<ApplicantId, SensitiveInfo> =
        sensitive.entries.associate { (id, info) -> ApplicantId(id) to info }

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
            if (useMock) {
                val dto = auth.login(LoginBody(username, password))
                if (dto.sessid.isNotBlank() && dto.session_name.isNotBlank()) {
                    tokens.saveSession("${dto.session_name}=${dto.sessid}", dto.token.ifBlank { null })
                }
                val mapped = UserCentreMap.name(username)
                val base = api.session().toModel()
                val session = base.copy(
                    name = username,
                    displayName = username,
                    centres = base.centres.map { it.copy(name = mapped) },
                )
                sessionStore.setAccountJson(json.encodeToString(sessionLite(session)))
                return@runCatching session
            }
            cookies.clear()
            tokens.saveSession(null, null)
            // /user/login is 200; GET / and /centre are 403 and omit the form
            // when a leftover authenticated cookie is still attached.
            var rootHtml = api.userLogin().html()
            var block = SearchPageParser.loginBlock(rootHtml)
            if (block == null) {
                rootHtml = api.siteRoot().html()
                block = SearchPageParser.loginBlock(rootHtml)
            }
            if (block == null) {
                rootHtml = api.centreLanding().html()
                block = SearchPageParser.loginBlock(rootHtml)
            }
            val login = block ?: throw ApiException("Could not read the desk login form")
            val after = api.submitLogin(
                action = login.action,
                name = username,
                pass = password,
                formBuildId = login.formBuildId,
                formId = login.formId,
            )
            val html = after.html()
            if (stillOnLogin(html)) {
                throw ApiException(SearchPageParser.loginError(html) ?: "Login failed")
            }
            val session = sessionFromDeskHtml(html, after.raw().request.url.encodedPath, username)
            sessionStore.setAccountJson(json.encodeToString(sessionLite(session)))
            session
        }.getOrElse { throw it.toApi() }
    }

    /** Touch the Drupal session so the SESS cookie does not expire on a quiet desk. */
    suspend fun keepAlive() {
        if (useMock) {
            val token = runCatching { auth.csrfToken().string() }.getOrNull()
            if (!token.isNullOrBlank()) tokens.saveSession(tokens.sessionCookie(), token)
            return
        }
        val token = runCatching { auth.csrfToken().string() }.getOrNull()
        if (!token.isNullOrBlank()) tokens.saveSession(tokens.sessionCookie(), token)
        val dash = api.centreLanding()
        val html = dash.html()
        if (stillOnLogin(html) || dash.code() == 403) {
            throw ApiException("Session expired", unauthorized = true)
        }
    }

    suspend fun restoreSession(): Session? {
        if (tokens.sessionCookie().isNullOrBlank()) return null
        return runCatching {
            if (useMock) api.session().toModel() else {
                val dash = api.centreLanding()
                val html = dash.html()
                if (stillOnLogin(html) || dash.code() == 403) throw ApiException("Access denied", unauthorized = true)
                sessionFromDeskHtml(html, dash.raw().request.url.encodedPath, "")
            }
        }.getOrElse {
            val ex = it.toApi()
            if (ex.unauthorized) {
                sessionExpired()
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
            val list = api.courses(centreId.value, upcoming = 1).items.map { it.toModel() }
            refreshRooms(centreId.value)
            return@runCatching list
        }
        val dash = api.centreDashboard(centreId.value)
        val html = dash.html()
        if (stillOnLogin(html) || dash.code() == 403) throw ApiException("Access denied", unauthorized = true)
        val summaries = CentrePageParser.courseSummaries(html)
        refreshRooms(centreId.value)
        SearchPageParser.coursesFromDashboard(html).map {
            Course(CourseId(it.id), centreId, it.label, "", "", summary = summaries[it.id])
        }
    }.getOrElse { throw it.toApi() }

    /**
     * Server room config, read-only: `GET /centre/{cid}/acco-handler` — the
     * DataTables source behind the desk's Centre-settings Accommodation table.
     * Refreshed on every centre-page load (login and centre pick) and cached
     * in [CentreOpsPrefs.rooms]; any failure or non-Editor body (offline,
     * expired session) keeps the last fetch, so Rooms stays offline-first.
     */
    private suspend fun refreshRooms(centreId: Int) {
        runCatching {
            val resp = api.accoHandler(centreId)
            if (!resp.isSuccessful) return
            val rooms = AccoHandlerParser.roomsOrNull(resp.html()) ?: return
            val cur = sessionStore.centreOpsOnce()
            if (cur.rooms != rooms) sessionStore.setCentreOps(cur.copy(rooms = rooms))
        }
    }

    suspend fun loadStatuses(): List<String> = runCatching {
        if (useMock) return@runCatching api.statuses().items.map { it.value }
        lastStatuses
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
            val resp = api.searchCourse(
                centreId = cid,
                courseId = courseId.value,
                status = status.orEmpty(),
                old = "",
                gender = "",
                db = "a",
            )
            val html = resp.html()
            if (stillOnLogin(html) || (resp.code() == 403 && !html.contains("var dataset"))) {
                throw ApiException("Access denied", unauthorized = true)
            }
            val result = SearchPageParser.parse(html, cid, baseUrl)
            val rows = result.dataset
            // Unfiltered fetch = the worklist is being replaced → drop stale
            // sensitive entries; filtered fetches only refresh their subset.
            if (status.isNullOrBlank() && q.isNullOrBlank()) sensitive.clear()
            sensitive.putAll(result.sensitive)
            persist(rows)
            sessionStore.setLastSync(Instant.now().toString())
            val counts = linkedMapOf("All" to rows.size)
            rows.groupingBy { it.status }.eachCount().forEach { (k, v) ->
                if (k.isNotBlank()) counts[k] = v
            }
            if (counts.keys.size > 1) lastStatuses = counts.keys.filter { it != "All" }
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

    /**
     * Every applicant row in the Room cache, across all courses opened on
     * this device — the in-app Advanced Search's corpus. Read-only; no
     * fetch, no NPI (the cache never holds any).
     */
    suspend fun cachedApplicants(): List<ApplicantCard> =
        applicants.listAll().map {
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
                if (useMock) {
                    api.changeStatus(row.applicantId, row.status, 0, row.comment).toModel()
                } else {
                    api.changeStatusGet(row.applicantId, row.status, 0, row.comment).toModel()
                }
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

    /**
     * One applicant's room allocation to the desk's own update endpoint —
     * `POST /app-update-attended/{id}` (owner amendment 2026-08-16). Sends
     * exactly the dialog's fields via [RoomAllocSync.params]: room section/
     * no/group/seating; the desk-side laundry/valuable token numbers, cell
     * and comment are not tracked here and post empty. Never a status,
     * never NPI. Outcomes map 401/403 → [RoomPostOutcome.AuthExpired] and
     * IO errors → [RoomPostOutcome.Offline] so the bulk walk can stop.
     */
    suspend fun syncRoomAllocation(id: ApplicantId, record: CheckInRecord): RoomPostOutcome {
        return runCatching { api.updateAttended(id.value, RoomAllocSync.params(record)) }.fold(
            onSuccess = { dto ->
                if (dto.status) RoomPostOutcome.Ok else RoomPostOutcome.Rejected(dto.msg.ifBlank { "Update failed" })
            },
            onFailure = { e ->
                val apiEx = e.toApi()
                when {
                    apiEx.unauthorized -> RoomPostOutcome.AuthExpired
                    e is java.io.IOException -> RoomPostOutcome.Offline
                    else -> RoomPostOutcome.Rejected(apiEx.message ?: "Update failed")
                }
            },
        )
    }

    /**
     * User-initiated bulk sync: walks every unsynced checked-in record with
     * a room, posts each, marks successes in [SessionStore] as they land
     * (partial runs keep their progress), collects per-row refusals, and
     * stops early on auth loss (throws unauthorized → sign-in) or on a
     * connectivity drop (partial result; the rest stays queued).
     */
    suspend fun syncRoomAllocations(records: Map<ApplicantId, CheckInRecord>): RoomSyncResult {
        val result = RoomAllocSync.walk(
            pending = RoomAllocSync.pending(records),
            post = { id, record -> syncRoomAllocation(id, record) },
            markSynced = { id, record -> markRoomSynced(id, record) },
        )
        if (result.authExpired) throw ApiException("Session expired", unauthorized = true)
        return result
    }

    private suspend fun markRoomSynced(id: ApplicantId, sent: CheckInRecord) {
        val all = sessionStore.checkInsOnce()
        val cur = all[id.value] ?: return
        // Edited while the post was in flight → the edit already re-queued it.
        if (cur.copy(synced = sent.synced, syncedAt = sent.syncedAt) != sent) return
        sessionStore.setCheckIns(
            all + (id.value to cur.copy(synced = true, syncedAt = Instant.now().toString())),
        )
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
        if (useMock) runCatching { auth.logout() } else runCatching { api.logoutGet() }
        cookies.clear()
        applicants.clear()
        outbox.clear()
        sessionStore.clear()
        sensitive.clear()
        lastCentreId = null
    }

    /**
     * Auth expiry mid-session (403): drop the dead cookies/CSRF and the
     * session-scoped sensitive map so the next screen is Sign in — but keep
     * the applicant cache, the queued outbox, and every check-in record
     * (including the room-sync walk's partial progress). A routine session
     * timeout must not destroy desk work; only an explicit Logout or
     * "Erase all local data" wipes.
     */
    suspend fun sessionExpired() {
        cookies.clear()
        tokens.saveSession(null, null)
        sensitive.clear()
        lastCentreId = null
    }

    suspend fun factoryReset() {
        if (useMock) runCatching { auth.logout() } else runCatching { api.logoutGet() }
        cookies.clear()
        applicants.clear()
        outbox.clear()
        sessionStore.wipeAll()
        sensitive.clear()
        lastCentreId = null
    }

    private fun stillOnLogin(html: String): Boolean =
        html.contains("name=\"pass\"") &&
            (html.contains("user_login_block") || html.contains("name=\"form_id\"") && html.contains("user_login"))

    private suspend fun sessionFromDeskHtml(html: String, path: String, username: String): Session {
        var body = html
        var pathNow = path
        if (!body.contains("table-heading") && !body.contains("/course/")) {
            val dash = api.centreLanding()
            body = dash.html()
            pathNow = dash.raw().request.url.encodedPath
            if (stillOnLogin(body) || dash.code() == 403) {
                throw ApiException("Access denied", unauthorized = true)
            }
        }
        val mapped = SearchPageParser.selectOptions(body, "edit-centre")
        val cid = SearchPageParser.centreIdFromPath(pathNow)
            ?: Regex("""/course/(\d+)/""").find(body)?.groupValues?.get(1)?.toIntOrNull()
            ?: mapped.firstOrNull()?.id
            ?: throw ApiException("Could not read your centre from /centre")
        lastCentreId = cid
        val name = SearchPageParser.centreName(body)
            ?: mapped.firstOrNull { it.id == cid }?.label
            ?: "Centre $cid"
        val centres = if (mapped.isNotEmpty()) {
            mapped.map { org.dhamma.dipi.staff.model.Centre(CentreId(it.id), it.label) }
        } else {
            listOf(org.dhamma.dipi.staff.model.Centre(CentreId(cid), name))
        }
        val user = username.ifBlank {
            sessionStore.accountJson()?.let {
                runCatching { json.decodeFromString(SessionLite.serializer(), it).name }.getOrNull()
            }.orEmpty().ifBlank { "registrar" }
        }
        return Session(
            uid = 0,
            name = user,
            displayName = user,
            centres = centres,
            modeTest = false,
        )
    }

    private suspend fun persist(rows: List<ApplicantDto>) {
        applicants.upsert(
            rows.map {
                ApplicantEntity(it.id, it.courseId, json.encodeToString(ApplicantDto.serializer(), it))
            },
        )
    }

    suspend fun markAttendedLocal(id: ApplicantId, attended: Boolean = true) {
        val row = applicants.get(id.value) ?: return
        val dto = json.decodeFromString(ApplicantDto.serializer(), row.payload)
        val next = dto.copy(attended = attended)
        applicants.upsert(
            listOf(row.copy(payload = json.encodeToString(ApplicantDto.serializer(), next))),
        )
    }

    /**
     * Local echo of an audit batch fix (e.g. stripping an honorific). The live
     * desk has no field-edit endpoint, so the correction stays on-device;
     * only the name changes, all other fields preserved.
     */
    suspend fun setGivenNameLocal(id: ApplicantId, givenName: String) {
        val row = applicants.get(id.value) ?: return
        val dto = json.decodeFromString(ApplicantDto.serializer(), row.payload)
        applicants.upsert(
            listOf(
                row.copy(
                    payload = json.encodeToString(ApplicantDto.serializer(), dto.copy(givenName = givenName)),
                ),
            ),
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
