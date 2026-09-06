package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry

/** 4-digit device PIN. The digits live in dialog state only — never logged, never persisted raw. */
internal const val PIN_LENGTH = 4

private fun cleanPin(raw: String): String = raw.filter { it.isDigit() }.take(PIN_LENGTH)

/**
 * The device-PIN prompt (spec 2a S3, v6 C8): entering Settings from course
 * ops goes through this gate. Four cells, not a password field. A wrong PIN
 * shows [error] in the fixed severity colour and the dialog stays. Raw
 * digits are handed to [onSubmit] and nowhere else. The store is unchanged.
 */
@Composable
fun PinDialog(
    title: String,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
    error: String? = null,
) {
    val c = LocalDipi.current
    val industry = LocalIndustry.current
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        modifier = Modifier.testTag("pin-dialog"),
        containerColor = Color(0xFFFAFAFB),
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                letterSpacing = 0.2.sp,
            )
        },
        text = {
            Column {
                Text(
                    "Settings are locked while this tablet is in course ops. " +
                        "Use this tablet’s four-digit PIN.",
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    color = industry.neutral600,
                )
                BasicTextField(
                    value = pin,
                    onValueChange = { pin = cleanPin(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(Color.Transparent),
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .testTag("pin-input"),
                    decorationBox = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            repeat(PIN_LENGTH) { i ->
                                val filled = i < pin.length
                                val active = i == pin.length
                                Box(
                                    Modifier
                                        .width(56.dp)
                                        .height(64.dp)
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .border(
                                            if (active) 1.5.dp else 1.dp,
                                            when {
                                                active -> industry.accent
                                                filled -> industry.neutral300
                                                else -> industry.neutral300
                                            },
                                            RoundedCornerShape(6.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (filled) {
                                        Text(
                                            "•",
                                            fontFamily = DipiMono,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 26.sp,
                                            color = industry.text,
                                        )
                                    }
                                }
                            }
                            Text(
                                "4 DIGITS",
                                fontFamily = DipiMono,
                                fontWeight = FontWeight.Medium,
                                fontSize = 9.sp,
                                letterSpacing = 1.4.sp,
                                color = industry.neutral400,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    },
                )
                if (error != null) {
                    Text(
                        error,
                        color = c.hard,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 10.dp)
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
