package org.dhamma.dipi.staff.ui

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
import org.dhamma.dipi.staff.data.ApiException
import org.dhamma.dipi.staff.data.ConnectivityMonitor
import org.dhamma.dipi.staff.data.PhotoEditStore
import org.dhamma.dipi.staff.data.StaffRepository
import org.dhamma.dipi.staff.datastore.SessionStore
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.PhotoEdit
import org.dhamma.dipi.staff.model.PhotoReviewItem
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.WorklistFilter
import javax.inject.Inject

enum class DeskScreen { Login, Courses, Today, Card, Photos, Summary, Settings }

data class DeskUiState(
    val screen: DeskScreen = DeskScreen.Login,
    val username: String = "",
    val password: String = "",
    val loginError: String? = null,
    val loginLoading: Boolean = false,
    val session: Session? = null,
    val courses: List<Course> = emptyList(),
    val course: Course? = null,
    val rows: List<ApplicantCard> = emptyList(),
    val visible: List<ApplicantCard> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val selected: Set<String> = emptySet(),
    val query: String = "",
    val loading: Boolean = false,
    val card: ApplicantCard? = null,
    val dark: Boolean = false,
    val offline: Boolean = false,
    val queuedById: Map<ApplicantId, String> = emptyMap(),
    val queuedCount: Int = 0,
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
)

@HiltViewModel
class DeskViewModel @Inject constructor(
    private val repo: StaffRepository,
    private val sessionStore: SessionStore,
    private val photoStore: PhotoEditStore,
    connectivity: ConnectivityMonitor,
) : ViewModel() {

    private val _state = MutableStateFlow(DeskUiState())
    val state: StateFlow<DeskUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var observeJob: Job? = null
    private var lastOffline: Boolean? = null

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
            sessionStore.lastSync.collect { sync -> _state.update { it.copy(lastSync = sync) } }
        }
        viewModelScope.launch {
            photoStore.edits.collect { edits -> _state.update { it.copy(edits = edits) } }
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
        viewModelScope.launch { restore() }
    }

    fun onUser(v: String) { _state.update { it.copy(username = v) } }
    fun onPass(v: String) { _state.update { it.copy(password = v) } }

    fun signIn() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(loginLoading = true, loginError = null) }
            runCatching { repo.login(s.username, s.password) }
                .onSuccess { afterLogin(it) }
                .onFailure { e ->
                    _state.update {
                        it.copy(loginLoading = false, loginError = e.message)
                    }
                }
        }
    }

    fun pickCourse(course: Course) {
        _state.update { it.copy(course = course, screen = DeskScreen.Today, loading = true) }
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val id = course.id
            repo.observeApplicants(id).collect { rows ->
                _state.update { cur ->
                    cur.copy(
                        rows = rows,
                        visible = WorklistFilter.visible(rows, cur.selected, cur.query),
                        loading = false,
                    )
                }
            }
        }
        viewModelScope.launch {
            refreshWorklist(unfiltered = true)
            runCatching { repo.loadStatuses() }.onSuccess { list ->
                _state.update { it.copy(statusChoices = ApplicantStatus.mergeChoices(list)) }
            }
        }
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
        _state.update { it.copy(card = card, screen = DeskScreen.Card) }
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
        _state.update { it.copy(screen = DeskScreen.Photos) }
        val course = _state.value.course ?: return
        viewModelScope.launch {
            runCatching { repo.photoReview(course.id) }
                .onSuccess { list -> _state.update { it.copy(photos = list) } }
                .onFailure { handleAuth(it) }
        }
    }

    fun openSummary() { _state.update { it.copy(screen = DeskScreen.Summary) } }
    fun openSettings() { _state.update { it.copy(screen = DeskScreen.Settings) } }

    fun back() {
        _state.update { cur ->
            cur.copy(
                screen = when (cur.screen) {
                    DeskScreen.Card, DeskScreen.Photos, DeskScreen.Summary, DeskScreen.Settings ->
                        DeskScreen.Today
                    DeskScreen.Today -> DeskScreen.Courses
                    else -> cur.screen
                },
                sheetOpen = false,
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

    fun consumeSnack() { _state.update { it.copy(snack = null) } }

    fun setPhotoFilter(f: String) { _state.update { it.copy(photoFilter = f) } }

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

    fun toggleOffline() {
        viewModelScope.launch { sessionStore.setForceOffline(!_state.value.offline) }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
            photoStore.clear()
            _state.value = DeskUiState(dark = _state.value.dark)
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
        val centre = session.centres.firstOrNull()
        if (centre != null) {
            runCatching { repo.loadCourses(centre.id) }
                .onSuccess { list -> _state.update { it.copy(courses = list, screen = DeskScreen.Courses) } }
                .onFailure { e ->
                    _state.update { it.copy(loginError = e.message, screen = DeskScreen.Login) }
                    handleAuth(e)
                }
        } else {
            _state.update { it.copy(screen = DeskScreen.Courses) }
        }
    }

    private suspend fun restore() {
        runCatching { repo.restoreSession() }.onSuccess { session ->
            if (session != null) afterLogin(session)
        }.onFailure { handleAuth(it) }
    }

    private suspend fun refreshWorklist(unfiltered: Boolean) {
        val course = _state.value.course ?: return
        val s = _state.value
        _state.update { it.copy(loading = true) }
        val status = if (unfiltered || s.selected.isEmpty()) null else s.selected.joinToString(",")
        val q = if (unfiltered) null else s.query.takeIf { it.isNotBlank() }
        runCatching { repo.refreshApplicants(course.id, status, q) }
            .onSuccess { (_, counts) ->
                _state.update { it.copy(counts = counts, loading = false) }
            }
            .onFailure { e ->
                _state.update { it.copy(loading = false) }
                handleAuth(e)
            }
    }

    private suspend fun flush() {
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

    private fun handleAuth(e: Throwable) {
        if (e is ApiException && e.unauthorized) {
            viewModelScope.launch {
                runCatching { repo.logout() }
                photoStore.clear()
                _state.value = DeskUiState(
                    dark = _state.value.dark,
                    loginError = e.message,
                    screen = DeskScreen.Login,
                )
            }
        } else if (_state.value.snack == null && e.message != null && e !is ApiException) {
            _state.update { it.copy(snack = FlushSnack(e.message ?: "", error = true)) }
        } else if (e is ApiException && !e.unauthorized) {
            _state.update { it.copy(snack = FlushSnack(e.message ?: "", error = true)) }
        }
    }
}
