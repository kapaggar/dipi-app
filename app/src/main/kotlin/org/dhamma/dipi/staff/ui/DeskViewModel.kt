package org.dhamma.dipi.staff.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dhamma.dipi.staff.audit.ClientAudit
import org.dhamma.dipi.staff.data.ApiException
import org.dhamma.dipi.staff.data.ConnectivityMonitor
import org.dhamma.dipi.staff.data.PhotoEditStore
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.deskCallList
import org.dhamma.dipi.staff.desk.deskCallLogged
import org.dhamma.dipi.staff.desk.deskCheckedIn
import org.dhamma.dipi.staff.desk.deskFindingCount
import org.dhamma.dipi.staff.desk.deskOccupied
import org.dhamma.dipi.staff.desk.deskRecord
import org.dhamma.dipi.staff.desk.deskRoll
import org.dhamma.dipi.staff.desk.deskHealthSnack
import org.dhamma.dipi.staff.desk.deskSaveSnack
import org.dhamma.dipi.staff.desk.stripHonorific
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantDeskHistory
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.HISTORY_ACTIVITY
import org.dhamma.dipi.staff.model.HISTORY_CLARIFICATIONS
import org.dhamma.dipi.staff.model.HISTORY_COURSES
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.model.RoomAllocSync
import org.dhamma.dipi.staff.model.RoomSyncResult
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.model.clearSyncedIfChanged
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.model.WorklistFilter
import org.dhamma.dipi.staff.network.PhotoLoader
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.Industry
import javax.inject.Inject

enum class DeskScreen { Login, Centre, CourseHub, Today, Card, Photos, Summary, Settings, DeskAction, ZeroDay, Audit, Calling, Rooms, CentreOps, Search }

data class DeskActionDest(val title: String, val route: String)

/**
 * The in-app sheet viewer overlay (Board exports / Applications edit).
 * The title shows immediately while the fetch is in flight; [html] arrives
 * when the payload resolves. HTML bodies stay in memory only — never
 * persisted, never logged (they can carry NPI).
 */
data class SheetViewUi(
    val title: String,
    val loading: Boolean = true,
    val html: SheetPayload.Html? = null,
)

fun deskBack(screen: DeskScreen, returnTo: DeskScreen?): DeskScreen = when (screen) {
    DeskScreen.Settings, DeskScreen.DeskAction -> returnTo ?: DeskScreen.Centre
    DeskScreen.Rooms -> when (returnTo) {
        DeskScreen.CentreOps -> DeskScreen.CentreOps
        else -> DeskScreen.ZeroDay
    }
    // Centre settings are global now — they can open straight from the Centre screen.
    DeskScreen.CentreOps ->
        returnTo.takeIf { it == DeskScreen.Centre || it == DeskScreen.CourseHub } ?: DeskScreen.CourseHub
    DeskScreen.ZeroDay, DeskScreen.Audit, DeskScreen.Calling ->
        returnTo.takeIf { it == DeskScreen.CourseHub } ?: DeskScreen.CourseHub
    // A card opened from the in-app Advanced Search backs to the search results.
    DeskScreen.Card -> returnTo.takeIf { it == DeskScreen.Search } ?: DeskScreen.Today
    DeskScreen.Photos, DeskScreen.Summary -> DeskScreen.Today
    DeskScreen.Today -> DeskScreen.CourseHub
    // The in-app Advanced Search opens from the Centre screen only.
    DeskScreen.Search -> DeskScreen.Centre
    DeskScreen.CourseHub -> DeskScreen.Centre
    DeskScreen.Centre, DeskScreen.Login -> screen
}

fun deskAfterLogin(): DeskScreen = DeskScreen.Centre

fun deskAfterPickCourse(): DeskScreen = DeskScreen.CourseHub

data class DeskUiState(
    val screen: DeskScreen = DeskScreen.Login,
    val username: String = "",
    val password: String = "",
    val loginError: String? = null,
    val loginLoading: Boolean = false,
    val remember: Boolean = false,
    val session: Session? = null,
    val courses: List<Course> = emptyList(),
    val olderCourses: List<Course> = emptyList(),
    val course: Course? = null,
    val rows: List<ApplicantCard> = emptyList(),
    val visible: List<ApplicantCard> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val loading: Boolean = false,
    val card: ApplicantCard? = null,
    val dark: Boolean = false,
    val skin: DeskSkin = DeskSkin.Steel,
    val lotus: Boolean = true,
    val offline: Boolean = false,
    val queuedById: Map<ApplicantId, String> = emptyMap(),
    val queuedCount: Int = 0,
    /**
     * Epoch millis of the last outbox flush attempt — reconnect or manual
     * RETRY alike. Never persisted: a fresh process shows the queued strip
     * without a last-try line until the first attempt (spec R7).
     */
    val lastSyncAttemptAt: Long? = null,
    val statusChoices: List<String> = ApplicantStatus.SHEET_CHOICES,
    val sheetOpen: Boolean = false,
    val sheetPick: String = "",
    val sheetComment: String = "",
    val sheetCustom: String = "",
    val photos: List<PhotoReviewItem> = emptyList(),
    val edits: Map<ApplicantId, PhotoEdit> = emptyMap(),
    val photoFilter: String = "All",
    val lastSync: String? = null,
    val snack: FlushSnack? = null,
    val deskAction: DeskActionDest? = null,
    val centreOps: CentreOpsPrefs = CentreOpsPrefs(),
    val auditRows: List<ApplicantCard> = emptyList(),
    val callState: Map<ApplicantId, CallRecord> = emptyMap(),
    val callFilter: String = "To call",
    /** Call-round name box. Session-only — a search is never worth restoring. */
    val callSearch: String = "",
    /** Call-round order: false is A-Z, true floats the still-to-reach rows up. */
    val callPriority: Boolean = false,
    val roomsGender: Gender? = null,
    val roomsApplicantId: ApplicantId? = null,
    val deskSection: DeskSection = DeskSection.Board,
    val checkIns: Map<ApplicantId, CheckInRecord> = emptyMap(),
    val deskScan: String = "",
    val deskZeroFilter: String = "To arrive",
    /** Zero-day desk gender filter ("Both"/"Male"/"Female"), persisted in SessionStore. */
    val deskGender: String = "Both",
    /** Zero-day desk old/new filter ("Both"/"New"/"Old"), persisted in SessionStore. */
    val deskSeniority: String = "Both",
    val deskMarkId: ApplicantId? = null,
    val deskRoomOpen: Boolean = false,
    val deskFinding: String? = null,
    val deskAppId: ApplicantId? = null,
    /**
     * Display-only ID + health disclosures by applicant, mirrored from the
     * repository's session-scoped in-memory map. Never persisted or logged.
     */
    val sensitiveById: Map<ApplicantId, SensitiveInfo> = emptyMap(),
    /**
     * Lazy desk-history fragments by applicant. Null list = not fetched yet.
     * In-memory only — never Room, never DataStore, never NPI.
     */
    val history: Map<ApplicantId, ApplicantDeskHistory> = emptyMap(),
    /** Bulk room-allocation sync in flight (owner amendment 2026-08-16). */
    val roomSyncBusy: Boolean = false,
    /** Zero-day attended-table pull in flight. */
    val roomPullBusy: Boolean = false,
    /** Last bulk sync outcome — counts + per-row failures for the UI to bind. */
    val roomSync: RoomSyncResult? = null,
    /** The in-app Advanced Search corpus: every applicant cached in Room. */
    val searchRows: List<ApplicantCard> = emptyList(),
    /** The in-app sheet viewer overlay — null when no sheet is open. */
    val sheetView: SheetViewUi? = null,
    /** One-shot: a streamed PDF/Excel to hand to the system viewer (consumed like [snack]). */
    val openDoc: SheetPayload.Document? = null,
)

