package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
) {
    val c = LocalDipi.current
    Column(Modifier.background(c.background).padding(20.dp)) {
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
        Text("App version  1.0.0 (v1 · vertical 1)", color = c.muted, modifier = Modifier.padding(top = 8.dp))
        Button(onLogout, Modifier.padding(top = 24.dp)) { Text("Log out") }
    }
}
