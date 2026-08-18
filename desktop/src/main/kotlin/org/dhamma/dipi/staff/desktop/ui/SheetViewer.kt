package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.network.SearchPageParser

@Composable
fun SheetViewerPane(
    title: String,
    html: SheetPayload.Html?,
    loading: Boolean,
    onClose: () -> Unit,
) {
    val c = LocalDipi.current
    val body = remember(html?.html) { html?.html?.let { SearchPageParser.stripTags(it) }.orEmpty() }
    Column(Modifier.fillMaxSize().background(c.background)) {
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        ) {
            Text(title, color = c.foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            DeskButton("Close", onClose)
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent)
        Text(
            if (loading) "Fetching from the live desk…" else body.ifBlank { "Empty sheet." },
            color = c.foreground,
            fontSize = 13.sp,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        )
        Text(
            "In-memory only. Never persisted. JavaScript off.",
            color = c.muted,
            fontSize = 11.sp,
            modifier = Modifier.padding(16.dp),
        )
    }
}
