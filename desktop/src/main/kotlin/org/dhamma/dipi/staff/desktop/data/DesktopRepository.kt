package org.dhamma.dipi.staff.desktop.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dhamma.dipi.staff.audit.ClientAudit
import org.dhamma.dipi.staff.desktop.DesktopConfig
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreCourses
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.OutboxReconciler
import org.dhamma.dipi.staff.model.RoomAllocSync
import org.dhamma.dipi.staff.model.RoomPostOutcome
import org.dhamma.dipi.staff.model.RoomSyncResult
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.model.StatusWrite
import org.dhamma.dipi.staff.model.UserCentreMap
import org.dhamma.dipi.staff.network.AccoHandlerParser
import org.dhamma.dipi.staff.network.ApplicantDto
import org.dhamma.dipi.staff.network.AttendedTableParser
import org.dhamma.dipi.staff.network.CentrePageParser
import org.dhamma.dipi.staff.network.DrupalAuthApi
import org.dhamma.dipi.staff.network.LoginBody
import org.dhamma.dipi.staff.network.SearchPageParser
import org.dhamma.dipi.staff.network.SessionCookieJar
import org.dhamma.dipi.staff.network.SheetTransport
import org.dhamma.dipi.staff.network.StaffApi
import org.dhamma.dipi.staff.network.TokenStore
import org.dhamma.dipi.staff.network.html
import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

