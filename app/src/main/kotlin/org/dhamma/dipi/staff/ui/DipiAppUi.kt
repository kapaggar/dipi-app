package org.dhamma.dipi.staff.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dhamma.dipi.staff.BuildConfig
import org.dhamma.dipi.staff.R
import org.dhamma.dipi.staff.applicants.AuditScreen
import org.dhamma.dipi.staff.applicants.CallingScreen
import org.dhamma.dipi.staff.applicants.CardScreen
import org.dhamma.dipi.staff.applicants.StatusSheet
import org.dhamma.dipi.staff.applicants.TodayScreen
import org.dhamma.dipi.staff.applicants.ZeroDayScreen
import org.dhamma.dipi.staff.auth.LoginScreen
import org.dhamma.dipi.staff.course.AdvancedSearchScreen
import org.dhamma.dipi.staff.course.CentreOpsScreen
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.course.CourseHubLive
import org.dhamma.dipi.staff.course.CourseHubScreen
import org.dhamma.dipi.staff.course.DeskActionScreen
import org.dhamma.dipi.staff.course.RoomsScreen
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.desk.AuditPane
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.desk.CallingPane
import org.dhamma.dipi.staff.desk.CheckInDialog
import org.dhamma.dipi.staff.desk.CheckInPane
import org.dhamma.dipi.staff.desk.DeskRail
import org.dhamma.dipi.staff.desk.DeskCourse
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.DeskShell
import org.dhamma.dipi.staff.desk.DeskSnackbar
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.desk.SheetViewerPane
import org.dhamma.dipi.staff.desk.deskRoll
import org.dhamma.dipi.staff.desk.deskWaNumber
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.photos.PhotoReviewScreen
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.summary.DaySummaryScreen
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.phoneWash

