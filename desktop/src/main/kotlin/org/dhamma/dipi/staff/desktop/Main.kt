package org.dhamma.dipi.staff.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.dhamma.dipi.staff.desktop.data.DesktopNetwork
import org.dhamma.dipi.staff.desktop.data.DesktopRepository
import org.dhamma.dipi.staff.desktop.data.DesktopStore
import org.dhamma.dipi.staff.desktop.state.DesktopController
import org.dhamma.dipi.staff.desktop.ui.AppRoot
import org.dhamma.dipi.staff.desktop.ui.LinuxOpen

fun main(args: Array<String>) {
    val config = DesktopConfig.fromArgs(args)
    config.dataDir.mkdirs()
    val store = DesktopStore(config.dataDir)
    val clients = DesktopNetwork.create(config, store)
    val repo = DesktopRepository(config, store, clients)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val vm = DesktopController(repo, store, scope, config)
    application {
        val windowState = rememberWindowState(
            width = DesktopConfig.DECK_WIDTH.dp,
            height = DesktopConfig.DECK_HEIGHT.dp,
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "DIPI Staff",
            icon = painterResource("icons/dipi-staff.png"),
            state = windowState,
            undecorated = config.deckFullscreen,
            onKeyEvent = { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                    vm.back()
                    true
                } else {
                    false
                }
            },
        ) {
            AppRoot(
                vm = vm,
                onOpenDoc = { LinuxOpen.file(it.file) },
                onExit = ::exitApplication,
            )
        }
    }
}
