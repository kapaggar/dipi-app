package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun SettingsScreen(
    session: Session?,
    dark: Boolean,
    lastSync: String?,
    queued: Int,
    offline: Boolean,
    onToggleTheme: () -> Unit,
    onToggleOffline: () -> Unit = {},
    onLogout: () -> Unit,
    onFactoryReset: () -> Unit = {},
    appVersion: String = "",
) {
    val c = LocalDipi.current
    var confirmReset by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Settings", fontFamily = DipiCondensed, fontSize = 22.sp)
        if (session?.modeTest == true) {
            Text(
                "TEST MODE — sandbox. Status changes hit the mock (or a sandbox host). The strip stays on every screen.",
                color = c.muted,
                modifier = Modifier.padding(vertical = 12.dp),
            )
        }
        TextButton(onToggleTheme) { Text(if (dark) "Theme: Dark" else "Theme: Light") }
        TextButton(onToggleOffline) { Text(if (offline) "Simulate offline: on" else "Simulate offline: off") }
        Text("Signed in  ${session?.displayName ?: "—"} · ${session?.centres?.firstOrNull()?.name ?: ""}", modifier = Modifier.padding(top = 8.dp))
        Text(
            if (offline) "Offline · $queued changes queued" else "Last synced ${lastSync ?: "just now"}",
            color = c.muted,
        )
        Text(
            "App version  ${appVersion.ifBlank { "—" }}",
            color = c.muted,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onLogout, Modifier.padding(top = 24.dp)) { Text("Log out") }
        TextButton(onClick = { confirmReset = true }, Modifier.padding(top = 8.dp)) {
            Text("Erase all local data", color = c.hard)
        }
        Text(
            "Removes the saved password, session cookie, course cache, and queued status changes from this tablet.",
            color = c.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Erase everything on this tablet?") },
            text = {
                Text("This is a factory reset of the app. You will need to sign in again.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onFactoryReset()
                    },
                ) { Text("Erase") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}
