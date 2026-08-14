package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.LocalDipi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSheet(
    current: String,
    choices: List<String>,
    pick: String,
    comment: String,
    custom: String,
    onPick: (String) -> Unit,
    onComment: (String) -> Unit,
    onCustom: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        StatusSheetContent(
            current = current,
            choices = choices,
            pick = pick,
            comment = comment,
            custom = custom,
            onPick = onPick,
            onComment = onComment,
            onCustom = onCustom,
            onConfirm = onConfirm,
        )
    }
}

@Composable
fun StatusSheetContent(
    current: String,
    choices: List<String>,
    pick: String,
    comment: String,
    custom: String,
    onPick: (String) -> Unit,
    onComment: (String) -> Unit,
    onCustom: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val c = LocalDipi.current
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("$current → choose new status", fontFamily = DipiCondensed, fontSize = 18.sp)
        choices.forEach { choice ->
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(choice == pick) { onPick(choice) }
                    .padding(vertical = 6.dp),
            ) {
                RadioButton(choice == pick, { onPick(choice) })
                Text(choice, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
            }
        }
        if (pick.contains("Custom", ignoreCase = true)) {
            OutlinedTextField(custom, onCustom, label = { Text("Custom status") }, modifier = Modifier.fillMaxWidth())
        }
        OutlinedTextField(comment, onComment, label = { Text("Comment (optional)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text(
            "✉ The server may send the applicant a letter for this change.",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onConfirm, Modifier.fillMaxWidth()) { Text("Confirm change") }
    }
}