/**
 * Opening a course wipes the per-course buffers. The scan buffer goes with
 * them: it is session-scoped, so a conf number typed against one course must
 * never survive into the next one (the roster would open silently filtered).
 * The tablet's own gender/seniority filters describe the desk, not the course,
 * and are deliberately kept. Applicant desk history is applicant-scoped and
 * is retained across a course switch.
 */
fun deskOpenCourse(state: DeskUiState, course: Course): DeskUiState = state.copy(
    course = course,
    screen = deskAfterPickCourse(),
    deskSection = DeskSection.Board,
    rows = emptyList(),
    visible = emptyList(),
    counts = emptyMap(),
    selected = emptySet(),
    query = "",
    card = null,
    loading = false,
    sensitiveById = emptyMap(),
    sheetView = null,
    deskScan = "",
)

/**
 * The search door onto the same swap. Advanced Search runs over the whole
 * cache, so a result can belong to a course other than the open one; the row's
 * course is adopted (when it is one of the listed upcoming/older courses) so
 * the card pane has its context.
 *
 * That is still a course swap, so the session-scoped scan buffer goes with it —
 * otherwise Check-in later opens silently filtered by the previous course's
 * conf number, exactly as it did before [deskOpenCourse].
 *
 * Only the course and the scan move. This opens a Card, not a desk session: it
 * does not cancel the worklist observer, so clearing the worklist buffers here
 * would only have them refilled by the still-live collector. Starting a session
 * is [deskOpenCourse]'s job, and it cancels that observer first.
 */
fun deskAdoptSearchCourse(state: DeskUiState, card: ApplicantCard): DeskUiState {
    val course = (state.courses + state.olderCourses).firstOrNull { it.id == card.courseId }
        ?: return state
    if (state.course?.id == course.id) return state
    return state.copy(course = course, deskScan = "")
}

/**
 * Rail counts for the v2 desk — derived from the worklist plus the local
 * check-in records, never stored, so the numbers cannot drift.
 */
fun deskRailCounts(state: DeskUiState): Map<DeskSection, Int> = buildMap {
    val roll = deskRoll(state.rows)
    val applications = state.counts["All"] ?: state.rows.size
    if (applications > 0) put(DeskSection.Applications, applications)
    put(DeskSection.Audit, deskFindingCount(state.auditRows))
    put(DeskSection.Calling, deskCallList(roll).count { !deskCallLogged(state.callState[it.id]) })
    put(DeskSection.CheckIn, roll.count { !deskCheckedIn(it, state.checkIns) })
    val occupied = deskOccupied(roll, state.checkIns)
    put(DeskSection.Rooms, state.centreOps.rooms.count { it.code !in occupied })
}

fun parseCourseStart(start: String?): java.time.LocalDate? {
    if (start.isNullOrBlank()) return null
    return listOf(
        java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
        java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
    ).firstNotNullOfOrNull { fmt ->
        runCatching { java.time.LocalDate.parse(start.trim(), fmt) }.getOrNull()
    }
}

/** "DAY 0 · TODAY" / "STARTS IN n DAYS" / "DAY n" — null when the start date is unknown. */
fun deskDayChip(start: String?, today: java.time.LocalDate): String? {
    val date = parseCourseStart(start) ?: return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> "DAY 0 · TODAY"
        days > 0L -> "STARTS IN $days DAYS"
        else -> "DAY ${-days}"
    }
}

/** Check-in records still owing the server their room allocation. */
fun deskRoomSyncPending(checkIns: Map<ApplicantId, CheckInRecord>): Int =
    RoomAllocSync.pending(checkIns).size

/** Snack for a user-initiated pull: count of non-blank rooms on the attended table. */
fun roomPullSnack(n: Int): FlushSnack =
    if (n == 0) FlushSnack("No rooms assigned on the desk yet", error = false)
    else FlushSnack("✓ Pulled $n room assignment(s) from the desk", error = false)

/** Snack for a bulk allocation sync: successes first, then the first refusal verbatim. */
fun roomSyncSnack(result: RoomSyncResult): FlushSnack = when {
    result.offline ->
        FlushSnack("${result.synced} synced · connection lost — the rest will sync when online", error = true)
    result.failures.isNotEmpty() ->
        FlushSnack("${result.synced} synced · ${result.failed} failed — ${result.failures.first().reason}", error = true)
    else -> FlushSnack("✓ Synced ${result.synced} room allocation(s) to the desk", error = false)
}

/** The rail footer's truth claim about sync state. */
fun deskSyncLine(lastSyncIso: String?, now: java.time.Instant, offline: Boolean, queued: Int): String {
    if (offline) return "offline · $queued queued"
    if (queued > 0) return "$queued queued to sync"
    val sync = lastSyncIso?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
        ?: return "not synced yet"
    val mins = java.time.temporal.ChronoUnit.MINUTES.between(sync, now)
    return when {
        mins < 1 -> "synced just now"
        mins < 60 -> "synced $mins min ago"
        else -> "synced ${mins / 60} h ago"
    }
}

