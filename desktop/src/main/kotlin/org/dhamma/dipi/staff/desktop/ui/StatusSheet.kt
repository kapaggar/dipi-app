package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi

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
    val c = LocalDipi.current
    Box(
        Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0x99000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.56f)
                .clip(DeskShape)
                .background(c.background)
                .border(1.dp, c.hairlineStrong, DeskShape)
                .clickable(enabled = false) {}
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Change status", color = c.foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Now $current · this may send a letter. Never Approved.", color = c.muted, fontSize = 13.sp)
            choices.filter { !it.equals("Approved", ignoreCase = true) }.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { label ->
                        DeskButton(label, { onPick(label) }, primary = pick == label, modifier = Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                }
            }
            if (pick.contains("Custom", true)) {
                DeskField(custom, onCustom, Modifier.fillMaxWidth(), placeholder = "Custom status (not Approved)")
            }
            DeskField(comment, onComment, Modifier.fillMaxWidth(), placeholder = "Comment (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeskButton("Send", onConfirm, primary = true)
                DeskButton("Cancel", onDismiss)
            }
        }
    }
}
