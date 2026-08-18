package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Name-or-conf-number filter over the locally cached applicants. Blank
 * queries match nobody (the list would just be everyone), otherwise a
 * case-insensitive contains on the display name or the conf number.
 */
fun advancedSearchMatches(rows: List<ApplicantCard>, query: String): List<ApplicantCard> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    return rows.filter { card ->
        card.displayName.contains(q, ignoreCase = true) ||
            card.confNo?.display()?.contains(q, ignoreCase = true) == true
    }.sortedBy { it.displayName }
}

/**
 * Local Room cache plus an optional desk POST (`search-app/{cid}`), labelled
 * "from desk". Never posts bulk-mail schedule fields.
 */
@Composable
fun AdvancedSearchScreen(
    rows: List<ApplicantCard>,
    onOpen: (ApplicantCard) -> Unit,
    onSearchDesk: (String, String?) -> Unit = { _, _ -> },
    deskRows: List<ApplicantCard> = emptyList(),
    deskBusy: Boolean = false,
    deskError: String? = null,
    deskStatuses: List<String> = emptyList(),
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val matches = advancedSearchMatches(rows, query)
    val courseCount = rows.map { it.courseId }.distinct().size
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Advanced Search", fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            if (rows.isEmpty()) {
                "Nothing cached yet — open a course once to search its applicants here."
            } else {
                "Searches the ${rows.size} applicants cached on this device " +
                    "($courseCount course${if (courseCount == 1) "" else "s"} loaded so far)."
            },
            color = c.muted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        TextButton(onClick = onBack) { Text("Back") }

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DipiSans,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = c.foreground,
            ),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.semantics { contentDescription = "Search name or conf number" },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(c.field, DeskStyle.controlShape)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text("Name or conf number", color = c.muted, fontSize = 15.sp)
                    }
                    inner()
                }
            },
        )

        when {
            query.isBlank() -> Text(
                "Type a name or a conf number to search.",
                color = c.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            matches.isEmpty() -> Text(
                "No cached applicant matches \"${query.trim()}\".",
                color = c.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp),
            )
            else -> Column(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                matches.take(60).forEach { card ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .deskCard(fill = c.field, border = c.hairline)
                            .clickable { onOpen(card) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                card.displayName,
                                fontFamily = DipiCondensed,
                                fontSize = 17.sp,
                                color = c.foreground,
                            )
                            Text(card.status.value, color = c.muted, fontSize = 12.sp)
                        }
                        Text(
                            card.confNo?.display() ?: "—",
                            fontFamily = DipiMono,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = c.foreground,
                        )
                    }
                }
                if (matches.size > 60) {
                    Text("Showing the first 60 of ${matches.size} matches.", color = c.muted, fontSize = 12.sp)
                }
            }
        }

        if (deskStatuses.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                deskStatuses.take(6).forEach { s ->
                    Text(
                        s,
                        color = if (status == s) c.accent else c.muted,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { status = if (status == s) null else s }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
        TextButton(
            onClick = { onSearchDesk(query, status) },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(if (deskBusy) "Searching the desk…" else "Search the desk")
        }
        if (deskError != null) {
            Text(deskError, color = c.foreground, fontSize = 13.sp)
        }
        if (deskRows.isNotEmpty()) {
            Text(
                "from desk · ${deskRows.size}",
                color = c.accent,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
            )
            deskRows.take(60).forEach { card ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .deskCard(fill = c.field, border = c.hairline)
                        .clickable { onOpen(card) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(card.displayName, fontFamily = DipiCondensed, fontSize = 17.sp, color = c.foreground)
                        Text(card.status.value, color = c.muted, fontSize = 12.sp)
                    }
                    Text(
                        card.confNo?.display() ?: "—",
                        fontFamily = DipiMono,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = c.foreground,
                    )
                }
            }
        }
    }
}
