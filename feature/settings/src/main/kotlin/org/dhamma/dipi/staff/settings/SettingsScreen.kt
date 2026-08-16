package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.chipGradientColors
import org.dhamma.dipi.staff.ui.theme.deskCard

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
    skin: DeskSkin = DeskSkin.Steel,
    lotus: Boolean = true,
    onSkin: (DeskSkin) -> Unit = {},
    onToggleLotus: () -> Unit = {},
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

        SkinSwitcher(
            skin = skin,
            lotus = lotus,
            onSkin = onSkin,
            onToggleLotus = onToggleLotus,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

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

/**
 * The version-3 skin switcher: a rounded card with the `SKIN` kicker and one
 * button per skin — 18dp gradient chip, Barlow Condensed uppercase label,
 * selected = accent fill — plus the lotus toggle that gates the sign-in hero
 * and the desk watermark together.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkinSwitcher(
    skin: DeskSkin,
    lotus: Boolean,
    onSkin: (DeskSkin) -> Unit,
    onToggleLotus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDipi.current
    Column(
        modifier
            .fillMaxWidth()
            .deskCard(fill = c.field, border = c.hairline, elevation = 0.dp)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "SKIN",
            fontFamily = DipiMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            letterSpacing = 0.18.em,
            color = c.muted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DeskSkin.entries.forEach { s ->
                SkinButton(s, selected = s == skin, onClick = { onSkin(s) })
            }
        }
        Text(
            "Accent ramp, paper, neutrals and the lotus wash all move together.",
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = c.muted,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = lotus, role = Role.Switch, onValueChange = { onToggleLotus() }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Lotus watermark", fontSize = 14.sp, color = c.foreground, modifier = Modifier.weight(1f))
            Switch(checked = lotus, onCheckedChange = null)
        }
        Text(
            "Status colours stay put; they carry meaning, not mood.",
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = c.muted,
        )
    }
}

@Composable
private fun SkinButton(skin: DeskSkin, selected: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    val shape = DeskStyle.controlShape
    val chip = remember(skin) { skin.chipGradientColors() }
    Row(
        Modifier
            .height(40.dp)
            .background(if (selected) c.accent else Color.Transparent, shape)
            .border(1.dp, if (selected) c.accent else c.hairlineStrong, shape)
            .clickable(onClick = onClick)
            .padding(start = 11.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .size(18.dp)
                .background(Brush.linearGradient(0f to chip[0], 0.52f to chip[1], 1f to chip[2]))
                .border(1.dp, Color(0x1F000000)),
        )
        Text(
            skin.label.uppercase(),
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 0.07.em,
            color = if (selected) Color.White else c.foreground,
        )
    }
}
