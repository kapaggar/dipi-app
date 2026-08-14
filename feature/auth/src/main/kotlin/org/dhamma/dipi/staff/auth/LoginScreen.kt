package org.dhamma.dipi.staff.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@Composable
fun LoginScreen(
    username: String,
    password: String,
    error: String?,
    loading: Boolean,
    onUser: (String) -> Unit,
    onPass: (String) -> Unit,
    onSubmit: () -> Unit,
    remember: Boolean = false,
    onRemember: (Boolean) -> Unit = {},
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("DIPI Staff", fontFamily = DipiCondensed, fontSize = 32.sp, color = c.foreground)
        Text("Centre admin desk", color = c.muted, fontSize = 14.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(username, onUser, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password,
            onPass,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = remember, role = Role.Checkbox, onValueChange = onRemember)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Checkbox(checked = remember, onCheckedChange = null)
            Text("Remember me", modifier = Modifier.padding(start = 8.dp), color = c.foreground, fontSize = 14.sp)
        }
        Text("Your centre is read from your account after sign-in.", color = c.muted, fontSize = 13.sp)
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = c.hard, fontSize = 14.sp)
        }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSubmit, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "Signing in…" else "Sign in")
        }
    }
}
