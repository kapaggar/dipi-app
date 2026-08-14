package org.dhamma.dipi.staff.ui

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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dhamma.dipi.staff.R
import org.dhamma.dipi.staff.applicants.CardScreen
import org.dhamma.dipi.staff.applicants.StatusSheet
import org.dhamma.dipi.staff.applicants.TodayScreen
import org.dhamma.dipi.staff.auth.LoginScreen
import org.dhamma.dipi.staff.course.CoursesScreen
import org.dhamma.dipi.staff.photos.PhotoReviewScreen
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.summary.DaySummaryScreen
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun DipiAppUi(vm: DeskViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val wide = LocalConfiguration.current.screenWidthDp >= 600

    LaunchedEffect(state.snack) {
        val snack = state.snack ?: return@LaunchedEffect
        snackbar.showSnackbar(snack.text)
        vm.consumeSnack()
    }

    DipiTheme(dark = state.dark) {
        val c = LocalDipi.current
        Box(
            Modifier
                .fillMaxSize()
                .background(c.background)
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
                if (state.offline || state.queuedCount > 0) {
                    Text(
                        text = stringResource(R.string.offline_banner, state.queuedCount),
                        color = c.foreground,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.tint)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
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
                        )
                        DeskScreen.Courses -> {
                            val session = state.session
                            if (session != null) {
                                CoursesScreen(session, state.courses, vm::pickCourse, vm::pickCentre)
                            }
                        }
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

@Composable
private fun DeskBody(vm: DeskViewModel, state: DeskUiState, wide: Boolean) {
    val course = state.course ?: return
    val centre = state.session?.centres?.firstOrNull()?.name.orEmpty()
    val showSplit = wide && state.screen == DeskScreen.Card

    BackHandler(enabled = state.screen != DeskScreen.Today) { vm.back() }

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
            )
            DeskScreen.Summary -> DaySummaryScreen(course, state.rows)
            DeskScreen.Settings -> SettingsScreen(
                session = state.session,
                dark = state.dark,
                lastSync = state.lastSync,
                queued = state.queuedCount,
                offline = state.offline,
                onToggleTheme = vm::toggleTheme,
                onToggleOffline = vm::toggleOffline,
                onLogout = vm::logout,
            )
            else -> today(Modifier.fillMaxSize())
        }
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
