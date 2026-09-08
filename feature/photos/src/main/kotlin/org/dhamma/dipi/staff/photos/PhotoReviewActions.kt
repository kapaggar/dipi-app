package org.dhamma.dipi.staff.photos

import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ConfSeniority
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.PhotoCrop
import org.dhamma.dipi.staff.model.PhotoScope

sealed class PhotoReviewAction {
    data class Open(
        val scope: PhotoScope,
        val applicants: List<ApplicantCard>,
        val focusApplicantId: Int? = null,
    ) : PhotoReviewAction()

    data class SetSearch(val query: String) : PhotoReviewAction()
    data class SetReviewFilter(val filter: PhotoReviewFilter) : PhotoReviewAction()
    data class SetGender(val gender: Gender?) : PhotoReviewAction()
    data class SetSeniority(val seniority: ConfSeniority?) : PhotoReviewAction()
    data class SelectApplicant(val applicantId: Int) : PhotoReviewAction()
    data class Rotate(val delta: Int) : PhotoReviewAction()
    data class SetCrop(val crop: PhotoCrop?) : PhotoReviewAction()
    data object Undo : PhotoReviewAction()
    data object Reset : PhotoReviewAction()
    data object SaveDraft : PhotoReviewAction()
    data object Approve : PhotoReviewAction()
    data object KeepAndNext : PhotoReviewAction()
    data object ApproveSelected : PhotoReviewAction()
    data object ToggleCompare : PhotoReviewAction()
    data object CloseEditor : PhotoReviewAction()
    data object Scan : PhotoReviewAction()
    data object CancelScan : PhotoReviewAction()
    data object ApplySafeSuggestions : PhotoReviewAction()
    data object ClearCourse : PhotoReviewAction()
    data object Close : PhotoReviewAction()
    data object PrepareExport : PhotoReviewAction()
    data class ExportTo(val uri: String) : PhotoReviewAction()
    data object CancelExport : PhotoReviewAction()
    data object PrepareUpdate : PhotoReviewAction()
    data object StartUpdate : PhotoReviewAction()
    data object CancelUpdate : PhotoReviewAction()
    data object StopAfterCurrent : PhotoReviewAction()
    data object DismissNotice : PhotoReviewAction()
}
