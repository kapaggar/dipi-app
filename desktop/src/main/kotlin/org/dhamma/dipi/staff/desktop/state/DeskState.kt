package org.dhamma.dipi.staff.desktop.state

import org.dhamma.dipi.staff.desktop.derive.DeskSection
import org.dhamma.dipi.staff.desktop.derive.deskCallList
import org.dhamma.dipi.staff.desktop.derive.deskCheckedIn
import org.dhamma.dipi.staff.desktop.derive.deskFindingCount
import org.dhamma.dipi.staff.desktop.derive.deskOccupied
import org.dhamma.dipi.staff.desktop.derive.deskRoll
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantStatus
import org.dhamma.dipi.staff.model.CallRecord
import org.dhamma.dipi.staff.model.CentreOpsPrefs
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.FlushSnack
import org.dhamma.dipi.staff.model.RoomSyncResult
import org.dhamma.dipi.staff.model.SensitiveInfo
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.model.SheetPayload

enum class DeskScreen { Login, Centre, Desk, Settings }

data class SheetViewUi(
    val title: String,
    val loading: Boolean = true,
    val html: SheetPayload.Html? = null,
)

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
    val dark: Boolean = true,
    val lotus: Boolean = true,
    val offline: Boolean = false,
    val queuedById: Map<ApplicantId, String> = emptyMap(),
    val queuedCount: Int = 0,
    val statusChoices: List<String> = ApplicantStatus.SHEET_CHOICES,
    val sheetOpen: Boolean = false,
    val sheetPick: String = "",
    val sheetComment: String = "",
    val sheetCustom: String = "",
    val lastSync: String? = null,
    val snack: FlushSnack? = null,
    val centreOps: CentreOpsPrefs = CentreOpsPrefs(),
    val auditRows: List<ApplicantCard> = emptyList(),
    val callState: Map<ApplicantId, CallRecord> = emptyMap(),
    val callFilter: String = "To call",
    val deskSection: DeskSection = DeskSection.Board,
    val checkIns: Map<ApplicantId, CheckInRecord> = emptyMap(),
    val deskScan: String = "",
    val deskZeroFilter: String = "To arrive",
    val deskGender: String = "Both",
    val deskSeniority: String = "Both",
    val deskMarkId: ApplicantId? = null,
    val deskRoomOpen: Boolean = false,
    val deskFinding: String? = null,
    val deskAppId: ApplicantId? = null,
    val sensitiveById: Map<ApplicantId, SensitiveInfo> = emptyMap(),
    val roomSyncBusy: Boolean = false,
    val roomPullBusy: Boolean = false,
    val roomSync: RoomSyncResult? = null,
    val sheetView: SheetViewUi? = null,
    val openDoc: SheetPayload.Document? = null,
    val versionName: String = "2.0.0",
    val hostLabel: String = "dipi.vridhamma.org",
    val confirmExit: Boolean = false,
)

fun deskRailCounts(state: DeskUiState): Map<DeskSection, Int> = buildMap {
    val roll = deskRoll(state.rows)
    val applications = state.counts["All"] ?: state.rows.size
    if (applications > 0) put(DeskSection.Applications, applications)
    put(DeskSection.Audit, deskFindingCount(state.auditRows))
    put(DeskSection.Calling, deskCallList(roll).count { state.callState[it.id]?.logged != true })
    put(DeskSection.CheckIn, roll.count { !deskCheckedIn(it, state.checkIns) })
    val occupied = deskOccupied(roll, state.checkIns)
    put(DeskSection.Rooms, state.centreOps.rooms.count { it.code !in occupied })
}