@Composable
fun DipiAppUi(vm: DeskViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    // The v2 desk is designed at 1240×844; below ~1100dp the phone flow keeps serving.
    val deskWide = LocalConfiguration.current.screenWidthDp >= 1100
    val deskActive = deskWide && state.screen == DeskScreen.CourseHub &&
        state.session != null && state.course != null

    // One-shot: a fetched PDF/Excel goes to the system viewer via FileProvider.
    val docContext = LocalContext.current
    LaunchedEffect(state.openDoc) {
        val doc = state.openDoc ?: return@LaunchedEffect
        openSheetDocument(docContext, doc) { message -> vm.deskNote(message) }
        vm.consumeOpenDoc()
    }

    LaunchedEffect(state.snack, deskActive) {
        val snack = state.snack ?: return@LaunchedEffect
        if (deskActive) {
            // The desk renders its own bottom-left snackbar; just time it out.
            kotlinx.coroutines.delay(4200)
        } else {
            snackbar.showSnackbar(snack.text)
        }
        vm.consumeSnack()
    }

    DipiTheme(dark = state.dark) {
        val c = LocalDipi.current
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background)
                // Version-3 ambient wash behind every phone screen; the desk
                // paints its own frame (with its own washes) over it.
                .phoneWash(c.accent)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (state.session?.modeTest == true) {
                    Text(
                        text = stringResource(R.string.test_mode_banner),
                        color = androidx.compose.ui.graphics.Color.White,
                        fontFamily = DipiCondensed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.accent)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                SyncBannerStrips(
                    offline = state.offline,
                    queued = state.queuedCount,
                    lastTryAtMs = state.lastSyncAttemptAt,
                    onRetry = vm::retrySync,
                )
                Box(Modifier.weight(1f)) {
                    val canBack = state.screen != DeskScreen.Login && state.screen != DeskScreen.Centre
                    val activity = LocalContext.current as? Activity
                    var confirmExit by remember { mutableStateOf(false) }
                    BackHandler {
                        if (canBack) vm.back() else confirmExit = true
                    }
                    if (confirmExit) {
                        ExitAppDialog(
                            onStay = { confirmExit = false },
                            onExit = { activity?.finish() },
                        )
                    }
                    when (state.screen) {
                        DeskScreen.Login -> LoginScreen(
                            username = state.username,
                            password = state.password,
                            error = state.loginError,
                            loading = state.loginLoading,
                            onUser = vm::onUser,
                            onPass = vm::onPass,
                            onSubmit = vm::signIn,
                            remember = state.remember,
                            onRemember = vm::onRemember,
                            skin = state.skin,
                            lotus = state.lotus,
                        )
                        DeskScreen.Centre -> {
                            val session = state.session
                            if (session != null) {
                                CentreScreen(
                                    session,
                                    state.courses,
                                    vm::pickCourse,
                                    vm::pickCentre,
                                    vm::openSettings,
                                    vm::openLater,
                                    onCentreOps = vm::openCentreOps,
                                    onAdvancedSearch = vm::openAdvancedSearch,
                                    lotus = state.lotus,
                                    olderCourses = state.olderCourses,
                                )
                            }
                        }
                        DeskScreen.Search -> {
                            val cid = state.session?.centres?.firstOrNull()?.id?.value ?: 0
                            AdvancedSearchScreen(
                                rows = state.searchRows,
                                onOpen = vm::openSearchResult,
                                onOpenDesk = { vm.openLater("Advanced Search", "search-app/$cid") },
                                onBack = vm::back,
                            )
                        }
                        DeskScreen.CourseHub -> {
                            val session = state.session
                            val course = state.course
                            if (session != null && course != null && deskWide) {
                                LaunchedEffect(course.id) { vm.ensureDesk() }
                                DeskHost(vm, state, session, course)
                            } else if (session != null && course != null) {
                                // Silent worklist prefetch so the hub's count chips light up.
                                LaunchedEffect(course.id) { vm.ensureDesk() }
                                CourseHubScreen(
                                    course = course,
                                    centreName = session.centres.firstOrNull()?.name.orEmpty(),
                                    counts = courseHubCounts(state),
                                    onBack = vm::back,
                                    onSettings = vm::openSettings,
                                    onApplications = vm::openApplications,
                                    onSummary = vm::openSummary,
                                    onPhotos = vm::openPhotos,
                                    onAudit = vm::openAudit,
                                    onCalling = vm::openCalling,
                                    onZeroDay = vm::openZeroDay,
                                    onCentreOps = vm::openCentreOps,
                                    onLater = vm::openLater,
                                )
                            }
                        }
                        DeskScreen.DeskAction -> {
                            val action = state.deskAction
                            if (action != null) {
                                DeskActionScreen(action.title, action.route, vm::back)
                            }
                        }
                        DeskScreen.ZeroDay -> {
                            val course = state.course
                            if (course != null) {
                                ZeroDayScreen(
                                    course = course,
                                    rows = state.rows,
                                    prefs = state.centreOps,
                                    drafts = state.zeroDayDrafts,
                                    onSeating = vm::setZeroDaySeating,
                                    onLaundry = vm::setZeroDayLaundry,
                                    onValuables = vm::setZeroDayValuables,
                                    onRoom = vm::openRoomsFromZeroDay,
                                    onMarkAttended = vm::markAttended,
                                    onOpen = vm::openCard,
                                    onBack = vm::back,
                                    pendingRoomSync = deskRoomSyncPending(state.checkIns),
                                    roomSyncBusy = state.roomSyncBusy,
                                    roomPullBusy = state.roomPullBusy,
                                    onSyncRooms = vm::syncRooms,
                                    onPullRooms = vm::pullRooms,
                                )
                            }
                        }
                        DeskScreen.Audit -> AuditScreen(
                            rows = state.auditRows,
                            onOpen = vm::openCard,
                            onBack = vm::back,
                        )
                        DeskScreen.Calling -> {
                            val context = LocalContext.current
                            CallingScreen(
                                rows = state.rows,
                                callState = state.callState.mapValues { it.value.outcome },
                                filter = state.callFilter,
                                onFilter = vm::setCallFilter,
                                onCallState = vm::setCallState,
                                onDial = { number ->
                                    val tel = number.filter { it.isDigit() || it == '+' }
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
                                },
                                onOpen = vm::openCard,
                                onBack = vm::back,
                            )
                        }
                        DeskScreen.Rooms -> RoomsScreen(
                            rooms = state.centreOps.rooms,
                            genderFilter = state.roomsGender,
                            layout = state.centreOps.roomLayout,
                            onColumns = vm::setRoomColumns,
                            onPick = vm::pickRoom,
                            onBack = vm::back,
                        )
                        DeskScreen.CentreOps -> CentreOpsScreen(
                            prefs = state.centreOps,
                            onToggleLaundry = vm::toggleLaundry,
                            onToggleValuables = vm::toggleValuables,
                            onToggleGroups = vm::toggleGroups,
                            onOpenRooms = vm::openRoomsFromCentreOps,
                            onBack = vm::back,
                        )
                        DeskScreen.Settings -> SettingsPane(vm, state)
                        else -> DeskBody(vm, state, wide)
                    }
                }
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(androidx.compose.ui.Alignment.BottomCenter),
            ) { data ->
                val err = state.snack?.error == true ||
                    data.visuals.message.startsWith("Please") ||
                    data.visuals.message.startsWith("Unrecognized") ||
                    data.visuals.message.startsWith("No fixed")
                Snackbar(
                    snackbarData = data,
                    containerColor = if (err) c.snackError else c.snack,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

/**
 * The v2 desk: shell + section panes, with the mark-attended dialog, the
 * status sheet and the desk snackbar layered above the whole frame.
 */
@Composable
private fun DeskHost(
    vm: DeskViewModel,
    state: DeskUiState,
    session: org.dhamma.dipi.staff.model.Session,
    course: org.dhamma.dipi.staff.model.Course,
) {
    val context = LocalContext.current
    val dial: (String) -> Unit = { number ->
        val tel = number.filter { it.isDigit() || it == '+' }
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
    }
    val roll = deskRoll(state.rows)
    val flagsById = remember(state.auditRows) { state.auditRows.associate { it.id to it.flags } }
    val openApp: (org.dhamma.dipi.staff.model.ApplicantCard) -> Unit = { card ->
        vm.selectDeskApp(card)
        vm.setDeskSection(DeskSection.Applications)
    }

    Box(Modifier.fillMaxSize()) {
        DeskShell(
            section = state.deskSection,
            rail = deskRail(state, session),
            course = deskCourse(session, course),
            clock = deskClock(),
            onSection = vm::setDeskSection,
            loading = state.loading,
            lotus = state.lotus,
        ) { section ->
            when (section) {
                DeskSection.Board -> BoardPane(
                    roll = roll,
                    checkIns = state.checkIns,
                    flagged = state.auditRows,
                    callOutcomes = state.callState
                        .filterValues { it.logged }
                        .mapValues { it.value.outcome },
                    onGoto = vm::setDeskSection,
                    onExport = vm::openSheet,
                )
                DeskSection.CheckIn -> CheckInPane(
                    roll = roll,
                    checkIns = state.checkIns,
                    rooms = state.centreOps.rooms,
                    scan = state.deskScan,
                    filter = state.deskZeroFilter,
                    flaggedIds = flagsById.keys,
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
                    roll = roll,
                    outcomes = state.callState,
                    filter = state.callFilter,
                    onFilter = vm::setCallFilter,
                    onOutcome = vm::setCallState,
                    onDial = { card ->
                        vm.logCallAttempt(card)
                        card.mobile?.let(dial)
                    },
                    onWhatsApp = { card ->
                        val wa = deskWaNumber(card.mobile) ?: return@CallingPane
                        vm.logCallAttempt(card)
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$wa")),
                        )
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
                    layout = state.centreOps.roomLayout,
                    pendingSync = deskRoomSyncPending(state.checkIns),
                    syncBusy = state.roomSyncBusy,
                    pullBusy = state.roomPullBusy,
                    syncFailures = state.roomSync?.failures.orEmpty(),
                    onSyncRooms = vm::syncRooms,
                    onPullRooms = vm::pullRooms,
                )
                DeskSection.Applications -> ApplicationsPane(
                    rows = state.visible,
                    flagsById = flagsById,
                    selectedId = state.deskAppId,
                    onSelect = vm::selectDeskApp,
                    onChangeStatus = { card ->
                        vm.selectDeskApp(card)
                        vm.openSheet()
                    },
                    onDial = dial,
                    onEdit = vm::openAppEdit,
                    loadPhoto = vm::loadPhoto,
                    counts = state.counts,
                    selectedStatuses = state.selected,
                    onToggleStatus = vm::toggleStatus,
                    sensitiveById = state.sensitiveById,
                    gender = state.deskGender,
                    seniority = state.deskSeniority,
                    onGender = vm::setDeskGender,
                    onSeniority = vm::setDeskSeniority,
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
                onDismiss = vm::dismissSheet,
            )
        }

        // The sheet viewer overlays the whole desk frame (rail included) the
        // way the dialogs overlay the shell; vm.back() closes it first, so
        // the DeskScreen back stack underneath is untouched.
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
            DeskSnackbar(
                snack.text,
                error = snack.error,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart),
            )
        }
    }
}

/** Count chips on the phone course hub — the same derived numbers as the desk rail. */
private fun courseHubCounts(state: DeskUiState): Map<CourseHubLive, Int> {
    val rail = deskRailCounts(state)
    return mapOf(
        CourseHubLive.Applications to (rail[DeskSection.Applications] ?: 0),
        CourseHubLive.ZeroDay to (rail[DeskSection.CheckIn] ?: 0),
        CourseHubLive.Audit to (rail[DeskSection.Audit] ?: 0),
        CourseHubLive.Calling to (rail[DeskSection.Calling] ?: 0),
    )
}

private fun deskRail(
    state: DeskUiState,
    session: org.dhamma.dipi.staff.model.Session,
): DeskRail = DeskRail(
    userName = session.name,
    syncLine = deskSyncLine(state.lastSync, java.time.Instant.now(), state.offline, state.queuedCount),
    counts = deskRailCounts(state),
)

/** Course identity for the 52dp top bar (moved out of the rail). */
private fun deskCourse(
    session: org.dhamma.dipi.staff.model.Session,
    course: org.dhamma.dipi.staff.model.Course,
): DeskCourse {
    val dates = listOf(course.start, course.end).filter { it.isNotBlank() }.joinToString(" – ")
    return DeskCourse(
        dates = dates.ifBlank { course.name },
        dayChip = deskDayChip(course.start, java.time.LocalDate.now()),
    )
}

/** "Wed 2 Sep · 09:41", ticking once a minute. */
@Composable
private fun deskClock(): String {
    val fmt = remember {
        java.time.format.DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", java.util.Locale.ENGLISH)
    }
    val clock = androidx.compose.runtime.produceState(java.time.LocalDateTime.now().format(fmt)) {
        while (true) {
            value = java.time.LocalDateTime.now().format(fmt)
            kotlinx.coroutines.delay(60_000L - (System.currentTimeMillis() % 60_000L))
        }
    }
    return clock.value
}

@Composable
private fun DeskBody(vm: DeskViewModel, state: DeskUiState, wide: Boolean) {
    val course = state.course
    if (course == null) {
        // A card can open with no course picked (Advanced Search across the
        // cache); the detail pane needs no course context.
        if (state.screen == DeskScreen.Card) CardPane(vm, state)
        return
    }
    val centre = state.session?.centres?.firstOrNull()?.name.orEmpty()
    val showSplit = wide && state.screen == DeskScreen.Card

    val today: @Composable (Modifier) -> Unit = { mod ->
        Box(mod) {
            TodayScreen(
                course = course,
                centreName = centre,
                query = state.query,
                onQuery = vm::onQuery,
                counts = state.counts,
                selected = state.selected,
                onToggleStatus = vm::toggleStatus,
                rows = state.visible,
                queued = state.queuedById,
                loading = state.loading,
                dark = state.dark,
                onOpen = vm::openCard,
                onSummary = vm::openSummary,
                onPhotos = vm::openPhotos,
                onSettings = vm::openSettings,
                onRefresh = vm::refresh,
            )
        }
    }

    if (showSplit) {
        Row(Modifier.fillMaxSize()) {
            today(Modifier.weight(0.42f).fillMaxHeight())
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(LocalDipi.current.hairline),
            )
            Box(Modifier.weight(0.58f).fillMaxHeight()) {
                CardPane(vm, state)
            }
        }
    } else {
        when (state.screen) {
            DeskScreen.Today -> today(Modifier.fillMaxSize())
            DeskScreen.Card -> CardPane(vm, state)
            DeskScreen.Photos -> PhotoReviewScreen(
                people = state.rows,
                suggestions = state.photos,
                edits = state.edits,
                filter = state.photoFilter,
                onFilter = vm::setPhotoFilter,
                onRotate = vm::rotatePhoto,
                onCrop = vm::cropPhoto,
                onDone = vm::markPhotoDone,
                onUpload = vm::uploadPhotos,
                pendingUploads = vm.pendingUploads(),
                loadPhoto = vm::loadPhoto,
            )
            DeskScreen.Summary -> DaySummaryScreen(course, state.rows)
            DeskScreen.Settings -> SettingsPane(vm, state)
            else -> today(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SettingsPane(vm: DeskViewModel, state: DeskUiState) {
    SettingsScreen(
        session = state.session,
        dark = state.dark,
        lastSync = state.lastSync,
        queued = state.queuedCount,
        offline = state.offline,
        onToggleTheme = vm::toggleTheme,
        onToggleOffline = vm::toggleOffline,
        onLogout = vm::logout,
        onFactoryReset = vm::factoryReset,
        appVersion = BuildConfig.VERSION_NAME,
        skin = state.skin,
        lotus = state.lotus,
        onSkin = vm::setSkin,
        onToggleLotus = vm::toggleLotus,
    )
}

/**
 * Hands a streamed sheet (cacheDir/sheets only) to the system viewer:
 * ACTION_VIEW with a FileProvider uri first, a chooser as the fallback, and
 * the desk snackbar when nothing on the device can open the type.
 */
private fun openSheetDocument(
    context: android.content.Context,
    doc: SheetPayload.Document,
    onNoViewer: (String) -> Unit,
) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        doc.file,
    )
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, doc.mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(view) }.onFailure {
        runCatching { context.startActivity(Intent.createChooser(view, doc.title)) }
            .onFailure { onNoViewer("No app on this device can open ${doc.title}") }
    }
}

@Composable
private fun CardPane(vm: DeskViewModel, state: DeskUiState) {
    val card = state.card ?: return
    CardScreen(
        card = card,
        photoNote = vm.photoNote(card),
        dark = state.dark,
        onChangeStatus = vm::openSheet,
        onPhoto = vm::openPhotos,
    )
    if (state.sheetOpen) {
        StatusSheet(
            current = card.status.value,
            choices = state.statusChoices,
            pick = state.sheetPick,
            comment = state.sheetComment,
            custom = state.sheetCustom,
            onPick = vm::onSheetPick,
            onComment = vm::onSheetComment,
            onCustom = vm::onSheetCustom,
            onConfirm = vm::confirmStatus,
            onDismiss = vm::dismissSheet,
        )
    }
}

@Composable
fun ExitAppDialog(
    onStay: () -> Unit,
    onExit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text("Exit DIPI Staff?") },
        text = { Text("Do you want to exit the app totally?") },
        confirmButton = {
            TextButton(onClick = onExit) { Text("Exit") }
        },
        dismissButton = {
            TextButton(onClick = onStay) { Text("Stay") }
        },
    )
}
