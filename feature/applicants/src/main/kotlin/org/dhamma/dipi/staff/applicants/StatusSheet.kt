package org.dhamma.dipi.staff.applicants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantStatus
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

@OptIn(ExperimentalLayoutApi::class)
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
    val common = ApplicantStatus.COMMON_CHOICES
    val rare = choices.filter { choice ->
        common.none { it.equals(choice, ignoreCase = true) } &&
            !choice.equals("Approved", ignoreCase = true)
    }
    Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("$current → choose new status", fontFamily = DipiCondensed, fontSize = 18.sp)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            common.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { choice ->
                        val selected = choice.equals(pick, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 60.dp)
                                .border(1.dp, c.accent, RoundedCornerShape(4.dp))
                                .background(if (selected) c.accent else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable { onPick(choice) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                choice,
                                color = if (selected) Color.White else c.foreground,
                                fontFamily = DipiCondensed,
                                fontSize = 16.sp,
                            )
                        }
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Text(
            "Less used",
            color = c.muted,
            fontFamily = DipiCondensed,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            rare.forEach { choice ->
                val selected = choice.equals(pick, ignoreCase = true)
                Text(
                    text = choice,
                    color = if (selected) Color.White else c.foreground,
                    fontFamily = DipiCondensed,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .border(1.dp, if (selected) c.accent else c.hairlineStrong, RoundedCornerShape(4.dp))
                        .background(if (selected) c.accent else Color.Transparent, RoundedCornerShape(4.dp))
                        .clickable { onPick(choice) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (pick.contains("Custom", ignoreCase = true)) {
            OutlinedTextField(
                custom,
                onCustom,
                label = { Text("Custom status") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        }
        OutlinedTextField(
            comment,
            onComment,
            label = { Text("Comment (optional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Text(
            "The server may send the applicant a letter for this change.",
            color = c.muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        Button(onConfirm, Modifier.fillMaxWidth()) { Text("Confirm change") }
    }
}
