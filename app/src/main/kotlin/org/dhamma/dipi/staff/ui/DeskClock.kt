package org.dhamma.dipi.staff.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLOCK_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.ENGLISH)

/** The receiver exists only while the visible desk is STARTED. No polling. */
internal fun interactiveScreen(context: Context): Flow<Boolean> = callbackFlow {
    val power = context.getSystemService(PowerManager::class.java)
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent.action != Intent.ACTION_SCREEN_OFF && power.isInteractive)
        }
    }
    ContextCompat.registerReceiver(
        context,
        receiver,
        IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        },
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    trySend(power.isInteractive)
    awaitClose { context.unregisterReceiver(receiver) }
}.distinctUntilChanged()

/** Restart at the current time; neither an invisible clock nor keep-alive catch-up. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun deskClockTicks(
    lifecycle: Lifecycle,
    interactive: Flow<Boolean>,
    now: () -> Long = System::currentTimeMillis,
): Flow<Long> = interactive.flatMapLatest { screenOn ->
    if (!screenOn) emptyFlow() else flow {
        while (currentCoroutineContext().isActive) {
            emit(now())
            delay(60_000L - Math.floorMod(now(), 60_000L))
        }
    }
}.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)

@Composable
internal fun deskClock(): String {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val ticks = remember(context, lifecycle) { deskClockTicks(lifecycle, interactiveScreen(context)) }
    val initial = remember { System.currentTimeMillis() }
    val time by ticks.collectAsState(initial)
    return remember(time) { CLOCK_FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())) }
}
