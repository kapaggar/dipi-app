package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.Session

@Composable
fun SettingsPane(
    session: Session?,
    dark: Boolean,
    lastSync: String?,
    queued: Int,
    offline: Boolean,
    lotus: Boolean,
    versionName: String,
    hostLabel: String,
    onToggleTheme: () -> Unit,
    onToggleOffline: () -> Unit,
    onToggleLotus: () -> Unit,
    onLogout: () -> Unit,
    onFactoryReset: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxSize().background(c.background).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", color = c.foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(session?.displayName ?: "Signed out", color = c.muted)
        Text("Host $hostLabel · version $versionName", color = c.muted, fontSize = 13.sp)
        Text("Last sync ${lastSync ?: "never"} · $queued queued", color = c.muted, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeskButton(if (dark) "OLED dark" else "Light", onToggleTheme, primary = dark)
            DeskButton(if (lotus) "Lotus on" else "Lotus off", onToggleLotus)
            DeskButton(if (offline) "Simulate offline" else "Online", onToggleOffline, primary = offline)
        }
        Text(
            "Remember-me stays after logout. Erase all local data wipes cookies, remember-me, worklist cache, outbox, and sheet files. ID documents are never written to disk.",
            color = c.muted,
            fontSize = 13.sp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeskButton("Back", onBack)
            DeskButton("Log out", onLogout)
            DeskButton("Erase all local data", onFactoryReset)
        }
    }
}
