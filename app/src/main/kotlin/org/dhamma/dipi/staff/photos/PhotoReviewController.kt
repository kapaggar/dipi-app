package org.dhamma.dipi.staff.photos

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dhamma.dipi.staff.datastore.PhotoCorrectionStore
import org.dhamma.dipi.staff.desk.deskScoped
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoDrafts
import org.dhamma.dipi.staff.model.PhotoGeometry
import org.dhamma.dipi.staff.model.PhotoKey
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoReviewState
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoStamp
import org.dhamma.dipi.staff.model.PhotoSuggestion
import org.dhamma.dipi.staff.model.PhotoSuggestionPolicy
import org.dhamma.dipi.staff.model.PhotoSuggestionReason
import org.dhamma.dipi.staff.model.PhotoWriteState
import org.dhamma.dipi.staff.network.PhotoDeskWriteResult
import org.dhamma.dipi.staff.network.PhotoSource
import org.dhamma.dipi.staff.network.PhotoSourceResult
import java.io.ByteArrayOutputStream
import java.io.OutputStream

fun interface PhotoSourceGateway {
    suspend fun load(key: PhotoKey, forceRefresh: Boolean): PhotoSourceResult
}

fun interface PhotoDeskWrite {
    suspend fun submit(applicantId: Int, jpeg: ByteArray, fileName: String): PhotoDeskWriteResult
}

