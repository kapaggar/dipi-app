package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi

@Composable
fun LoginScreen(
    username: String,
    password: String,
    error: String?,
    loading: Boolean,
    remember: Boolean,
    hostLabel: String,
    onUser: (String) -> Unit,
    onPass: (String) -> Unit,
    onRemember: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    val c = LocalDipi.current
    Box(Modifier.fillMaxSize().background(c.background), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("DIPI STAFF", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Registrar desk", color = c.foreground, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                "Steam Deck · $hostLabel",
                color = c.muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            DeskField(username, onUser, Modifier.fillMaxWidth(), placeholder = "Username")
            DeskField(password, onPass, Modifier.fillMaxWidth(), placeholder = "Password", password = true)
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(remember, role = Role.Checkbox, onValueChange = onRemember)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(remember, onCheckedChange = onRemember)
                Text("Remember me on this Deck", color = c.foreground, fontSize = 15.sp)
            }
            if (error != null) {
                Text(error, color = c.hard, fontSize = 14.sp)
            }
            DeskButton(
                if (loading) "Signing in…" else "Sign in",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                primary = true,
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
            )
            Text(
                "Cookies stay on this device. Logout keeps remember-me. Erase all local data wipes everything.",
                color = c.muted,
                fontSize = 12.sp,
            )
        }
    }
}