@HiltViewModel
class DeskViewModel @Inject constructor(
    private val repo: StaffRepository,
    private val sessionStore: SessionStore,
    private val photoStore: PhotoEditStore,
    private val photoLoader: PhotoLoader,
    connectivity: ConnectivityMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(DeskUiState())
    val state: StateFlow<DeskUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var observeJob: Job? = null
    private var keepAliveJob: Job? = null
    private var lastOffline: Boolean? = null
    private var returnTo: DeskScreen? = null

    /** Where centre settings were opened from (Centre screen or phone course hub). */
    private var centreOpsFrom: DeskScreen = DeskScreen.CourseHub

    init {
        viewModelScope.launch {
            combine(connectivity.online, sessionStore.forceOffline) { net, force ->
                !net || force
            }.collect { offline ->
                val was = lastOffline
                lastOffline = offline
                _state.update { it.copy(offline = offline) }
                if (was == true && !offline) {
                    flush()
                }
            }
        }
        viewModelScope.launch {
            sessionStore.darkTheme.collect { dark -> _state.update { it.copy(dark = dark) } }
        }
        viewModelScope.launch {
            sessionStore.skin.collect { key ->
                val skin = DeskSkin.fromKey(key)
                Industry.apply(skin)
                _state.update { it.copy(skin = skin) }
            }
        }
        viewModelScope.launch {
            sessionStore.lotus.collect { on -> _state.update { it.copy(lotus = on) } }
        }
        viewModelScope.launch {
            sessionStore.lastSync.collect { sync -> _state.update { it.copy(lastSync = sync) } }
        }
        viewModelScope.launch {
            photoStore.edits.collect { edits -> _state.update { it.copy(edits = edits) } }
        }
        viewModelScope.launch {
            sessionStore.centreOps.collect { prefs -> _state.update { it.copy(centreOps = prefs) } }
        }
        viewModelScope.launch {
            sessionStore.checkIns.collect { records ->
                _state.update { cur ->
                    cur.copy(checkIns = records.entries.associate { (id, rec) -> ApplicantId(id) to rec })
                }
            }
        }
        viewModelScope.launch {
            sessionStore.deskGender.collect { g -> _state.update { it.copy(deskGender = g) } }
        }
        viewModelScope.launch {
            sessionStore.deskSeniority.collect { s -> _state.update { it.copy(deskSeniority = s) } }
        }
        viewModelScope.launch {
            sessionStore.callLog.collect { records ->
                _state.update { cur ->
                    cur.copy(callState = records.entries.associate { (id, rec) -> ApplicantId(id) to rec })
                }
            }
        }
        viewModelScope.launch {
            repo.observeOutbox().collect { rows ->
                val pending = rows.filter { it.state != "Synced" }
                _state.update {
                    it.copy(
                        queuedCount = pending.size,
                        queuedById = pending.associate { row ->
                            ApplicantId(row.applicantId) to row.status
                        },
                    )
                }
            }
        }
        viewModelScope.launch {
            // Reading remember-me touches EncryptedSharedPreferences; guard it
            // so a keystore that cannot be read never crashes desk startup.
            val saved = runCatching { sessionStore.remembered() }.getOrNull()
            if (saved?.on == true) {
                _state.update {
                    it.copy(username = saved.username, password = saved.password, remember = true)
                }
            }
            restore()
        }
    }

    fun onUser(v: String) { _state.update { it.copy(username = v) } }
    fun onPass(v: String) { _state.update { it.copy(password = v) } }
    fun onRemember(v: Boolean) { _state.update { it.copy(remember = v) } }

    fun signIn() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loginLoading = true, loginError = null) }
            runCatching { repo.login(s.username, s.password) }
                .onSuccess { session ->
                    sessionStore.setRemembered(s.remember, s.username, s.password)
                    afterLogin(session)
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loginLoading = false, loginError = e.message)
                    }
                }
        }
    }

    fun pickCentre(centre: Centre) {
        viewModelScope.launch {
            val session = _state.value.session ?: return@launch
            val reordered = listOf(centre) + session.centres.filter { it.id != centre.id }
            _state.update { it.copy(session = session.copy(centres = reordered), loading = true) }
            runCatching { repo.loadCourses(centre.id) }
                .onSuccess { lists ->
                    _state.update {
                        it.copy(courses = lists.upcoming, olderCourses = lists.older, loading = false)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false) }
                    handleAuth(e)
                }
        }
    }

    fun pickCourse(course: Course) {
        observeJob?.cancel()
        observeJob = null
        _state.update { deskOpenCourse(it, course) }
    }

    /** V2 desk: rail navigation between sections. No page transition, no loading state. */
    fun setDeskSection(section: DeskSection) {
        _state.update { it.copy(deskSection = section, sheetView = null) }
    }

    /** V2 desk: one fetch of the course's application set on open, then local. */
    fun ensureDesk() {
        val course = _state.value.course ?: return
        if (_state.value.rows.isEmpty()) _state.update { it.copy(loading = true) }
        ensureWorklist(course)
    }

    /* ── V2 desk · check-in ─────────────────────────────────────────── */

    fun setDeskScan(q: String) = _state.update { it.copy(deskScan = q) }
    fun setDeskZeroFilter(f: String) = _state.update { it.copy(deskZeroFilter = f) }

    /** Which desk this tablet sits on — persists across restarts via SessionStore. */
    fun setDeskGender(g: String) {
        _state.update { it.copy(deskGender = g) }
        viewModelScope.launch { sessionStore.setDeskGender(g) }
    }

    /** New / old student scope for this tablet — persists with [setDeskGender]. */
    fun setDeskSeniority(s: String) {
        _state.update { it.copy(deskSeniority = s) }
        viewModelScope.launch { sessionStore.setDeskSeniority(s) }
    }

    fun openDeskMark(card: ApplicantCard) =
        _state.update { it.copy(deskMarkId = card.id, deskRoomOpen = false) }

    fun closeDeskMark() = _state.update { it.copy(deskMarkId = null, deskRoomOpen = false) }

    fun toggleDeskRoomPicker() = _state.update { it.copy(deskRoomOpen = !it.deskRoomOpen) }

    private fun markCard(): ApplicantCard? {
        val id = _state.value.deskMarkId ?: return null
        return _state.value.rows.firstOrNull { it.id == id }
    }

    /** The dialog's working record — the effective one, or a fresh default. */
    fun deskMarkRecord(): CheckInRecord {
        val card = markCard() ?: return CheckInRecord()
        return deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
    }

    private fun patchDeskRecord(patch: (CheckInRecord) -> CheckInRecord) {
        val card = markCard() ?: return
        val cur = deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
        // Any material edit clears the record's synced flag so it re-queues.
        val next = patch(cur).clearSyncedIfChanged(cur)
        persistCheckIns(_state.value.checkIns + (card.id to next))
    }

    fun setDeskRoom(code: String) {
        patchDeskRecord { it.copy(room = code) }
        _state.update { it.copy(deskRoomOpen = false) }
    }

    fun setDeskSeat(seat: String) = patchDeskRecord { it.copy(seat = seat) }
    fun toggleDeskValuables() = patchDeskRecord { it.copy(valuables = !it.valuables) }
    fun toggleDeskLaundry() = patchDeskRecord { it.copy(laundry = !it.laundry) }
    fun setDeskGroup(group: String) = patchDeskRecord { it.copy(group = group) }

    /** Blocked with an error snackbar if no room; otherwise checks in, in place. */
    fun saveDeskMark() {
        val card = markCard() ?: return
        val record = deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
        val (text, err) = deskSaveSnack(record, card)
        if (err) {
            _state.update { it.copy(snack = FlushSnack(text, error = true)) }
            return
        }
        persistCheckIns(
            _state.value.checkIns + (card.id to record.copy(checkedIn = true).clearSyncedIfChanged(record)),
        )
        _state.update { cur ->
            val rows = cur.rows.map { if (it.id == card.id) it.copy(attended = true) else it }
            cur.copy(
                deskMarkId = null,
                deskRoomOpen = false,
                rows = rows,
                visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                snack = FlushSnack(text, error = false),
            )
        }
        viewModelScope.launch { repo.markAttendedLocal(card.id, attended = true) }
    }

    fun undoDeskMark() {
        val card = markCard() ?: return
        val record = deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
        persistCheckIns(
            _state.value.checkIns + (card.id to record.copy(checkedIn = false, room = "").clearSyncedIfChanged(record)),
        )
        _state.update { cur ->
            val rows = cur.rows.map { if (it.id == card.id) it.copy(attended = false) else it }
            cur.copy(
                deskMarkId = null,
                deskRoomOpen = false,
                rows = rows,
                visible = WorklistFilter.visible(rows, cur.selected, cur.query),
            )
        }
        viewModelScope.launch { repo.markAttendedLocal(card.id, attended = false) }
    }

    private fun persistCheckIns(records: Map<ApplicantId, CheckInRecord>) {
        _state.update { it.copy(checkIns = records) }
        viewModelScope.launch {
            sessionStore.setCheckIns(records.entries.associate { (id, rec) -> id.value to rec })
        }
    }

    fun pullRooms() {
        pullRooms(userInitiated = true)
    }

    /**
     * Pull room assignments from `GET /zero-day/{cid}/{courseId}`. Default
     * auto-pull after the worklist is silent; the Rooms / Zero Day action
     * snacks. Unsynced local rooms are never overwritten.
     */
    fun pullRooms(userInitiated: Boolean) {
        val s = _state.value
        if (s.roomPullBusy || s.roomSyncBusy) return
        val course = s.course ?: return
        if (s.offline) {
            if (userInitiated) {
                _state.update { it.copy(snack = FlushSnack("offline — will pull when online", error = false)) }
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(roomPullBusy = true) }
            runCatching { repo.pullRoomAllocations(course.centreId.value, course.id.value) }
                .onSuccess { pulled ->
                    persistCheckIns(RoomAllocSync.mergePulled(_state.value.checkIns, pulled))
                    val n = pulled.values.count { it.room.isNotBlank() }
                    _state.update {
                        it.copy(
                            roomPullBusy = false,
                            snack = if (userInitiated) roomPullSnack(n) else it.snack,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(roomPullBusy = false) }
                    if (userInitiated || (e is ApiException && e.unauthorized)) handleAuth(e)
                }
        }
    }

    /**
     * Bulk room-allocation sync (owner amendment 2026-08-16) — user-initiated,
     * walks every unsynced checked-in record. Progress lands back in
     * [DeskUiState.roomSyncBusy]/[DeskUiState.roomSync] plus the snackbar;
     * an expired session boots to sign-in via the usual auth path.
     */
    fun syncRooms() {
        val s = _state.value
        if (s.roomSyncBusy || s.roomPullBusy) return
        if (deskRoomSyncPending(s.checkIns) == 0) {
            _state.update { it.copy(snack = FlushSnack("All room allocations are synced", error = false)) }
            return
        }
        if (s.offline) {
            _state.update { it.copy(snack = FlushSnack("offline — will sync when online", error = false)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(roomSyncBusy = true) }
            runCatching { repo.syncRoomAllocations(_state.value.checkIns) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(roomSyncBusy = false, roomSync = result, snack = roomSyncSnack(result))
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(roomSyncBusy = false) }
                    handleAuth(e)
                }
        }
    }

    /* ── V2 desk · audit ────────────────────────────────────────────── */

    fun selectDeskFinding(code: String) = _state.update { it.copy(deskFinding = code) }

    /** Mechanical batch fix, applied locally; the snackbar states what was preserved. */
    fun runDeskBatch(code: String, label: String) {
        if (code != "name_title_prefix") return
        val targets = _state.value.auditRows.filter { card ->
            card.flags.any { it.ruleId == code }
        }
        viewModelScope.launch {
            targets.forEach { card ->
                val stripped = stripHonorific(card.givenName)
                if (stripped != card.givenName) repo.setGivenNameLocal(card.id, stripped)
            }
            _state.update { cur ->
                val rows = cur.rows.map { card ->
                    if (targets.any { it.id == card.id }) {
                        card.copy(givenName = stripHonorific(card.givenName))
                    } else {
                        card
                    }
                }
                cur.copy(
                    rows = rows,
                    visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                    auditRows = flagAudit(rows),
                    snack = FlushSnack("✓ $label · all other fields preserved", error = false),
                )
            }
        }
    }

    /* ── V2 desk · applications ─────────────────────────────────────── */

    /**
     * Select in the list–detail pane without leaving the desk. Selecting an
     * applicant with health disclosures fires the desk snackbar once per
     * selection — a reminder, not a gate.
     */
    fun selectDeskApp(card: ApplicantCard) {
        _state.update { cur ->
            val hasHealth = cur.sensitiveById[card.id]?.health?.isNotEmpty() == true
            cur.copy(
                deskAppId = card.id,
                card = card,
                snack = deskHealthSnack(cur.deskAppId, card.id, hasHealth) ?: cur.snack,
            )
        }
    }

    /** Informational snackbar on the desk (exports, edit — things the desk site still owns). */
    fun deskNote(text: String) {
        _state.update { it.copy(snack = FlushSnack(text, error = false)) }
    }

    /* ── V2 desk · sheets & exports ─────────────────────────────────── */

    /**
     * Test seams over the frozen repository contract: unit tests swap these
     * for fakes; production always routes through [StaffRepository].
     */
    internal var sheetFetch: suspend (SheetExport, Int, Int) -> SheetPayload =
        { export, centreId, courseId -> repo.fetchSheet(export, centreId, courseId) }
    internal var editFetch: suspend (ApplicantId) -> SheetPayload =
        { id -> repo.fetchAppEditPage(id) }
    internal var clarFetch: suspend (ApplicantId, Int) -> SheetPayload =
        { appId, clarId -> repo.fetchClarification(appId, clarId) }

    /**
     * A Board export cell: open the viewer shell immediately (its progress
     * hairline is the fetch feedback), then resolve the payload — HTML stays
     * in the viewer, a document fires the one-shot [DeskUiState.openDoc],
     * a refusal closes the viewer and shows the server's message verbatim.
     */
    fun openSheet(label: String) {
        val export = SheetExport.fromLabel(label) ?: return
        val course = _state.value.course ?: return
        _state.update { it.copy(sheetView = SheetViewUi(title = export.label)) }
        viewModelScope.launch {
            resolveSheet(export.label) { sheetFetch(export, course.centreId.value, course.id.value) }
        }
    }

    /**
     * The Centre screen's Course report chip. The export is centre-scoped —
     * `SheetRoute.ReportForm` never reads the courseId — so it works with no
     * course open; 0 is a deliberate dummy. Document payloads land in
     * `openDoc`, collected above the width gate, so this works on both sizes.
     */
    fun openCourseReport() {
        val cid = _state.value.session?.centres?.firstOrNull()?.id?.value ?: return
        val label = SheetExport.CourseReport.label
        _state.update { it.copy(sheetView = SheetViewUi(title = label)) }
        viewModelScope.launch {
            resolveSheet(label) { sheetFetch(SheetExport.CourseReport, cid, 0) }
        }
    }

    /** Applications "Edit": the desk's own edit page, display-only, in the same viewer. */
    fun openAppEdit(card: ApplicantCard) {
        val title = "Edit · ${card.displayName}"
        _state.update { it.copy(sheetView = SheetViewUi(title = title)) }
        viewModelScope.launch {
            resolveSheet(title) { editFetch(card.id) }
        }
    }

    fun expandHistory(id: ApplicantId, key: String) {
        val cur = _state.value.history[id] ?: ApplicantDeskHistory()
        val already = when (key) {
            HISTORY_COURSES -> cur.courses != null
            HISTORY_ACTIVITY -> cur.activity != null
            HISTORY_CLARIFICATIONS -> cur.clarifications != null
            else -> true
        }
        if (already) return
        patchHistory(id, cur.copy(loading = cur.loading + key, errors = cur.errors - key))
        viewModelScope.launch {
            runCatching {
                when (key) {
                    HISTORY_COURSES -> {
                        val courses = repo.loadAppCourses(id)
                        val now = _state.value.history[id] ?: cur
                        patchHistory(id, now.copy(courses = courses, loading = now.loading - key))
                    }
                    HISTORY_ACTIVITY -> {
                        val activity = repo.loadAppActivity(id)
                        val now = _state.value.history[id] ?: cur
                        patchHistory(id, now.copy(activity = activity, loading = now.loading - key))
                    }
                    HISTORY_CLARIFICATIONS -> {
                        val clarifications = repo.loadAppClarifications(id)
                        val now = _state.value.history[id] ?: cur
                        patchHistory(
                            id,
                            now.copy(clarifications = clarifications, loading = now.loading - key),
                        )
                    }
                }
            }.onFailure { e ->
                handleAuth(e)
                val now = _state.value.history[id] ?: cur
                patchHistory(
                    id,
                    now.copy(
                        loading = now.loading - key,
                        errors = now.errors + (key to (e.message ?: "Unavailable")),
                    ),
                )
            }
        }
    }

    fun openClarification(appId: ApplicantId, clarId: Int) {
        val title = "Clarification $clarId"
        _state.update { it.copy(sheetView = SheetViewUi(title = title)) }
        viewModelScope.launch {
            resolveSheet(title) { clarFetch(appId, clarId) }
        }
    }

    private fun patchHistory(id: ApplicantId, next: ApplicantDeskHistory) {
        _state.update { it.copy(history = it.history + (id to next)) }
    }

    fun closeSheet() = _state.update { it.copy(sheetView = null) }

    fun consumeOpenDoc() = _state.update { it.copy(openDoc = null) }

    private suspend fun resolveSheet(title: String, fetch: suspend () -> SheetPayload) {
        val payload = runCatching { fetch() }.getOrElse { e ->
            if (e is ApiException && e.unauthorized) {
                _state.update { it.copy(sheetView = null) }
                handleAuth(e)
                return
            }
            SheetPayload.NotAvailable(e.message ?: "$title unavailable")
        }
        _state.update { cur ->
            // The viewer was closed or replaced while fetching — drop the result.
            if (cur.sheetView?.title != title) return@update cur
            when (payload) {
                is SheetPayload.Html ->
                    cur.copy(sheetView = cur.sheetView.copy(loading = false, html = payload))
                is SheetPayload.Document ->
                    cur.copy(sheetView = null, openDoc = payload)
                is SheetPayload.NotAvailable ->
                    cur.copy(sheetView = null, snack = FlushSnack(payload.message, error = true))
            }
        }
    }

    fun openApplications() {
        val course = _state.value.course ?: return
        _state.update { it.copy(screen = DeskScreen.Today, loading = true) }
        ensureWorklist(course)
    }

    fun openLater(title: String, route: String) {
        val cur = _state.value.screen
        if (cur != DeskScreen.DeskAction) returnTo = cur
        _state.update { it.copy(screen = DeskScreen.DeskAction, deskAction = DeskActionDest(title, route)) }
    }

    /**
     * The in-app Advanced Search (owner feedback 2026-08-16): opens from the
     * Centre screen over everything already cached in Room — no fetch.
     */
    fun openAdvancedSearch() {
        returnTo = DeskScreen.Centre
        _state.update { it.copy(screen = DeskScreen.Search) }
        viewModelScope.launch {
            val cached = repo.cachedApplicants()
            _state.update { it.copy(searchRows = cached) }
        }
    }

    /** A search result opens the regular applicant card; back returns to the results. */
    fun openSearchResult(card: ApplicantCard) {
        // Adopt the row's course when it is one of the listed upcoming
        // or older courses, so the card pane has its context; otherwise leave as-is.
        _state.update { deskAdoptSearchCourse(it, card) }
        openCard(card)
    }

    fun openAudit() {
        val course = _state.value.course ?: return
        returnTo = DeskScreen.CourseHub
        _state.update { it.copy(screen = DeskScreen.Audit, loading = true) }
        ensureWorklist(course)
    }

    fun openCalling() {
        val course = _state.value.course ?: return
        returnTo = DeskScreen.CourseHub
        _state.update { it.copy(screen = DeskScreen.Calling, loading = true) }
        ensureWorklist(course)
    }

    fun openZeroDay() {
        val course = _state.value.course ?: return
        returnTo = DeskScreen.CourseHub
        _state.update { it.copy(screen = DeskScreen.ZeroDay, loading = true) }
        ensureWorklist(course)
    }

    /** Global centre settings — opens from the Centre screen or the phone course hub. */
    fun openCentreOps() {
        val cur = _state.value.screen
        if (cur == DeskScreen.Centre || cur == DeskScreen.CourseHub) centreOpsFrom = cur
        if (cur != DeskScreen.CentreOps) returnTo = centreOpsFrom
        _state.update { it.copy(screen = DeskScreen.CentreOps) }
    }

    fun openRoomsFromZeroDay(card: ApplicantCard) {
        returnTo = DeskScreen.ZeroDay
        _state.update {
            it.copy(
                screen = DeskScreen.Rooms,
                roomsGender = card.gender,
                roomsApplicantId = card.id,
            )
        }
    }

    fun openRoomsFromCentreOps() {
        returnTo = DeskScreen.CentreOps
        _state.update {
            it.copy(
                screen = DeskScreen.Rooms,
                roomsGender = null,
                roomsApplicantId = null,
            )
        }
    }

    fun pickRoom(room: AccoRoom) {
        _state.value.roomsApplicantId?.let { id -> patchRecord(id) { it.copy(room = room.code) } }
        back()
    }

    // Rooms are read-only in the app: the list comes from the desk site's
    // centre config (acco-handler) and is cached for offline. Only the three
    // check-in switches remain user-editable here.
    fun toggleLaundry() = persistOps(_state.value.centreOps.let { it.copy(laundry = !it.laundry) })
    fun toggleValuables() = persistOps(_state.value.centreOps.let { it.copy(valuables = !it.valuables) })
    fun toggleGroups() = persistOps(_state.value.centreOps.let { it.copy(groups = !it.groups) })

    /** The centre's own reconfirmation wording; blank restores the default. */
    fun setWhatsAppTemplate(text: String) = persistOps(
        _state.value.centreOps.copy(whatsAppTemplate = text.take(1000)),
    )

    /** Room chart column stepper (spec S4): device-local grid shape per gender+section block. */
    fun setRoomColumns(gender: Gender, section: String, columns: Int) = persistOps(
        _state.value.centreOps.let { it.copy(roomLayout = it.roomLayout.withColumns(gender, section, columns)) },
    )

    /** Phone Zero Day edits the same records the tablet dialog writes. */
    private fun patchRecord(id: ApplicantId, patch: (CheckInRecord) -> CheckInRecord) {
        val cur = _state.value.checkIns[id] ?: CheckInRecord()
        persistCheckIns(_state.value.checkIns + (id to patch(cur).clearSyncedIfChanged(cur)))
    }

    fun setZeroDaySeat(card: ApplicantCard, seat: String) =
        patchRecord(card.id) { it.copy(seat = seat) }

    fun toggleZeroDayLaundry(card: ApplicantCard) =
        patchRecord(card.id) { it.copy(laundry = !it.laundry) }

    fun toggleZeroDayValuables(card: ApplicantCard) =
        patchRecord(card.id) { it.copy(valuables = !it.valuables) }

    fun markAttended(card: ApplicantCard) {
        val record = _state.value.checkIns[card.id] ?: CheckInRecord()
        val (text, err) = deskSaveSnack(record, card)
        if (err) {
            _state.update { it.copy(snack = FlushSnack(text, error = true)) }
            return
        }
        persistCheckIns(
            _state.value.checkIns + (card.id to record.copy(checkedIn = true).clearSyncedIfChanged(record)),
        )
        _state.update { cur ->
            val rows = cur.rows.map { if (it.id == card.id) it.copy(attended = true) else it }
            cur.copy(
                rows = rows,
                visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                auditRows = flagAudit(rows),
                snack = FlushSnack(text, error = false),
            )
        }
        viewModelScope.launch { repo.markAttendedLocal(card.id) }
    }

    fun setCallFilter(filter: String) { _state.update { it.copy(callFilter = filter) } }

    fun setCallSearch(q: String) { _state.update { it.copy(callSearch = q) } }

    fun toggleCallPriority() { _state.update { it.copy(callPriority = !it.callPriority) } }

    /** Log an outcome. "No answer" also counts as an attempt, mirroring the tracker. */
    fun setCallState(card: ApplicantCard, value: String) {
        val cur = _state.value.callState[card.id] ?: CallRecord()
        persistCallLog(
            _state.value.callState + (card.id to cur.copy(
                outcome = value,
                attempts = if (value == "No answer") cur.attempts + 1 else cur.attempts,
                lastAttemptMs = System.currentTimeMillis(),
            )),
        )
    }

    /** A dial or WhatsApp tap: bump the attempts counter without touching the outcome. */
    fun logCallAttempt(card: ApplicantCard) {
        val cur = _state.value.callState[card.id] ?: CallRecord()
        persistCallLog(
            _state.value.callState + (card.id to cur.copy(
                attempts = cur.attempts + 1,
                lastAttemptMs = System.currentTimeMillis(),
            )),
        )
    }

    fun setCallNote(card: ApplicantCard, note: String) {
        val cur = _state.value.callState[card.id] ?: CallRecord()
        persistCallLog(_state.value.callState + (card.id to cur.copy(note = note.take(200))))
    }

    private fun persistCallLog(records: Map<ApplicantId, CallRecord>) {
        _state.update { it.copy(callState = records) }
        viewModelScope.launch {
            sessionStore.setCallLog(records.entries.associate { (id, rec) -> id.value to rec })
        }
    }

    private fun persistOps(prefs: CentreOpsPrefs) {
        _state.update { it.copy(centreOps = prefs) }
        viewModelScope.launch { sessionStore.setCentreOps(prefs) }
    }

    fun onQuery(q: String) {
        _state.update {
            it.copy(query = q, visible = WorklistFilter.visible(it.rows, it.selected, q))
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            if (!_state.value.offline) refreshWorklist(unfiltered = false)
        }
    }

    fun toggleStatus(key: String) {
        _state.update { cur ->
            val next = if (key == "All") {
                emptySet()
            } else {
                val k = cur.selected.firstOrNull { it.equals(key, true) }
                if (k != null) cur.selected - k else cur.selected + key
            }
            cur.copy(selected = next, visible = WorklistFilter.visible(cur.rows, next, cur.query))
        }
        if (!_state.value.offline) {
            viewModelScope.launch { refreshWorklist(unfiltered = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshWorklist(unfiltered = true) }
    }

    fun openCard(card: ApplicantCard) {
        // Back from the card returns to the Advanced Search results only when
        // the card was opened from there; any other origin backs to Today.
        returnTo = DeskScreen.Search.takeIf { _state.value.screen == DeskScreen.Search }
        _state.update { cur ->
            val hasHealth = cur.sensitiveById[card.id]?.health?.isNotEmpty() == true
            cur.copy(
                card = card,
                screen = DeskScreen.Card,
                snack = deskHealthSnack(cur.card?.id, card.id, hasHealth) ?: cur.snack,
            )
        }
        viewModelScope.launch {
            val fresh = runCatching { repo.loadCard(card.id) }.getOrElse { e ->
                handleAuth(e)
                repo.cachedCard(card.id) ?: card
            }
            _state.update { cur ->
                val merged = cur.rows.firstOrNull { it.id == fresh.id } ?: fresh
                cur.copy(card = merged.copy(history = fresh.history ?: merged.history))
            }
        }
    }

    fun openPhotos() {
        val course = _state.value.course
        _state.update { it.copy(screen = DeskScreen.Photos) }
        if (course != null) {
            ensureWorklist(course)
            viewModelScope.launch {
                runCatching { repo.photoReview(course.id) }
                    .onSuccess { list -> _state.update { it.copy(photos = list) } }
                    .onFailure { handleAuth(it) }
            }
        }
    }

    fun openSummary() {
        val course = _state.value.course
        _state.update { it.copy(screen = DeskScreen.Summary) }
        if (course != null) ensureWorklist(course)
    }

    fun openSettings() {
        val cur = _state.value.screen
        if (cur != DeskScreen.Settings) returnTo = cur
        _state.update { it.copy(screen = DeskScreen.Settings) }
    }

    fun back() {
        // An open sheet viewer swallows back: close the overlay, leave the
        // DeskScreen (and the desk section underneath) untouched.
        if (_state.value.sheetView != null) {
            closeSheet()
            return
        }
        val next = deskBack(_state.value.screen, returnTo)
        // Rooms round-trips clobber returnTo; restore the settings origin so a
        // second back from CentreOps still lands where the user came from.
        if (next == DeskScreen.CentreOps) returnTo = centreOpsFrom
        _state.update { cur ->
            cur.copy(
                screen = next,
                sheetOpen = false,
                deskAction = if (next == DeskScreen.DeskAction) cur.deskAction else null,
            )
        }
    }

    fun openSheet() {
        val card = _state.value.card ?: return
        _state.update {
            it.copy(
                sheetOpen = true,
                sheetPick = "",
                sheetComment = "",
                sheetCustom = "",
            )
        }
        // keep current on the header; pick starts empty until user chooses
        _state.update { it.copy(sheetPick = it.statusChoices.firstOrNull { c -> c != "Custom…" } ?: card.status.value) }
    }

    fun dismissSheet() { _state.update { it.copy(sheetOpen = false) } }
    fun onSheetPick(v: String) { _state.update { it.copy(sheetPick = v) } }
    fun onSheetComment(v: String) { _state.update { it.copy(sheetComment = v) } }
    fun onSheetCustom(v: String) { _state.update { it.copy(sheetCustom = v) } }

    fun confirmStatus() {
        val s = _state.value
        val card = s.card ?: return
        val status = if (s.sheetPick.contains("Custom", true)) s.sheetCustom.trim() else s.sheetPick
        if (status.isBlank()) return
        if (ApplicantStatus.isForbiddenWrite(status)) {
            _state.update {
                it.copy(sheetOpen = false, snack = FlushSnack("The app never sends Approved", error = true))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(sheetOpen = false) }
            runCatching {
                repo.changeStatus(card.id, status, s.sheetComment, s.offline)
            }.onSuccess { snack ->
                _state.update { cur ->
                    val updated = cur.rows.firstOrNull { it.id == card.id } ?: cur.card
                    cur.copy(snack = snack, card = updated ?: cur.card)
                }
            }.onFailure { handleAuth(it) }
        }
    }

    /**
     * A status change made from a row rather than from the open card — the
     * calling round's inline changer. Same write as [confirmStatus]: the
     * repository echoes locally, queues the GET and flushes when online, so
     * the outcome reads back in the snackbar either way.
     */
    fun changeStatusFor(card: ApplicantCard, status: String) {
        val value = status.trim()
        if (value.isBlank()) return
        if (ApplicantStatus.isForbiddenWrite(value)) {
            _state.update { it.copy(snack = FlushSnack("The app never sends Approved", error = true)) }
            return
        }
        viewModelScope.launch {
            runCatching { repo.changeStatus(card.id, value, "", _state.value.offline) }
                .onSuccess { snack -> _state.update { it.copy(snack = snack) } }
                .onFailure { handleAuth(it) }
        }
    }

    fun consumeSnack() { _state.update { it.copy(snack = null) } }

    fun setPhotoFilter(f: String) { _state.update { it.copy(photoFilter = f) } }

    /**
     * Live photo for one applicant — GET show-photo/{id} through the shared
     * authenticated client, ≤6 concurrent, memory-cached only. Null (403/404/
     * offline) keeps the initials placeholder.
     */
    suspend fun loadPhoto(id: ApplicantId): ImageBitmap? =
        photoLoader.load(id.value)?.asImageBitmap()

    fun rotatePhoto(id: ApplicantId, delta: Int) {
        viewModelScope.launch {
            val cur = photoStore.snapshot()[id] ?: seedEdit(id)
            photoStore.put(id, cur.copy(rotate = ((cur.rotate + delta) % 360 + 360) % 360, done = false, uploaded = false))
        }
    }

    fun cropPhoto(id: ApplicantId) {
        viewModelScope.launch {
            val cur = photoStore.snapshot()[id] ?: seedEdit(id)
            photoStore.put(id, cur.copy(cropped = true, done = false, uploaded = false))
        }
    }

    fun markPhotoDone(id: ApplicantId) {
        viewModelScope.launch {
            val cur = photoStore.snapshot()[id] ?: seedEdit(id)
            photoStore.put(id, cur.copy(done = true, uploaded = false))
        }
    }

    fun uploadPhotos() {
        viewModelScope.launch {
            runCatching { repo.uploadPhotos(_state.value.edits) }
                .onSuccess { (n, msg) ->
                    if (n > 0) {
                        _state.value.edits.filter { it.value.done && !it.value.uploaded }.forEach { (id, e) ->
                            photoStore.put(id, e.copy(uploaded = true))
                        }
                    }
                    _state.update { it.copy(snack = FlushSnack(msg, error = n == 0)) }
                }
                .onFailure { handleAuth(it) }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch { sessionStore.setTheme(!_state.value.dark) }
    }

    fun setSkin(skin: DeskSkin) {
        viewModelScope.launch { sessionStore.setSkin(skin.key) }
    }

    fun toggleLotus() {
        viewModelScope.launch { sessionStore.setLotus(!_state.value.lotus) }
    }

    fun toggleOffline() {
        viewModelScope.launch { sessionStore.setForceOffline(!_state.value.offline) }
    }

    fun logout() {
        viewModelScope.launch {
            keepAliveJob?.cancel()
            returnTo = null
            repo.logout()
            photoStore.clear()
            val saved = sessionStore.remembered()
            _state.value = DeskUiState(
                dark = _state.value.dark,
                skin = _state.value.skin,
                lotus = _state.value.lotus,
                remember = saved.on,
                username = if (saved.on) saved.username else "",
                password = if (saved.on) saved.password else "",
            )
        }
    }

    fun factoryReset() {
        viewModelScope.launch {
            keepAliveJob?.cancel()
            returnTo = null
            repo.factoryReset()
            photoStore.clear()
            _state.value = DeskUiState()
        }
    }

    fun pendingUploads(): Int =
        _state.value.edits.values.count { it.done && !it.uploaded }

    fun photoNote(card: ApplicantCard): String {
        val edit = _state.value.edits[card.id]
        val sug = _state.value.photos.firstOrNull { it.applicantId == card.id }
        return when {
            edit?.done == true -> "◎ Photo fixed"
            sug != null && sug.kind != "auto" && sug.kind != "good" -> "◎ Photo needs review"
            else -> "◎ Photo looks fine"
        }
    }

    private suspend fun afterLogin(session: Session) {
        _state.update { it.copy(session = session, loginLoading = false, loginError = null) }
        startKeepAlive()
        val centre = session.centres.firstOrNull()
        if (centre != null) {
            runCatching { repo.loadCourses(centre.id) }
                .onSuccess { lists ->
                    _state.update {
                        it.copy(
                            courses = lists.upcoming,
                            olderCourses = lists.older,
                            screen = deskAfterLogin(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loginError = e.message, screen = DeskScreen.Login) }
                    handleAuth(e)
                }
        } else {
            _state.update { it.copy(screen = deskAfterLogin()) }
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = viewModelScope.launch {
            while (true) {
                delay(KEEP_ALIVE_MS)
                if (_state.value.session == null || _state.value.offline) continue
                runCatching { repo.keepAlive() }.onFailure { handleAuth(it) }
            }
        }
    }

    companion object {
        const val KEEP_ALIVE_MS = 20 * 60 * 1000L
    }

    private suspend fun restore() {
        runCatching { repo.restoreSession() }.onSuccess { session ->
            if (session != null) afterLogin(session)
        }.onFailure { handleAuth(it) }
    }

    private fun ensureWorklist(course: Course) {
        if (observeJob?.isActive != true) {
            observeJob = viewModelScope.launch {
                val id = course.id
                repo.observeApplicants(id).collect { rows ->
                    _state.update { cur ->
                        cur.copy(
                            rows = rows,
                            visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                            auditRows = flagAudit(rows),
                            loading = false,
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            refreshWorklist(unfiltered = true)
            runCatching { repo.loadStatuses() }.onSuccess { list ->
                _state.update { it.copy(statusChoices = list) }
            }
            pullRooms(userInitiated = false)
        }
    }

    private suspend fun refreshWorklist(unfiltered: Boolean) {
        val course = _state.value.course ?: return
        val s = _state.value
        _state.update { it.copy(loading = true) }
        val status = if (unfiltered || s.selected.isEmpty()) null else s.selected.joinToString(",")
        val q = if (unfiltered) null else s.query.takeIf { it.isNotBlank() }
        runCatching { repo.refreshApplicants(course.id, status, q, course.centreId) }
            .onSuccess { (_, counts) ->
                _state.update {
                    it.copy(counts = counts, sensitiveById = repo.sensitiveSnapshot(), loading = false)
                }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false) }
                handleAuth(e)
            }
    }

    /**
     * User-initiated outbox retry from the queued strip. Always attempts the
     * send — no client-side reachability gate (hard rule 1); failures surface
     * through the existing FlushSnack path.
     */
    fun retrySync() {
        viewModelScope.launch { flush() }
    }

    private suspend fun flush() {
        // The one shared flush path: stamped here so the reconnect collector
        // and retrySync() both feed the strip's "last try" line.
        _state.update { it.copy(lastSyncAttemptAt = System.currentTimeMillis()) }
        runCatching { repo.flushOutbox() }
            .onSuccess { snacks ->
                snacks.lastOrNull()?.let { snack -> _state.update { it.copy(snack = snack) } }
            }
            .onFailure { handleAuth(it) }
    }

    private fun seedEdit(id: ApplicantId): PhotoEdit {
        val sug = _state.value.photos.firstOrNull { it.applicantId == id }
        return PhotoEdit(rotate = sug?.suggestedRotate ?: 0, cropped = sug?.suggestedCrop == true)
    }

    private fun flagAudit(rows: List<ApplicantCard>): List<ApplicantCard> =
        rows.map { card ->
            card.copy(flags = ClientAudit.merge(ClientAudit.evaluate(card, rows), card.flags))
        }.filter { it.flags.isNotEmpty() }
            .sortedWith(compareByDescending<ApplicantCard> { it.hardFlagCount }.thenBy { it.displayName })

    private fun handleAuth(e: Throwable) {
        if (e is ApiException && e.unauthorized) {
            viewModelScope.launch {
                keepAliveJob?.cancel()
                returnTo = null
                // Session timeout, not a logout: drop only the dead cookies so
                // queued outbox rows, check-ins, and room-sync progress survive
                // the re-login (owner amendment: partial sync progress persists).
                runCatching { repo.sessionExpired() }
                val saved = sessionStore.remembered()
                _state.value = DeskUiState(
                    dark = _state.value.dark,
                    skin = _state.value.skin,
                    lotus = _state.value.lotus,
                    loginError = e.message,
                    screen = DeskScreen.Login,
                    remember = saved.on,
                    username = if (saved.on) saved.username else "",
                    password = if (saved.on) saved.password else "",
                )
            }
        } else if (_state.value.snack == null && e.message != null && e !is ApiException) {
            _state.update { it.copy(snack = FlushSnack(e.message ?: "", error = true)) }
        } else if (e is ApiException && !e.unauthorized) {
            _state.update { it.copy(snack = FlushSnack(e.message ?: "", error = true)) }
        }
    }

    /** Test-only: preload a signed-in desk state without touching the network. */
    @androidx.annotation.VisibleForTesting
    internal fun seedForTest(state: DeskUiState) {
        _state.value = state
    }
}