/** Live-desk repository for Linux. Same Drupal HTML protocol as the Android client. */
class DesktopRepository(
    private val auth: DrupalAuthApi,
    private val api: StaffApi,
    private val tokens: TokenStore,
    private val store: DesktopStore,
    private val json: Json,
    private val cookies: SessionCookieJar,
    private val useMock: Boolean,
    private val baseUrl: String,
    sheetsDir: java.io.File,
) {
    constructor(config: DesktopConfig, store: DesktopStore, clients: DesktopClients) : this(
        auth = clients.auth,
        api = clients.api,
        tokens = store,
        store = store,
        json = clients.json,
        cookies = clients.cookies,
        useMock = config.useMock,
        baseUrl = config.baseUrl,
        sheetsDir = config.sheetsDir,
    )

    @Volatile private var lastCentreId: Int? = null
    @Volatile private var lastStatuses: List<String> = emptyList()
    private val rowIds = AtomicLong(1)
    private val worklist = MutableStateFlow(store.loadWorklist())
    private val outboxFlow = MutableStateFlow(store.loadOutbox())
    private val sensitive = java.util.concurrent.ConcurrentHashMap<Int, SensitiveInfo>()
    private val sheets = SheetTransport(api, baseUrl) { sheetsDir }

    init {
        sheets.wipe()
    }

    fun sensitiveSnapshot(): Map<ApplicantId, SensitiveInfo> =
        sensitive.entries.associate { (id, info) -> ApplicantId(id) to info }

    fun observeApplicants(courseId: CourseId): Flow<List<ApplicantCard>> =
        worklist.map { rows ->
            val cards = rows.filter { it.courseId == courseId.value }.map { decode(it) }
            cards.map { card ->
                card.copy(flags = ClientAudit.merge(ClientAudit.evaluate(card, cards), card.flags))
            }
        }

    fun observeOutbox(): Flow<List<OutboxRow>> = outboxFlow

    suspend fun login(username: String, password: String): Session = runCatching {
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
            store.setAccountJson(json.encodeToString(sessionLite(session)))
            return@runCatching session
        }
        cookies.clear()
        tokens.saveSession(null, null)
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
        store.setAccountJson(json.encodeToString(sessionLite(session)))
        session
    }.getOrElse { throw it.toApi() }

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
            if (useMock) {
                api.session().toModel()
            } else {
                val dash = api.centreLanding()
                val html = dash.html()
                if (stillOnLogin(html) || dash.code() == 403) {
                    throw ApiException("Access denied", unauthorized = true)
                }
                sessionFromDeskHtml(html, dash.raw().request.url.encodedPath, "")
            }
        }.getOrElse {
            val ex = it.toApi()
            if (ex.unauthorized) {
                sessionExpired()
                throw ex
            }
            store.accountJson()?.let { raw ->
                runCatching { json.decodeFromString(SessionLite.serializer(), raw).toModel() }.getOrNull()
            }
        }
    }

    suspend fun loadCourses(centreId: CentreId): CentreCourses = runCatching {
        lastCentreId = centreId.value
        if (useMock) {
            refreshRooms(centreId.value)
            return@runCatching CentreCourses(
                upcoming = api.courses(centreId.value, upcoming = 1).items.map { it.toModel() },
                older = api.courses(centreId.value, upcoming = 0).items.map { it.toModel() },
            )
        }
        val dash = api.centreDashboard(centreId.value)
        val html = dash.html()
        if (stillOnLogin(html) || dash.code() == 403) throw ApiException("Access denied", unauthorized = true)
        val summaries = CentrePageParser.courseSummaries(html)
        refreshRooms(centreId.value)
        val upcomingOpts = SearchPageParser.coursesFromDashboard(html)
        val upcomingIds = upcomingOpts.map { it.id }.toSet()
        val upcoming = upcomingOpts.map {
            Course(CourseId(it.id), centreId, it.label, "", "", summary = summaries[it.id])
        }
        val older = CentrePageParser.olderCourseOptions(html, upcomingIds).map {
            Course(CourseId(it.id), centreId, it.label, "", "")
        }
        CentreCourses(upcoming, older)
    }.getOrElse { throw it.toApi() }

    private suspend fun refreshRooms(centreId: Int) {
        runCatching {
            val resp = api.accoHandler(centreId)
            if (!resp.isSuccessful) return
            val rooms = AccoHandlerParser.roomsOrNull(resp.html()) ?: return
            val cur = store.centreOps()
            if (cur.rooms != rooms) store.setCentreOps(cur.copy(rooms = rooms))
        }
    }

    suspend fun refreshApplicants(
        courseId: CourseId,
        status: String? = null,
        q: String? = null,
        centreId: CentreId? = null,
    ): Pair<List<ApplicantCard>, Map<String, Int>> = runCatching {
        if (useMock) {
            val page = api.applicants(
                courseId.value,
                status = status?.takeIf { it.isNotBlank() },
                q = q?.takeIf { it.isNotBlank() },
            )
            val unfiltered = status.isNullOrBlank() && q.isNullOrBlank()
            if (unfiltered) persist(page.items)
            store.setLastSync(Instant.now().toString())
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
        if (status.isNullOrBlank() && q.isNullOrBlank()) sensitive.clear()
        sensitive.putAll(result.sensitive)
        persist(rows)
        store.setLastSync(Instant.now().toString())
        val counts = linkedMapOf("All" to rows.size)
        rows.groupingBy { it.status }.eachCount().forEach { (k, v) ->
            if (k.isNotBlank()) counts[k] = v
        }
        if (counts.keys.size > 1) lastStatuses = counts.keys.filter { it != "All" }
        rows.map { it.toModel() } to counts
    }.getOrElse { throw it.toApi() }

    suspend fun loadCard(id: ApplicantId): ApplicantCard =
        cachedCard(id) ?: throw ApiException("Applicant is not in the cached worklist")

    suspend fun cachedCard(id: ApplicantId): ApplicantCard? =
        worklist.value.firstOrNull { it.id == id.value }?.let { decode(it) }

    suspend fun cachedApplicants(): List<ApplicantCard> = worklist.value.map { decode(it) }

    suspend fun changeStatus(
        applicantId: ApplicantId,
        status: String,
        comment: String,
        offline: Boolean,
    ): FlushSnack {
        if (status.equals("Approved", ignoreCase = true)) {
            return FlushSnack("The desk client never sends Approved", error = true)
        }
        val params = StatusWrite.query(status, letterId = 0, comment = comment)
        echoLocal(applicantId, status, null)
        val row = OutboxRow(
            rowId = rowIds.getAndIncrement(),
            applicantId = applicantId.value,
            status = params.getValue("s"),
            comment = params.getValue("c"),
            state = "Pending",
        )
        saveOutbox(outboxFlow.value + row)
        return if (offline) {
            FlushSnack("queued: → $status", error = false)
        } else {
            flushOutbox().lastOrNull() ?: FlushSnack("Status updated", error = false)
        }
    }

    suspend fun flushOutbox(): List<FlushSnack> {
        val snacks = mutableListOf<FlushSnack>()
        for (row in outboxFlow.value.filter { it.state != "Synced" }) {
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
                patchOutbox(row.rowId) { it.copy(state = "Failed", message = apiEx.message) }
                snacks += FlushSnack(apiEx.message ?: "Failed", error = true)
                continue
            }
            val result = sent.getOrThrow()
            if (!result.ok) {
                patchOutbox(row.rowId) { it.copy(state = "Failed", message = result.msg) }
                snacks += OutboxReconciler.snack(row.status, result, null)
                continue
            }
            patchOutbox(row.rowId) { it.copy(state = "Synced", message = null) }
            val server = runCatching { loadCard(ApplicantId(row.applicantId)) }.getOrNull()
            if (server == null && result.confNo != null) {
                echoLocal(ApplicantId(row.applicantId), result.newStatus ?: row.status, result.confNo)
            }
            snacks += OutboxReconciler.snack(row.status, result, server?.status?.value)
        }
        return snacks
    }

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

    suspend fun syncRoomAllocations(records: Map<ApplicantId, CheckInRecord>): RoomSyncResult {
        val result = RoomAllocSync.walk(
            pending = RoomAllocSync.pending(records),
            post = { id, record -> syncRoomAllocation(id, record) },
            markSynced = { id, record -> markRoomSynced(id, record) },
        )
        if (result.authExpired) throw ApiException("Session expired", unauthorized = true)
        return result
    }

    suspend fun pullRoomAllocations(centreId: Int, courseId: Int): Map<ApplicantId, CheckInRecord> =
        runCatching {
            val resp = api.sheetPage("zero-day", centreId, courseId)
            val html = resp.html()
            if (stillOnLogin(html) || (resp.code() == 403 && !html.contains("table-attending"))) {
                throw ApiException("Access denied", unauthorized = true)
            }
            AttendedTableParser.parse(html)
        }.getOrElse { throw it.toApi() }

    suspend fun fetchSheet(export: SheetExport, centreId: Int, courseId: Int): SheetPayload =
        sheets.fetch(export, centreId, courseId)

    suspend fun fetchAppEditPage(id: ApplicantId): SheetPayload = sheets.appEditPage(id.value)

    private suspend fun markRoomSynced(id: ApplicantId, sent: CheckInRecord) {
        val all = store.checkIns()
        val cur = all[id.value] ?: return
        if (cur.copy(synced = sent.synced, syncedAt = sent.syncedAt) != sent) return
        store.setCheckIns(all + (id.value to cur.copy(synced = true, syncedAt = Instant.now().toString())))
    }

    suspend fun logout() {
        if (useMock) runCatching { auth.logout() } else runCatching { api.logoutGet() }
        cookies.clear()
        store.clearWorklist()
        store.clearOutbox()
        worklist.value = emptyList()
        outboxFlow.value = emptyList()
        store.clear()
        sensitive.clear()
        sheets.wipe()
        lastCentreId = null
    }

    suspend fun sessionExpired() {
        cookies.clear()
        tokens.saveSession(null, null)
        sensitive.clear()
        sheets.wipe()
        lastCentreId = null
    }

    suspend fun factoryReset() {
        if (useMock) runCatching { auth.logout() } else runCatching { api.logoutGet() }
        cookies.clear()
        store.wipeAll()
        worklist.value = emptyList()
        outboxFlow.value = emptyList()
        sensitive.clear()
        sheets.wipe()
        lastCentreId = null
    }

    suspend fun markAttendedLocal(id: ApplicantId, attended: Boolean = true) {
        patchCached(id) { it.copy(attended = attended) }
    }

    suspend fun setGivenNameLocal(id: ApplicantId, givenName: String) {
        patchCached(id) { it.copy(givenName = givenName) }
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
            mapped.map { Centre(CentreId(it.id), it.label) }
        } else {
            listOf(Centre(CentreId(cid), name))
        }
        val user = username.ifBlank {
            store.accountJson()?.let {
                runCatching { json.decodeFromString(SessionLite.serializer(), it).name }.getOrNull()
            }.orEmpty().ifBlank { "registrar" }
        }
        return Session(uid = 0, name = user, displayName = user, centres = centres, modeTest = false)
    }

    private fun persist(rows: List<ApplicantDto>) {
        val keep = worklist.value.filter { cached -> rows.none { it.id == cached.id } }
        val next = keep + rows.map {
            CachedApplicant(it.id, it.courseId, json.encodeToString(ApplicantDto.serializer(), it))
        }
        worklist.value = next
        store.saveWorklist(next)
    }

    private fun echoLocal(id: ApplicantId, status: String, confNo: String?) {
        patchCached(id) {
            it.copy(status = status, confNo = confNo?.takeIf { n -> n.isNotBlank() } ?: it.confNo)
        }
    }

    private fun patchCached(id: ApplicantId, block: (ApplicantDto) -> ApplicantDto) {
        val next = worklist.value.map { row ->
            if (row.id != id.value) return@map row
            val dto = block(json.decodeFromString(ApplicantDto.serializer(), row.payload))
            row.copy(payload = json.encodeToString(ApplicantDto.serializer(), dto))
        }
        worklist.value = next
        store.saveWorklist(next)
    }

    private fun decode(row: CachedApplicant): ApplicantCard =
        json.decodeFromString(ApplicantDto.serializer(), row.payload).toModel()

    private fun saveOutbox(rows: List<OutboxRow>) {
        outboxFlow.value = rows
        store.saveOutbox(rows)
    }

    private fun patchOutbox(rowId: Long, block: (OutboxRow) -> OutboxRow) {
        saveOutbox(outboxFlow.value.map { if (it.rowId == rowId) block(it) else it })
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
internal data class SessionLite(
    val uid: Int,
    val name: String,
    val displayName: String,
    val centres: List<Pair<Int, String>>,
    val modeTest: Boolean,
) {
    fun toModel() = Session(
        uid = uid,
        name = name,
        displayName = displayName,
        centres = centres.map { Centre(CentreId(it.first), it.second) },
        modeTest = modeTest,
    )
}
