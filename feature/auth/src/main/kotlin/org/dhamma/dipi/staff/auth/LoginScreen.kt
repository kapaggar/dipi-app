package org.dhamma.dipi.staff.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.R
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LoginLotusRelief
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Sign-in, compact (owner feedback 2026-08-16): the form lives in a
 * centred DeskStyle card capped at 380dp — never full-screen-width fields —
 * floating over the lotus relief — the circular mark at readable opacity
 * with a vertical gradient fade (see [LoginLotusRelief]). Remember-me, the
 * verbatim server error and the loading state stay.
 */
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
    skin: DeskSkin = DeskSkin.Steel,
    lotus: Boolean = true,
) {
    val c = LocalDipi.current
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (lotus) {
            LoginLotusRelief()
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .deskCard(fill = c.background.copy(alpha = 0.92f), border = c.hairlineStrong)
                    .padding(horizontal = 24.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Box(
                        Modifier
                            .clip(DeskStyle.controlShape)
                            .background(c.background.copy(alpha = 0.78f))
                            .border(1.dp, c.accent, DeskStyle.controlShape)
                            .padding(7.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.lotus_mark),
                            contentDescription = "DIPI",
                            modifier = Modifier.size(46.dp),
                        )
                    }
                    Text(
                        "DIPI Staff",
                        fontFamily = DipiCondensed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.01).em,
                        color = c.foreground,
                    )
                    Text("Centre admin desk", fontSize = 13.sp, color = c.muted)
                }

                Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                    LoginField(
                        label = "Username",
                        value = username,
                        onValue = onUser,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )
                    LoginField(
                        label = "Password",
                        value = password,
                        onValue = onPass,
                        password = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .toggleable(value = remember, role = Role.Checkbox, onValueChange = onRemember)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = remember, onCheckedChange = null)
                        Text("Remember me", modifier = Modifier.padding(start = 8.dp), color = c.foreground, fontSize = 14.sp)
                    }
                    if (!error.isNullOrBlank()) {
                        Text(error, color = c.hard, fontSize = 14.sp, lineHeight = 19.sp)
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(DeskStyle.controlShape)
                        .background(if (loading) c.accentPressed else c.accent)
                        .clickable(enabled = !loading, onClick = onSubmit),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (if (loading) "Signing in…" else "Sign in").uppercase(),
                        fontFamily = DipiCondensed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        letterSpacing = 0.08.em,
                        color = Color.White,
                    )
                }

                Text(
                    "Your centre is read from your account after sign-in.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = c.muted,
                )
            }
        }
    }
}

/** 52dp field with an 11.5sp uppercase label, on the theme's field fill. */
@Composable
private fun LoginField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val c = LocalDipi.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            label.uppercase(),
            fontSize = 11.5.sp,
            letterSpacing = 0.09.em,
            color = c.muted,
        )
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DipiSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = c.foreground,
            ),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(c.field, DeskStyle.controlShape)
                        .border(1.dp, c.hairlineStrong, DeskStyle.controlShape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}
