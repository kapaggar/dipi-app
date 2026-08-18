package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.derive.DeskCourse
import org.dhamma.dipi.staff.desktop.derive.DeskRail
import org.dhamma.dipi.staff.desktop.derive.DeskSection
import org.dhamma.dipi.staff.desktop.derive.deskBoardDay
import org.dhamma.dipi.staff.desktop.derive.deskDayChip
import org.dhamma.dipi.staff.desktop.derive.deskRoll
import org.dhamma.dipi.staff.desktop.derive.deskSyncLine
import org.dhamma.dipi.staff.desktop.derive.deskWaNumber
import org.dhamma.dipi.staff.desktop.state.DeskScreen
import org.dhamma.dipi.staff.desktop.state.DesktopController
import org.dhamma.dipi.staff.desktop.state.deskRailCounts
import org.dhamma.dipi.staff.desktop.theme.DipiTheme
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.SheetPayload
import java.time.Instant
import java.time.LocalDate

@Composable
fun AppRoot(vm: DesktopController, onOpenDoc: (SheetPayload.Document) -> Unit, onExit: () -> Unit) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.openDoc) {
        val doc = state.openDoc ?: return@LaunchedEffect
        onOpenDoc(doc)
        vm.consumeOpenDoc()
    }
    LaunchedEffect(state.snack) {
        if (state.snack != null) {
            kotlinx.coroutines.delay(4200)
            vm.consumeSnack()
        }
    }
    DipiTheme(dark = state.dark) {
        val c = LocalDipi.current
        Box(Modifier.fillMaxSize().background(c.background)) {
            Column(Modifier.fillMaxSize()) {
                if (state.session?.modeTest == true) {
                    Text(
                        "TEST MODE",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().background(c.accent).padding(8.dp),
                    )
                }
                if (state.offline || state.queuedCount > 0) {
                    Text(
                        if (state.offline) "Offline · ${state.queuedCount} queued" else "${state.queuedCount} queued to sync",
                        color = c.foreground,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().background(c.tint).padding(8.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
                    when (state.screen) {
                        DeskScreen.Login -> LoginScreen(
                            username = state.username,
                            password = state.password,
                            error = state.loginError,
                            loading = state.loginLoading,
                            remember = state.remember,
                            hostLabel = state.hostLabel,
                            onUser = vm::onUser,
                            onPass = vm::onPass,
                            onRemember = vm::onRemember,
                            onSubmit = vm::signIn,
                        )
                        DeskScreen.Centre -> state.session?.let { session ->
                            CentreScreen(
                                session = session,
                                courses = state.courses,
                                olderCourses = state.olderCourses,
                                loading = state.loading,
                                onPickCourse = vm::pickCourse,
                                onPickCentre = vm::pickCentre,
                                onSettings = vm::openSettings,
                            )
                        }
                        DeskScreen.Settings -> SettingsPane(
                            session = state.session,
                            dark = state.dark,
                            lastSync = state.lastSync,
                            queued = state.queuedCount,
                            offline = state.offline,
                            lotus = state.lotus,
                            versionName = state.versionName,
                            hostLabel = state.hostLabel,
                            onToggleTheme = vm::toggleTheme,
                            onToggleOffline = vm::toggleOffline,
                            onToggleLotus = vm::toggleLotus,
                            onLogout = vm::logout,
                            onFactoryReset = vm::factoryReset,
                            onBack = vm::back,
                        )
                        DeskScreen.Desk -> {
                            val session = state.session
                            val course = state.course
                            if (session != null && course != null) {
                                LaunchedEffect(course.id) { vm.ensureDesk() }
                                DeskHost(vm, state, session, course)
                            }
                        }
                    }
                }
            }
            if (state.confirmExit) {
                ExitDialog(onStay = vm::stay, onExit = onExit)
            }
            val snack = state.snack
            if (snack != null && state.screen != DeskScreen.Desk) {
                Text(
                    snack.text,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(if (snack.error) c.snackError else c.snack)
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun DeskHost(
    vm: DesktopController,
    state: org.dhamma.dipi.staff.desktop.state.DeskUiState,
    session: org.dhamma.dipi.staff.model.Session,
    course: org.dhamma.dipi.staff.model.Course,
) {
    val roll = deskRoll(state.rows)
    val flagsById = state.auditRows.associate { it.id to it.flags }
    val openApp: (org.dhamma.dipi.staff.model.ApplicantCard) -> Unit = { card ->
        vm.selectDeskApp(card)
        vm.setDeskSection(DeskSection.Applications)
    }
    Box(Modifier.fillMaxSize()) {
        DeskShell(
            section = state.deskSection,
            rail = DeskRail(
                userName = session.name,
                syncLine = deskSyncLine(state.lastSync, Instant.now(), state.offline, state.queuedCount),
                counts = deskRailCounts(state),
            ),
            course = DeskCourse(
                label = session.centres.firstOrNull()?.name ?: course.name,
                dates = listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" – ").ifBlank { course.name },
                dayChip = deskDayChip(course.start, LocalDate.now()),
            ),
            loading = state.loading,
            onSection = vm::setDeskSection,
            onSettings = vm::openSettings,
            onBack = vm::back,
        ) { section ->
            when (section) {
                DeskSection.Board -> BoardPane(
                    centreName = session.centres.firstOrNull()?.name ?: course.name,
                    dayLabel = deskBoardDay(course.start, LocalDate.now()),
                    roll = roll,
                    checkIns = state.checkIns,
                    flagged = state.auditRows,
                    callOutcomes = state.callState.filterValues { it.logged }.mapValues { it.value.outcome },
                    onGoto = vm::setDeskSection,
                    onExport = vm::openSheetExport,
                )
                DeskSection.CheckIn -> CheckInPane(
                    roll = roll,
                    checkIns = state.checkIns,
                    rooms = state.centreOps.rooms,
                    scan = state.deskScan,
                    filter = state.deskZeroFilter,
                    gender = state.deskGender,
                    seniority = state.deskSeniority,
                    onScan = vm::setDeskScan,
                    onFilter = vm::setDeskZeroFilter,
                    onGender = vm::setDeskGender,
                    onSeniority = vm::setDeskSeniority,
                    onOpen = vm::openDeskMark,
                )
                DeskSection.Audit -> AuditPane(
                    flagged = state.auditRows,
                    selectedCode = state.deskFinding,
                    onSelect = vm::selectDeskFinding,
                    onBatch = vm::runDeskBatch,
                    onOpen = openApp,
                )
                DeskSection.Calling -> CallingPane(
                    rows = state.rows,
                    outcomes = state.callState,
                    filter = state.callFilter,
                    onFilter = vm::setCallFilter,
                    onOutcome = vm::setCallState,
                    onDial = { card ->
                        vm.logCallAttempt(card)
                        card.mobile?.let { LinuxOpen.tel(it) }
                    },
                    onWhatsApp = { card ->
                        val wa = deskWaNumber(card.mobile) ?: return@CallingPane
                        vm.logCallAttempt(card)
                        LinuxOpen.uri("https://wa.me/$wa")
                    },
                    onNote = vm::setCallNote,
                    gender = state.deskGender,
                    seniority = state.deskSeniority,
                    onGender = vm::setDeskGender,
                    onSeniority = vm::setDeskSeniority,
                )
                DeskSection.Rooms -> RoomsPane(
                    roll = roll,
                    checkIns = state.checkIns,
                    rooms = state.centreOps.rooms,
                    pendingSync = org.dhamma.dipi.staff.desktop.derive.deskRoomSyncPending(state.checkIns),
                    syncBusy = state.roomSyncBusy,
                    pullBusy = state.roomPullBusy,
                    syncFailures = state.roomSync?.failures.orEmpty(),
                    onSyncRooms = vm::syncRooms,
                    onPullRooms = { vm.pullRooms(true) },
                )
                DeskSection.Applications -> ApplicationsPane(
                    rows = state.visible,
                    flagsById = flagsById,
                    selectedId = state.deskAppId,
                    onSelect = vm::selectDeskApp,
                    onChangeStatus = { card ->
                        vm.selectDeskApp(card)
                        vm.openStatusSheet()
                    },
                    onDial = LinuxOpen::tel,
                    onEdit = vm::openAppEdit,
                    counts = state.counts,
                    selectedStatuses = state.selected,
                    onToggleStatus = vm::toggleStatus,
                    sensitiveById = state.sensitiveById,
                    gender = state.deskGender,
                    seniority = state.deskSeniority,
                    onGender = vm::setDeskGender,
                    onSeniority = vm::setDeskSeniority,
                    dark = state.dark,
                    query = state.query,
                    onQuery = vm::onQuery,
                )
            }
        }
        val markCard = state.deskMarkId?.let { id -> state.rows.firstOrNull { it.id == id } }
        if (markCard != null) {
            CheckInDialog(
                card = markCard,
                record = vm.deskMarkRecord(),
                roll = roll,
                checkIns = state.checkIns,
                rooms = state.centreOps.rooms,
                roomOpen = state.deskRoomOpen,
                laundryOn = state.centreOps.laundry,
                valuablesOn = state.centreOps.valuables,
                groupsOn = state.centreOps.groups,
                onToggleRooms = vm::toggleDeskRoomPicker,
                onRoom = vm::setDeskRoom,
                onSeat = vm::setDeskSeat,
                onValuables = vm::toggleDeskValuables,
                onLaundry = vm::toggleDeskLaundry,
                onGroup = vm::setDeskGroup,
                onSave = vm::saveDeskMark,
                onUndo = vm::undoDeskMark,
                onClose = vm::closeDeskMark,
            )
        }
        val sheetCard = state.card
        if (state.sheetOpen && sheetCard != null) {
            StatusSheet(
                current = sheetCard.status.value,
                choices = state.statusChoices,
                pick = state.sheetPick,
                comment = state.sheetComment,
                custom = state.sheetCustom,
                onPick = vm::onSheetPick,
                onComment = vm::onSheetComment,
                onCustom = vm::onSheetCustom,
                onConfirm = vm::confirmStatus,
                onDismiss = vm::dismissStatusSheet,
            )
        }
        val sheetView = state.sheetView
        if (sheetView != null) {
            SheetViewerPane(
                title = sheetView.title,
                html = sheetView.html,
                loading = sheetView.loading,
                onClose = vm::closeSheet,
            )
        }
        val snack = state.snack
        if (snack != null) {
            Text(
                snack.text,
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .background(if (snack.error) LocalDipi.current.snackError else LocalDipi.current.snack)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun ExitDialog(onStay: () -> Unit, onExit: () -> Unit) {
    val c = LocalDipi.current
    Box(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.background(c.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Exit DIPI Staff?", color = c.foreground, fontSize = 20.sp)
            Text("Do you want to exit the app totally?", color = c.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeskButton("Stay", onStay)
                DeskButton("Exit", onExit, primary = true)
            }
        }
    }
}

object LinuxOpen {
    fun tel(number: String) {
        val tel = number.filter { it.isDigit() || it == '+' }
        uri("tel:$tel")
    }

    fun uri(url: String) {
        runCatching {
            ProcessBuilder("xdg-open", url).redirectErrorStream(true).start()
        }.onFailure {
            runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) }
        }
    }

    fun file(path: java.io.File) {
        runCatching {
            ProcessBuilder("xdg-open", path.absolutePath).redirectErrorStream(true).start()
        }.onFailure {
            runCatching { java.awt.Desktop.getDesktop().open(path) }
        }
    }
}
