package org.dhamma.dipi.staff.photos

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoDraft
import org.dhamma.dipi.staff.model.PhotoRecipe
import org.dhamma.dipi.staff.model.PhotoReviewState
import org.dhamma.dipi.staff.model.PhotoScope
import org.dhamma.dipi.staff.model.PhotoSuggestionReason

enum class PhotoReviewFilter { ALL, NEEDS_REVIEW, READY, UPDATED }

data class PhotoScanProgress(val completed: Int, val total: Int)

data class PhotoExportRequest(val mime: String, val name: String)

data class PhotoEditorUi(
    val applicantId: Int,
    val name: String,
    val recipe: PhotoRecipe,
    val review: PhotoReviewState,
    val compareOriginal: Boolean = false,
    val canUndo: Boolean = false,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val missing: Boolean = false,
    val unsupported: Boolean = false,
)

data class PhotoCardUi(
    val applicant: ApplicantCard,
    val label: String,
    val reason: PhotoSuggestionReason? = null,
    val ready: Boolean = false,
)

data class PhotoReviewUiState(
    val scope: PhotoScope? = null,
    val applicants: List<ApplicantCard> = emptyList(),
    val drafts: Map<Int, PhotoDraft> = emptyMap(),
    val cards: List<PhotoCardUi> = emptyList(),
    val search: String = "",
    val reviewFilter: PhotoReviewFilter = PhotoReviewFilter.ALL,
    val gender: Gender? = null,
    val seniority: ConfSeniority? = null,
    val selectedApplicantId: Int? = null,
    val editor: PhotoEditorUi? = null,
    val loadingIds: Set<Int> = emptySet(),
    val errorById: Map<Int, String> = emptyMap(),
    val scan: PhotoScanProgress? = null,
    val exportProgress: Pair<Int, Int>? = null,
    val exportRequest: PhotoExportRequest? = null,
    val updateProgress: Pair<Int, Int>? = null,
    val confirmUpdate: Boolean = false,
    val updating: Boolean = false,
    val allowDeskWrite: Boolean = true,
    val readyCount: Int = 0,
    val notice: String? = null,
    val snack: String? = null,
    val confirmClear: Boolean = false,
    val safeSuggestionCount: Int = 0,
    val exported: Boolean = false,
)