class PhotoReviewController internal constructor(
    private val store: PhotoCorrectionStore,
    private val scope: CoroutineScope,
    private val sources: PhotoSourceGateway,
    private val renderer: PhotoRenderer = PhotoRenderer(),
    private val scanner: PhotoScanner = PhotoScanner(),
    private val exporter: PhotoExport = PhotoExport(renderer),
    private val enabled: Boolean = org.dhamma.dipi.staff.BuildConfig.PHOTO_REVIEW_ENABLED,
    var openOutput: (String) -> OutputStream? = { null },
) {
    var deskWrite: PhotoDeskWrite? = null
    var invalidateSource: (Int) -> Unit = {}
    var allowDeskWrite: Boolean = true
    private val _state = MutableStateFlow(PhotoReviewUiState())
    val state: StateFlow<PhotoReviewUiState> = _state.asStateFlow()

    private val loaded = mutableMapOf<Int, PhotoSource>()
    private val previewCache = mutableMapOf<String, ImageBitmap>()
    private val suggestions = mutableMapOf<Int, PhotoSuggestion>()
    private val undo = ArrayDeque<PhotoRecipe>()
    private var editorRecipe = PhotoRecipe()
    private var editorId: Int? = null
    private var frozenExport = emptyList<PhotoDraft>()
    private var frozenUpdate = emptyList<PhotoDraft>()
    private var scanJob: Job? = null
    private var exportJob: Job? = null
    private var updateJob: Job? = null
    private var stopAfterCurrent = false

    fun offerNotice(text: String?) {
        if (!text.isNullOrBlank()) _state.update { it.copy(notice = text) }
    }

    fun dispatch(action: PhotoReviewAction) {
        if (!enabled) return
        when (action) {
            is PhotoReviewAction.Open -> open(action.scope, action.applicants, action.focusApplicantId)
            is PhotoReviewAction.SetSearch -> {
                _state.update { it.copy(search = action.query) }
                publish()
            }
            is PhotoReviewAction.SetReviewFilter -> {
                _state.update { it.copy(reviewFilter = action.filter) }
                publish()
            }
            is PhotoReviewAction.SetGender -> {
                _state.update { it.copy(gender = action.gender) }
                publish()
            }
            is PhotoReviewAction.SetSeniority -> {
                _state.update { it.copy(seniority = action.seniority) }
                publish()
            }
            is PhotoReviewAction.SelectApplicant -> openEditor(action.applicantId)
            is PhotoReviewAction.Rotate -> rotate(action.delta)
            is PhotoReviewAction.SetCrop -> setCrop(action.crop)
            PhotoReviewAction.Undo -> undo()
            PhotoReviewAction.Reset -> reset()
            PhotoReviewAction.SaveDraft -> persist(approve = false, advance = false)
            PhotoReviewAction.Approve -> persist(approve = true, advance = false)
            PhotoReviewAction.KeepAndNext -> persist(approve = true, advance = true)
            PhotoReviewAction.ApproveSelected -> openNext(needsReview = true)
            PhotoReviewAction.ToggleCompare -> _state.update { cur ->
                val editor = cur.editor ?: return@update cur
                cur.copy(editor = editor.copy(compareOriginal = !editor.compareOriginal))
            }
            PhotoReviewAction.CloseEditor -> closeEditor(abandon = true)
            PhotoReviewAction.Scan -> startScan()
            PhotoReviewAction.CancelScan -> {
                scanJob?.cancel()
                scanJob = null
                _state.update { it.copy(scan = null, snack = "Scan cancelled") }
            }
            PhotoReviewAction.ApplySafeSuggestions -> applySafe()
            PhotoReviewAction.ClearCourse -> clearCourse()
            PhotoReviewAction.Close -> resetSession(clearPixels = true)
            PhotoReviewAction.PrepareExport -> prepareExport()
            is PhotoReviewAction.ExportTo -> runExport(action.uri)
            PhotoReviewAction.CancelExport -> {
                exportJob?.cancel()
                frozenExport = emptyList()
                _state.update { it.copy(exportRequest = null, exportProgress = null) }
            }
            PhotoReviewAction.PrepareUpdate -> prepareUpdate()
            PhotoReviewAction.StartUpdate -> startUpdate()
            PhotoReviewAction.CancelUpdate -> {
                updateJob?.cancel()
                frozenUpdate = emptyList()
                stopAfterCurrent = false
                _state.update { it.copy(confirmUpdate = false, updating = false, updateProgress = null) }
            }
            PhotoReviewAction.StopAfterCurrent -> stopAfterCurrent = true
            PhotoReviewAction.DismissNotice -> _state.update { it.copy(notice = null, snack = null) }
        }
    }

    suspend fun preview(id: ApplicantId): ImageBitmap? {
        val draft = _state.value.drafts[id.value]
        val source = sourceOf(id.value) ?: return null
        if (draft != null && !PhotoGeometry.isIdentity(draft.recipe) && draft.source == source.stamp) {
            return rendered(id.value, source.bitmap, draft.recipe)
        }
        return source.bitmap.asImageBitmap()
    }

    suspend fun original(id: Int): ImageBitmap? = sourceOf(id)?.bitmap?.asImageBitmap()

    suspend fun corrected(id: Int): ImageBitmap? {
        val source = sourceOf(id) ?: return null
        val recipe = if (editorId == id) editorRecipe else _state.value.drafts[id]?.recipe ?: PhotoRecipe()
        return if (PhotoGeometry.isIdentity(recipe)) {
            source.bitmap.asImageBitmap()
        } else {
            rendered(id, source.bitmap, recipe)
        }
    }

    fun releasePixels() {
        scanJob?.cancel()
        exportJob?.cancel()
        updateJob?.cancel()
        loaded.values.forEach { runCatching { if (!it.bitmap.isRecycled) it.bitmap.recycle() } }
        loaded.clear()
        previewCache.clear()
        scanner.close()
        closeEditor(abandon = true)
    }

    fun resetSession(clearPixels: Boolean) {
        if (clearPixels) releasePixels()
        suggestions.clear()
        undo.clear()
        frozenExport = emptyList()
        frozenUpdate = emptyList()
        stopAfterCurrent = false
        _state.value = PhotoReviewUiState(allowDeskWrite = allowDeskWrite)
    }

    private fun open(scope: PhotoScope, applicants: List<ApplicantCard>, focus: Int?) {
        val drafts = store.drafts(scope).associateBy { it.key.applicantId }
        _state.value = PhotoReviewUiState(
            scope = scope,
            applicants = applicants,
            drafts = drafts,
            selectedApplicantId = focus,
            notice = _state.value.notice,
            allowDeskWrite = allowDeskWrite,
        )
        publish()
        if (focus != null) openEditor(focus)
    }

    private fun openEditor(applicantId: Int) {
        val scope = _state.value.scope ?: return
        val person = _state.value.applicants.firstOrNull { it.id.value == applicantId } ?: return
        editorId = applicantId
        undo.clear()
        val draft = _state.value.drafts[applicantId]
        editorRecipe = draft?.recipe ?: PhotoRecipe()
        _state.update {
            it.copy(
                selectedApplicantId = applicantId,
                editor = PhotoEditorUi(
                    applicantId = applicantId,
                    name = person.displayName,
                    recipe = editorRecipe,
                    review = draft?.review ?: PhotoReviewState.UNREVIEWED,
                    sourceWidth = draft?.source?.width ?: loaded[applicantId]?.stamp?.width ?: 0,
                    sourceHeight = draft?.source?.height ?: loaded[applicantId]?.stamp?.height ?: 0,
                ),
            )
        }
        this.scope.launch {
            val result = ensureSource(PhotoKey(scope, applicantId))
            val editor = _state.value.editor?.takeIf { it.applicantId == applicantId } ?: return@launch
            when (result) {
                is PhotoSourceResult.Ready -> {
                    val source = result.source
                    val bound = draft?.let { PhotoDrafts.bindSource(it, source.stamp) }
                    if (bound != null && bound.review == PhotoReviewState.SOURCE_CHANGED) {
                        store.save(bound)
                        editorRecipe = PhotoRecipe()
                    }
                    refreshDrafts()
                    _state.update { cur ->
                        cur.copy(
                            editor = editor.copy(
                                missing = false,
                                unsupported = false,
                                sourceWidth = source.stamp.width,
                                sourceHeight = source.stamp.height,
                                recipe = editorRecipe,
                                review = cur.drafts[applicantId]?.review ?: PhotoReviewState.UNREVIEWED,
                            ),
                        )
                    }
                }
                PhotoSourceResult.Missing -> _state.update {
                    it.copy(editor = editor.copy(missing = true))
                }
                PhotoSourceResult.UnsupportedImage, PhotoSourceResult.TooLarge -> _state.update {
                    it.copy(editor = editor.copy(unsupported = true))
                }
                PhotoSourceResult.AuthenticationRequired -> _state.update {
                    it.copy(snack = "Sign in to load photos", editor = editor.copy(missing = true))
                }
                PhotoSourceResult.Unavailable -> _state.update {
                    it.copy(editor = editor.copy(missing = true), snack = "Photo unavailable")
                }
            }
        }
    }

    private fun rotate(delta: Int) {
        val editor = _state.value.editor ?: return
        val source = loaded[editor.applicantId] ?: return
        pushUndo()
        editorRecipe = PhotoGeometry.applyRotation(source.stamp, editorRecipe, delta)
        showEditor(source.stamp, PhotoReviewState.DRAFT)
    }

    private fun setCrop(crop: PhotoCrop?) {
        val editor = _state.value.editor ?: return
        val source = loaded[editor.applicantId] ?: return
        pushUndo()
        editorRecipe = PhotoGeometry.validate(source.stamp, editorRecipe.copy(crop = crop))
        showEditor(source.stamp, PhotoReviewState.DRAFT)
    }

    private fun undo() {
        val previous = undo.removeLastOrNull() ?: return
        val editor = _state.value.editor ?: return
        val source = loaded[editor.applicantId] ?: return
        editorRecipe = previous
        showEditor(source.stamp, PhotoReviewState.DRAFT)
    }

    private fun reset() {
        val editor = _state.value.editor ?: return
        val source = loaded[editor.applicantId] ?: return
        pushUndo()
        editorRecipe = PhotoRecipe()
        showEditor(source.stamp, PhotoReviewState.DRAFT)
    }

    private fun persist(approve: Boolean, advance: Boolean) {
        val scope = _state.value.scope ?: return
        val editor = _state.value.editor ?: return
        val source = loaded[editor.applicantId] ?: return
        val key = PhotoKey(scope, editor.applicantId)
        val existing = _state.value.drafts[editor.applicantId]
            ?: PhotoDraft(key, source.stamp)
        val bound = PhotoDrafts.bindSource(existing, source.stamp)
        val drafted = PhotoDrafts.applyRecipe(bound, source.stamp, editorRecipe)
        val saved = if (approve) PhotoDrafts.approve(drafted, source.stamp) else drafted
        store.save(saved)
        refreshDrafts()
        previewCache.keys.filter { it.startsWith("${editor.applicantId}:") }.forEach { previewCache.remove(it) }
        if (advance) {
            closeEditor(abandon = false)
            openNext(needsReview = true, after = editor.applicantId)
        } else {
            showEditor(source.stamp, saved.review)
        }
    }

    private fun startScan() {
        val scope = _state.value.scope ?: return
        val targets = visibleApplicants()
        if (targets.isEmpty()) return
        scanJob?.cancel()
        scanJob = this.scope.launch {
            _state.update { it.copy(scan = PhotoScanProgress(0, targets.size), snack = null) }
            targets.forEachIndexed { index, card ->
                val key = PhotoKey(scope, card.id.value)
                when (val result = ensureSource(key)) {
                    is PhotoSourceResult.Ready -> {
                        val evidence = scanner.scan(result.source.bitmap)
                        suggestions[card.id.value] = PhotoSuggestionPolicy.suggest(evidence, result.source.stamp)
                    }
                    else -> suggestions[card.id.value] = PhotoSuggestion(
                        null,
                        PhotoSuggestionReason.NO_FACE,
                        safeToApply = false,
                    )
                }
                _state.update { it.copy(scan = PhotoScanProgress(index + 1, targets.size)) }
            }
            scanJob = null
            _state.update { it.copy(scan = null) }
            publish()
        }
    }

    private fun applySafe() {
        val scope = _state.value.scope ?: return
        suggestions.forEach { (id, suggestion) ->
            if (!suggestion.safeToApply) return@forEach
            val recipe = suggestion.recipe ?: return@forEach
            val source = loaded[id] ?: return@forEach
            val key = PhotoKey(scope, id)
            val existing = _state.value.drafts[id] ?: PhotoDraft(key, source.stamp)
            store.save(PhotoDrafts.applyRecipe(PhotoDrafts.bindSource(existing, source.stamp), source.stamp, recipe))
        }
        refreshDrafts()
        _state.update { it.copy(snack = "Safe suggestions saved as drafts") }
    }

    private fun clearCourse() {
        val scope = _state.value.scope ?: return
        store.clearCourse(scope)
        suggestions.clear()
        undo.clear()
        previewCache.clear()
        editorId = null
        editorRecipe = PhotoRecipe()
        _state.update { it.copy(editor = null, snack = "Course photo edits cleared") }
        refreshDrafts()
    }

    private fun prepareExport() {
        val drafts = readyDrafts()
        frozenExport = drafts
        val request = when {
            drafts.isEmpty() -> null
            drafts.size == 1 -> PhotoExportRequest(
                "image/jpeg",
                PhotoExport.fileName(drafts.single().key.applicantId),
            )
            else -> PhotoExportRequest("application/zip", "photo-corrections.zip")
        }
        _state.update { it.copy(exportRequest = request, exported = false) }
    }

    private fun prepareUpdate() {
        if (!allowDeskWrite) {
            _state.update { it.copy(snack = "Course ops cannot update applications") }
            return
        }
        val drafts = readyDrafts()
        if (drafts.isEmpty()) {
            _state.update { it.copy(snack = "No reviewed photo corrections to update") }
            return
        }
        frozenUpdate = drafts
        _state.update { it.copy(confirmUpdate = true, snack = null) }
    }

    private fun startUpdate() {
        if (!allowDeskWrite) {
            _state.update {
                it.copy(
                    confirmUpdate = false,
                    snack = "Course ops cannot update applications",
                )
            }
            return
        }
        val writer = deskWrite
        if (writer == null) {
            _state.update { it.copy(confirmUpdate = false, snack = "Photo update is not configured") }
            return
        }
        val items = frozenUpdate.ifEmpty { readyDrafts() }
        if (items.isEmpty()) {
            _state.update { it.copy(confirmUpdate = false) }
            return
        }
        stopAfterCurrent = false
        updateJob?.cancel()
        _state.update { it.copy(confirmUpdate = false, updating = true, updateProgress = 0 to items.size) }
        updateJob = scope.launch {
            for ((index, draft) in items.withIndex()) {
                val source = sourceOf(draft.key.applicantId)
                if (source == null || source.stamp != draft.source) {
                    store.save(draft.copy(write = PhotoWriteState.FAILED))
                    refreshDrafts()
                    _state.update {
                        it.copy(
                            updating = false,
                            updateProgress = null,
                            snack = "Source changed",
                        )
                    }
                    return@launch
                }
                try {
                    store.save(draft.copy(write = PhotoWriteState.SUBMITTING))
                } catch (_: Exception) {
                    _state.update {
                        it.copy(
                            updating = false,
                            updateProgress = null,
                            snack = "Could not save the photo update state",
                        )
                    }
                    return@launch
                }
                refreshDrafts()
                val jpeg = ByteArrayOutputStream().use { out ->
                    renderer.writeJpeg(source.bitmap, draft.recipe, out)
                    out.toByteArray()
                }
                val result = writer.submit(
                    draft.key.applicantId,
                    jpeg,
                    PhotoExport.fileName(draft.key.applicantId),
                )
                when (result) {
                    is PhotoDeskWriteResult.Committed -> {
                        invalidateSource(draft.key.applicantId)
                        loaded.remove(draft.key.applicantId)
                        previewCache.keys.filter { it.startsWith("${draft.key.applicantId}:") }
                            .forEach { previewCache.remove(it) }
                        val refreshed = sources.load(draft.key, true)
                        val newSource = (refreshed as? PhotoSourceResult.Ready)?.source
                        if (newSource != null) loaded[draft.key.applicantId] = newSource
                        store.save(
                            draft.copy(
                                source = newSource?.stamp ?: draft.source,
                                recipe = PhotoRecipe(),
                                review = PhotoReviewState.UNREVIEWED,
                                write = PhotoWriteState.COMMITTED,
                                output = newSource?.stamp,
                                editRevision = draft.editRevision + 1,
                            ),
                        )
                        refreshDrafts()
                        _state.update {
                            it.copy(
                                updateProgress = (index + 1) to items.size,
                                snack = result.message,
                            )
                        }
                    }
                    is PhotoDeskWriteResult.Incomplete -> {
                        store.save(draft.copy(write = PhotoWriteState.NOT_STARTED))
                        refreshDrafts()
                        _state.update {
                            it.copy(updating = false, updateProgress = null, snack = result.message)
                        }
                        return@launch
                    }
                    is PhotoDeskWriteResult.Failed -> {
                        store.save(draft.copy(write = PhotoWriteState.FAILED))
                        refreshDrafts()
                        _state.update {
                            it.copy(updating = false, updateProgress = null, snack = result.message)
                        }
                        return@launch
                    }
                    is PhotoDeskWriteResult.Unknown -> {
                        store.save(draft.copy(write = PhotoWriteState.UNKNOWN))
                        refreshDrafts()
                        _state.update {
                            it.copy(updating = false, updateProgress = null, snack = result.message)
                        }
                        return@launch
                    }
                }
                if (stopAfterCurrent) break
            }
            updateJob = null
            frozenUpdate = emptyList()
            stopAfterCurrent = false
            _state.update { it.copy(updating = false, updateProgress = null) }
        }
    }

    private fun runExport(uri: String) {
        val items = frozenExport
        if (items.isEmpty()) return
        exportJob = scope.launch {
            val stream = openOutput(uri)
            if (stream == null) {
                _state.update { it.copy(snack = "Could not write the export", exportRequest = null) }
                return@launch
            }
            stream.use { out ->
                val result = if (items.size == 1) {
                    val draft = items.single()
                    val source = sourceOf(draft.key.applicantId)
                    if (source == null || source.stamp != draft.source) {
                        ExportResult.Failed(0, "Source changed")
                    } else {
                        exporter.writeOne(source.bitmap, draft.recipe, out)
                        ExportResult.Completed(1)
                    }
                } else {
                    exporter.writeZip(items, { key -> sourceOf(key.applicantId) }, out) { done, total ->
                        _state.update { it.copy(exportProgress = done to total) }
                    }
                }
                _state.update {
                    it.copy(
                        exportRequest = null,
                        exportProgress = null,
                        exported = result is ExportResult.Completed,
                        snack = when (result) {
                            is ExportResult.Completed -> "Saved locally"
                            is ExportResult.Cancelled -> "Export incomplete"
                            is ExportResult.Failed -> result.message
                        },
                    )
                }
            }
        }
    }

    private fun closeEditor(abandon: Boolean) {
        if (!abandon) undo.clear()
        editorId = null
        _state.update { it.copy(editor = null) }
        publish()
    }

    private fun openNext(needsReview: Boolean, after: Int? = null) {
        val cards = visibleApplicants()
        val start = after?.let { id -> cards.indexOfFirst { it.id.value == id } } ?: -1
        val next = cards.drop(start + 1).firstOrNull { card ->
            val draft = _state.value.drafts[card.id.value]
            if (needsReview) draft?.review != PhotoReviewState.APPROVED else true
        } ?: cards.firstOrNull { card ->
            _state.value.drafts[card.id.value]?.review != PhotoReviewState.APPROVED
        }
        if (next != null) openEditor(next.id.value) else closeEditor(abandon = false)
    }

    private fun showEditor(stamp: PhotoStamp, review: PhotoReviewState) {
        _state.update { cur ->
            cur.copy(
                editor = cur.editor?.copy(
                    recipe = editorRecipe,
                    review = review,
                    canUndo = undo.isNotEmpty(),
                    sourceWidth = stamp.width,
                    sourceHeight = stamp.height,
                    compareOriginal = false,
                ),
            )
        }
        publish()
    }

    private fun pushUndo() {
        undo.addLast(editorRecipe)
        if (undo.size > 20) undo.removeFirst()
    }

    private fun refreshDrafts() {
        val scope = _state.value.scope ?: return
        _state.update { it.copy(drafts = store.drafts(scope).associateBy { draft -> draft.key.applicantId }) }
        publish()
    }

    private fun publish() {
        val cur = _state.value
        val stamps = loaded.mapValues { it.value.stamp }
        val visible = visibleApplicants().map { card ->
            val draft = cur.drafts[card.id.value]
            val stamp = stamps[card.id.value] ?: draft?.source
            val ready = draft != null && stamp != null && PhotoDrafts.isReady(draft, stamp)
            PhotoCardUi(
                applicant = card,
                label = reviewLabel(draft, suggestions[card.id.value], ready),
                reason = suggestions[card.id.value]?.reason,
                ready = ready,
            )
        }
        val readyCount = cur.drafts.values.count { draft ->
            val stamp = stamps[draft.key.applicantId] ?: draft.source
            PhotoDrafts.isReady(draft, stamp)
        }
        _state.update {
            it.copy(
                cards = visible,
                readyCount = readyCount,
                safeSuggestionCount = suggestions.values.count { suggestion -> suggestion.safeToApply },
            )
        }
    }

    private fun visibleApplicants(): List<ApplicantCard> {
        val cur = _state.value
        val scoped = deskScoped(cur.applicants, cur.gender, cur.seniority)
        val q = cur.search.trim()
        return scoped.filter { card ->
            val matchesQuery = q.isEmpty() ||
                card.displayName.contains(q, true) ||
                card.confNo?.value.orEmpty().contains(q, true)
            val draft = cur.drafts[card.id.value]
            val stamp = loaded[card.id.value]?.stamp ?: draft?.source
            val ready = draft != null && stamp != null && PhotoDrafts.isReady(draft, stamp)
            val needs = when {
                draft == null -> true
                draft.review == PhotoReviewState.SOURCE_CHANGED -> true
                draft.review == PhotoReviewState.UNREVIEWED -> true
                draft.review == PhotoReviewState.DRAFT -> true
                else -> !ready && draft.review != PhotoReviewState.APPROVED
            }
            val matchesFilter = when (cur.reviewFilter) {
                PhotoReviewFilter.ALL -> true
                PhotoReviewFilter.NEEDS_REVIEW -> needs
                PhotoReviewFilter.READY -> ready
                PhotoReviewFilter.UPDATED -> draft?.write == PhotoWriteState.COMMITTED
            }
            matchesQuery && matchesFilter
        }
    }

    private fun readyDrafts(): List<PhotoDraft> {
        val stamps = loaded.mapValues { it.value.stamp }
        return _state.value.drafts.values.filter { draft ->
            val stamp = stamps[draft.key.applicantId] ?: draft.source
            PhotoDrafts.isReady(draft, stamp)
        }
    }

    private suspend fun ensureSource(key: PhotoKey): PhotoSourceResult {
        loaded[key.applicantId]?.let { return PhotoSourceResult.Ready(it) }
        _state.update { it.copy(loadingIds = it.loadingIds + key.applicantId) }
        val result = sources.load(key, false)
        _state.update { it.copy(loadingIds = it.loadingIds - key.applicantId) }
        if (result is PhotoSourceResult.Ready) loaded[key.applicantId] = result.source
        return result
    }

    private suspend fun sourceOf(id: Int): PhotoSource? {
        loaded[id]?.let { return it }
        val scope = _state.value.scope ?: return null
        return (ensureSource(PhotoKey(scope, id)) as? PhotoSourceResult.Ready)?.source
    }

    private suspend fun rendered(id: Int, bitmap: Bitmap, recipe: PhotoRecipe): ImageBitmap {
        val key = "$id:${recipe.clockwise}:${recipe.crop}"
        previewCache[key]?.let { return it }
        val out = renderer.render(bitmap, recipe).asImageBitmap()
        previewCache[key] = out
        return out
    }

    private fun reviewLabel(draft: PhotoDraft?, suggestion: PhotoSuggestion?, ready: Boolean): String = when {
        draft?.write == PhotoWriteState.COMMITTED -> "Updated on desk"
        ready -> "Ready"
        draft?.review == PhotoReviewState.SOURCE_CHANGED -> "Source changed"
        draft?.review == PhotoReviewState.APPROVED && PhotoGeometry.isIdentity(draft.recipe) -> "Reviewed"
        draft?.review == PhotoReviewState.DRAFT -> "Draft"
        suggestion?.reason == PhotoSuggestionReason.NO_FACE -> "No face"
        suggestion?.reason == PhotoSuggestionReason.MULTIPLE_FACES -> "Multiple faces"
        suggestion?.reason == PhotoSuggestionReason.AMBIGUOUS -> "Needs review"
        suggestion != null -> "Needs review"
        else -> "Unreviewed"
    }
}
