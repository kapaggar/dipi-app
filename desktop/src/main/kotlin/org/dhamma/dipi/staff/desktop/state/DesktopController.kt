package org.dhamma.dipi.staff.desktop.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dhamma.dipi.staff.desktop.DesktopConfig
import org.dhamma.dipi.staff.desktop.data.ApiException
import org.dhamma.dipi.staff.desktop.data.DesktopRepository
import org.dhamma.dipi.staff.desktop.data.DesktopStore
import org.dhamma.dipi.staff.desktop.derive.DeskSection
import org.dhamma.dipi.staff.desktop.derive.deskRecord
import org.dhamma.dipi.staff.desktop.derive.deskRoomSyncPending
import org.dhamma.dipi.staff.desktop.derive.deskSaveSnack
import org.dhamma.dipi.staff.desktop.derive.roomPullSnack
import org.dhamma.dipi.staff.desktop.derive.roomSyncSnack
import org.dhamma.dipi.staff.desktop.derive.stripHonorific
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.model.WorklistFilter
import org.dhamma.dipi.staff.model.clearSyncedIfChanged
import java.net.URI

class DesktopController(
    private val repo: DesktopRepository,
    private val store: DesktopStore,
    private val scope: CoroutineScope,
    private val config: DesktopConfig,
) {
    private val _state = MutableStateFlow(
        DeskUiState(
            dark = store.dark(),
            lotus = store.lotus(),
            deskGender = store.deskGender(),
            deskSeniority = store.deskSeniority(),
            centreOps = store.centreOps(),
            lastSync = store.lastSync(),
            offline = store.forceOffline(),
            versionName = config.versionName,
            hostLabel = URI(config.baseUrl).host ?: config.baseUrl,
        ),
    )
    val state: StateFlow<DeskUiState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var keepAliveJob: Job? = null
    private var returnTo: DeskScreen? = null

    init {
        val saved = store.remembered()
        if (saved.on) {
            _state.update {
                it.copy(username = saved.username, password = saved.password, remember = true)
            }
        }
        _state.update {
            it.copy(
                checkIns = store.checkIns().entries.associate { (id, rec) -> ApplicantId(id) to rec },
                callState = store.callLog().entries.associate { (id, rec) -> ApplicantId(id) to rec },
            )
        }
        scope.launch {
            repo.observeOutbox().collect { rows ->
                val pending = rows.filter { it.state != "Synced" }
                _state.update {
                    it.copy(
                        queuedCount = pending.size,
                        queuedById = pending.associate { row -> ApplicantId(row.applicantId) to row.status },
                    )
                }
            }
        }
        scope.launch { restore() }
    }

    fun onUser(v: String) { _state.update { it.copy(username = v) } }
    fun onPass(v: String) { _state.update { it.copy(password = v) } }
    fun onRemember(v: Boolean) { _state.update { it.copy(remember = v) } }

    fun signIn() {
        val s = _state.value
        scope.launch {
            _state.update { it.copy(loginLoading = true, loginError = null) }
            runCatching { repo.login(s.username, s.password) }
                .onSuccess { session ->
                    store.setRemembered(s.remember, s.username, s.password)
                    afterLogin(session)
                }
                .onFailure { e ->
                    _state.update { it.copy(loginLoading = false, loginError = e.message) }
                }
        }
    }

    fun pickCentre(centre: Centre) {
        scope.launch {
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
        _state.update {
            it.copy(
                course = course,
                screen = DeskScreen.Desk,
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
            )
        }
        ensureDesk()
    }

    fun setDeskSection(section: DeskSection) {
        _state.update { it.copy(deskSection = section, sheetView = null) }
    }

    fun ensureDesk() {
        val course = _state.value.course ?: return
        if (_state.value.rows.isEmpty()) _state.update { it.copy(loading = true) }
        ensureWorklist(course)
    }

    fun setDeskScan(q: String) = _state.update { it.copy(deskScan = q) }
    fun setDeskZeroFilter(f: String) = _state.update { it.copy(deskZeroFilter = f) }

    fun setDeskGender(g: String) {
        _state.update { it.copy(deskGender = g) }
        store.setDeskGender(g)
    }

    fun setDeskSeniority(s: String) {
        _state.update { it.copy(deskSeniority = s) }
        store.setDeskSeniority(s)
    }

    fun openDeskMark(card: ApplicantCard) =
        _state.update { it.copy(deskMarkId = card.id, deskRoomOpen = false) }

    fun closeDeskMark() = _state.update { it.copy(deskMarkId = null, deskRoomOpen = false) }
    fun toggleDeskRoomPicker() = _state.update { it.copy(deskRoomOpen = !it.deskRoomOpen) }

    private fun markCard(): ApplicantCard? {
        val id = _state.value.deskMarkId ?: return null
        return _state.value.rows.firstOrNull { it.id == id }
    }

    fun deskMarkRecord(): CheckInRecord {
        val card = markCard() ?: return CheckInRecord()
        return deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
    }

    private fun patchDeskRecord(patch: (CheckInRecord) -> CheckInRecord) {
        val card = markCard() ?: return
        val cur = deskRecord(card, _state.value.checkIns) ?: CheckInRecord()
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
        scope.launch { repo.markAttendedLocal(card.id, attended = true) }
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
        scope.launch { repo.markAttendedLocal(card.id, attended = false) }
    }

    private fun persistCheckIns(records: Map<ApplicantId, CheckInRecord>) {
        _state.update { it.copy(checkIns = records) }
        store.setCheckIns(records.entries.associate { (id, rec) -> id.value to rec })
    }

    fun pullRooms(userInitiated: Boolean = true) {
        val s = _state.value
        if (s.roomPullBusy || s.roomSyncBusy) return
        val course = s.course ?: return
        if (s.offline) {
            if (userInitiated) {
                _state.update { it.copy(snack = FlushSnack("offline — will pull when online", error = false)) }
            }
            return
        }
        scope.launch {
            _state.update { it.copy(roomPullBusy = true) }
            runCatching { repo.pullRoomAllocations(course.centreId.value, course.id.value) }
                .onSuccess { pulled ->
                    persistCheckIns(org.dhamma.dipi.staff.model.RoomAllocSync.mergePulled(_state.value.checkIns, pulled))
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
        scope.launch {
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

    fun selectDeskFinding(code: String) = _state.update { it.copy(deskFinding = code) }

    fun runDeskBatch(code: String, label: String) {
        if (code != "name_title_prefix") return
        val targets = _state.value.auditRows.filter { card -> card.flags.any { it.ruleId == code } }
        scope.launch {
            targets.forEach { card ->
                val stripped = stripHonorific(card.givenName)
                if (stripped != card.givenName) repo.setGivenNameLocal(card.id, stripped)
            }
            _state.update { cur ->
                val rows = cur.rows.map { card ->
                    if (targets.any { it.id == card.id }) card.copy(givenName = stripHonorific(card.givenName)) else card
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

    fun selectDeskApp(card: ApplicantCard) {
        _state.update { cur ->
            val hasHealth = cur.sensitiveById[card.id]?.health?.isNotEmpty() == true
            val newSelection = cur.deskAppId != card.id
            cur.copy(
                deskAppId = card.id,
                card = card,
                snack = if (hasHealth && newSelection) {
                    FlushSnack("Health disclosures on file — review before confirming", error = false)
                } else {
                    cur.snack
                },
            )
        }
    }

    fun deskNote(text: String) {
        _state.update { it.copy(snack = FlushSnack(text, error = false)) }
    }

    fun openSheetExport(label: String) {
        val export = SheetExport.fromLabel(label) ?: return
        val course = _state.value.course ?: return
        _state.update { it.copy(sheetView = SheetViewUi(title = export.label)) }
        scope.launch {
            resolveSheet(export.label) { repo.fetchSheet(export, course.centreId.value, course.id.value) }
        }
    }

    fun openAppEdit(card: ApplicantCard) {
        val title = "Edit · ${card.displayName}"
        _state.update { it.copy(sheetView = SheetViewUi(title = title)) }
        scope.launch { resolveSheet(title) { repo.fetchAppEditPage(card.id) } }
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
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q, visible = WorklistFilter.visible(it.rows, it.selected, q)) }
    }

    fun openStatusSheet() {
        val card = _state.value.card ?: return
        _state.update {
            it.copy(
                sheetOpen = true,
                sheetComment = "",
                sheetCustom = "",
                sheetPick = it.statusChoices.firstOrNull { c -> c != "Custom…" } ?: card.status.value,
            )
        }
    }

    fun dismissStatusSheet() { _state.update { it.copy(sheetOpen = false) } }
    fun onSheetPick(v: String) { _state.update { it.copy(sheetPick = v) } }
    fun onSheetComment(v: String) { _state.update { it.copy(sheetComment = v) } }
    fun onSheetCustom(v: String) { _state.update { it.copy(sheetCustom = v) } }

    fun confirmStatus() {
        val s = _state.value
        val card = s.card ?: return
        val status = if (s.sheetPick.contains("Custom", true)) s.sheetCustom.trim() else s.sheetPick
        if (status.isBlank()) return
        if (status.equals("Approved", ignoreCase = true)) {
            _state.update { it.copy(snack = FlushSnack("The desk client never sends Approved", error = true)) }
            return
        }
        scope.launch {
            _state.update { it.copy(sheetOpen = false) }
            runCatching { repo.changeStatus(card.id, status, s.sheetComment, s.offline) }
                .onSuccess { snack ->
                    _state.update { cur ->
                        val updated = cur.rows.firstOrNull { it.id == card.id } ?: cur.card
                        cur.copy(snack = snack, card = updated ?: cur.card)
                    }
                }
                .onFailure { handleAuth(it) }
        }
    }

    fun consumeSnack() { _state.update { it.copy(snack = null) } }

    fun setCallFilter(filter: String) { _state.update { it.copy(callFilter = filter) } }

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
        store.setCallLog(records.entries.associate { (id, rec) -> id.value to rec })
    }

    fun toggleLaundry() = persistOps(_state.value.centreOps.copy(laundry = !_state.value.centreOps.laundry))
    fun toggleValuables() = persistOps(_state.value.centreOps.copy(valuables = !_state.value.centreOps.valuables))
    fun toggleGroups() = persistOps(_state.value.centreOps.copy(groups = !_state.value.centreOps.groups))

    private fun persistOps(prefs: org.dhamma.dipi.staff.model.CentreOpsPrefs) {
        _state.update { it.copy(centreOps = prefs) }
        store.setCentreOps(prefs)
    }

    fun openSettings() {
        val cur = _state.value.screen
        if (cur != DeskScreen.Settings) returnTo = cur
        _state.update { it.copy(screen = DeskScreen.Settings) }
    }

    fun back() {
        if (_state.value.sheetView != null) {
            closeSheet()
            return
        }
        if (_state.value.sheetOpen) {
            dismissStatusSheet()
            return
        }
        if (_state.value.deskMarkId != null) {
            closeDeskMark()
            return
        }
        when (_state.value.screen) {
            DeskScreen.Settings -> _state.update { it.copy(screen = returnTo ?: DeskScreen.Centre) }
            DeskScreen.Desk -> _state.update { it.copy(screen = DeskScreen.Centre) }
            DeskScreen.Centre, DeskScreen.Login -> _state.update { it.copy(confirmExit = true) }
        }
    }

    fun stay() { _state.update { it.copy(confirmExit = false) } }

    fun toggleTheme() {
        val next = !_state.value.dark
        store.setDark(next)
        _state.update { it.copy(dark = next) }
    }

    fun toggleLotus() {
        val next = !_state.value.lotus
        store.setLotus(next)
        _state.update { it.copy(lotus = next) }
    }

    fun toggleOffline() {
        val next = !_state.value.offline
        store.setForceOffline(next)
        _state.update { it.copy(offline = next) }
    }

    fun logout() {
        scope.launch {
            keepAliveJob?.cancel()
            returnTo = null
            repo.logout()
            val saved = store.remembered()
            _state.value = DeskUiState(
                dark = _state.value.dark,
                lotus = _state.value.lotus,
                remember = saved.on,
                username = if (saved.on) saved.username else "",
                password = if (saved.on) saved.password else "",
                versionName = config.versionName,
                hostLabel = URI(config.baseUrl).host ?: config.baseUrl,
            )
        }
    }

    fun factoryReset() {
        scope.launch {
            keepAliveJob?.cancel()
            returnTo = null
            repo.factoryReset()
            _state.value = DeskUiState(
                versionName = config.versionName,
                hostLabel = URI(config.baseUrl).host ?: config.baseUrl,
            )
        }
    }

    fun refresh() {
        scope.launch { refreshWorklist() }
    }

    private suspend fun afterLogin(session: org.dhamma.dipi.staff.model.Session) {
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
                            screen = DeskScreen.Centre,
                            centreOps = store.centreOps(),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loginError = e.message, screen = DeskScreen.Login) }
                    handleAuth(e)
                }
        } else {
            _state.update { it.copy(screen = DeskScreen.Centre) }
        }
    }

    private fun startKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            while (true) {
                delay(KEEP_ALIVE_MS)
                if (_state.value.session == null || _state.value.offline) continue
                runCatching { repo.keepAlive() }.onFailure { handleAuth(it) }
            }
        }
    }

    private suspend fun restore() {
        runCatching { repo.restoreSession() }.onSuccess { session ->
            if (session != null) afterLogin(session)
        }.onFailure { handleAuth(it) }
    }

    private fun ensureWorklist(course: Course) {
        if (observeJob?.isActive != true) {
            observeJob = scope.launch {
                repo.observeApplicants(course.id).collect { rows ->
                    _state.update { cur ->
                        cur.copy(
                            rows = rows,
                            visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                            auditRows = flagAudit(rows),
                            loading = false,
                            sensitiveById = repo.sensitiveSnapshot(),
                        )
                    }
                }
            }
        }
        scope.launch { refreshWorklist() }
    }

    private suspend fun refreshWorklist() {
        val course = _state.value.course ?: return
        val centre = _state.value.session?.centres?.firstOrNull()?.id
        runCatching {
            repo.refreshApplicants(course.id, centreId = centre)
        }.onSuccess { (rows, counts) ->
            _state.update { cur ->
                cur.copy(
                    rows = rows,
                    visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                    counts = counts,
                    auditRows = flagAudit(rows),
                    loading = false,
                    lastSync = store.lastSync(),
                    statusChoices = ApplicantStatus.mergeChoices(counts.keys.filter { it != "All" }),
                    sensitiveById = repo.sensitiveSnapshot(),
                    centreOps = store.centreOps(),
                )
            }
            pullRooms(userInitiated = false)
        }.onFailure { handleAuth(it) }
    }

    private fun flagAudit(rows: List<ApplicantCard>): List<ApplicantCard> =
        rows.filter { it.flags.isNotEmpty() }

    private fun handleAuth(e: Throwable) {
        val api = (e as? ApiException) ?: return
        if (api.unauthorized) {
            scope.launch {
                repo.sessionExpired()
                keepAliveJob?.cancel()
                _state.update {
                    it.copy(
                        session = null,
                        screen = DeskScreen.Login,
                        loginError = api.message,
                        snack = FlushSnack("Session expired — sign in", error = true),
                    )
                }
            }
        } else {
            _state.update { it.copy(snack = FlushSnack(api.message ?: "Request failed", error = true)) }
        }
    }

    companion object {
        const val KEEP_ALIVE_MS = 20 * 60 * 1000L
    }
}
