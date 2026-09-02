package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.LocalDipi

/** 4-digit device PIN. The digits live in dialog state only — never logged, never persisted raw. */
internal const val PIN_LENGTH = 4

private fun cleanPin(raw: String): String = raw.filter { it.isDigit() }.take(PIN_LENGTH)

/**
 * The device-PIN prompt (spec 2a S3): entering Settings from course ops goes
 * through this gate — the one gate that also covers the mode switch, Logout
 * and Erase-all. A wrong PIN shows [error] in the fixed severity colour and
 * the dialog stays. `LocalDipi` tokens throughout; the raw digits are handed
 * to [onSubmit] and nowhere else.
 */
@Composable
fun PinDialog(
    title: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    val c = LocalDipi.current
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.testTag("pin-dialog"),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = cleanPin(it) },
                    label = { Text("Device PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin-input"),
                )
                if (error != null) {
                    // The fixed severity pair — never follows the skin.
                    Text(
                        error,
                        color = c.hard,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("pin-error"),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(pin) },
                enabled = pin.length == PIN_LENGTH,
                modifier = Modifier.testTag("pin-submit"),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

/**
 * First-enable PIN collection (spec 2a S3): a 4-digit PIN typed twice —
 * set + confirm — before course ops flips on. CONFIRM stays disabled until
 * both fields hold the same four digits.
 */
@Composable
fun PinSetupDialog(
    onSet: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val ready = pin.length == PIN_LENGTH && pin == again
    AlertDialog(
        modifier = Modifier.testTag("pin-setup-dialog"),
        onDismissRequest = onDismiss,
        title = { Text("Set a device PIN") },
        text = {
            Column {
                Text(
                    "Course ops locks Settings behind this PIN. It stays on the tablet " +
                        "and survives logout; Erase all local data removes it.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = cleanPin(it) },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("pin-set-input"),
                )
                OutlinedTextField(
                    value = again,
                    onValueChange = { again = cleanPin(it) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .testTag("pin-set-confirm"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSet(pin) },
                enabled = ready,
                modifier = Modifier.testTag("pin-set-submit"),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
