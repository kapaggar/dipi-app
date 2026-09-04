package org.dhamma.dipi.staff.desk

import android.content.Context
import android.print.PrintManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.dhamma.dipi.staff.model.DaySummary
import org.dhamma.dipi.staff.model.SheetExport
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.model.SheetSort
import org.dhamma.dipi.staff.ui.NativePrint
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Display-only viewer for the desk's print-styled HTML sheets (and the
 * Applications edit page). Covers the whole desk frame; PRINT hands the
 * WebView to the Android print framework, CLOSE returns to the pane
 * underneath.
 *
 * The WebView is hardened: JavaScript off, no cache, no DOM storage, no
 * file/content access — and the session cookie is never handed to WebView's
 * CookieManager (the sheet's public CSS under
 * /sites/all/modules/dh_manageapp/css/ loads anonymously, which is fine).
 *
 * v5 (frames `5a`, `5t`) puts a real chrome around that body. Because JS is
 * off, the sheet's own toolbars are inert; [SheetStylesheet] hides them and
 * this pane replaces them with two native controls that do work — a
 * segmented sort that refetches through an allowlisted query parameter, and
 * column chips that flip a CSS class with no request at all. The sheet's own
 * `<div class="title">` is hidden too, so the course identity appears once.
 */
@Composable
fun SheetViewerPane(
    title: String,
    html: SheetPayload.Html?,
    loading: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Null for the Applications edit page and clarifications — no chrome controls. */
    export: SheetExport? = null,
    /** `{type} / {year} / {dates}` plus roll count; the second header line. */
    courseLine: String = "",
    sort: SheetSort = SheetSort.Default,
    onSort: (SheetSort) -> Unit = {},
    /** Day 0 summary only (v5 T2): drawn natively in place of the WebView. */
    summary: DaySummary? = null,
    /** Device-local HH:mm of the successful fetch — hidden while [loading]. */
    fetchedAt: String? = null,
    /** Board native 5h hall — same geometry as Course ops, no WebView. */
    nativeHall: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    val columns = remember(export) { export?.let(SheetStylesheet::columnsFor).orEmpty() }
    // Chip state lives here, not in the ViewModel: hiding a column is a CSS
    // class flip on a body we already hold, and must never cost a refetch.
    var hidden by rememberSaveable(export) {
        mutableStateOf(columns.filterNot { it.onByDefault }.map { it.name }.toSet())
    }
    val sortOptions = export?.let(SheetSort::optionsFor).orEmpty()

    Column(
        modifier
            .fillMaxSize()
            .background(Industry.surface)
            .testTag("sheet-viewer"),
    ) {
        SheetHeader(
            title = title,
            courseLine = courseLine,
            export = export,
            canPrint = (html != null && nativeHall == null) || summary != null,
            onPrint = {
                when {
                    summary != null -> NativePrint.printHtml(context, title, daySummaryPrintHtml(summary))
                    else -> webView?.let { printSheet(context, title, it) }
                }
            },
            onClose = onClose,
        )

        if (sortOptions.size > 1 || columns.isNotEmpty() || (!loading && !fetchedAt.isNullOrBlank())) {
            SheetControlBand(
                export = export,
                sortOptions = sortOptions,
                sort = sort,
                onSort = onSort,
                columns = columns,
                hidden = hidden,
                onToggleColumn = { col ->
                    hidden = if (col.name in hidden) hidden - col.name else hidden + col.name
                },
                fetchedAt = if (loading) null else fetchedAt,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val page = html
            when {
                // T2: the Day 0 summary is counts, not a document — it never
                // reaches the WebView, so there is no stylesheet to inject.
                nativeHall != null -> nativeHall()
                summary != null -> DaySummaryPane(summary)
                page != null -> {
                    // The sheet body sits on its own white page inset in the
                    // surface ground; PRINT takes the page, never the chrome.
                    val body = remember(page, hidden, export) {
                        SheetStylesheet.render(
                            serverHtml = page.html,
                            hidden = columns.filter { it.name in hidden }.toSet(),
                            export = export,
                        )
                    }
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .testTag("sheet-web"),
                        factory = { ctx -> WebView(ctx).apply { hardenForSheets() } },
                        update = { wv ->
                            webView = wv
                            // The tag tracks what is loaded so recompositions
                            // don't reload — now keyed on the rendered body,
                            // so a chip flip redraws without a network call.
                            if (wv.tag != body) {
                                wv.tag = body
                                wv.loadDataWithBaseURL(page.baseUrl, body, "text/html", "utf-8", null)
                            }
                        },
                    )
                }
                loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DeskProgressHairline(Modifier.width(220.dp).testTag("sheet-view-loading"))
                    DeskSub("Fetching from the desk…")
                }
                else -> DeskEmpty(
                    "Nothing to show.",
                    Modifier.align(Alignment.Center).padding(26.dp),
                )
            }
        }
    }
}

/**
 * 64dp header: a 48dp back target, the sheet name, the view-only chip beside
 * it rather than as a kicker above it, and the course line underneath. The
 * seating plan says `READ & PRINT` — the desk's own version of that page is
 * a drag-and-drop editor, and the chip is where we say so.
 */
@Composable
private fun SheetHeader(
    title: String,
    courseLine: String,
    export: SheetExport?,
    canPrint: Boolean,
    onPrint: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .bottomHairline(Industry.neutral300)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clickable(onClick = onClose)
                .semantics { contentDescription = "Back" }
                .testTag("sheet-back"),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", fontSize = 26.sp, color = Industry.neutral700)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 23.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.text,
                    modifier = Modifier.testTag("sheet-title"),
                )
                Text(
                    if (export == SheetExport.SeatingPlan) "READ & PRINT" else "VIEW ONLY",
                    fontFamily = DipiMono,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    letterSpacing = 0.14.em,
                    color = Industry.neutral600,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Industry.neutral100)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("sheet-viewonly-chip"),
                )
            }
            if (courseLine.isNotBlank()) {
                Text(
                    courseLine,
                    fontSize = 12.5.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.neutral600,
                    modifier = Modifier.testTag("sheet-course-line"),
                )
            }
        }
        if (canPrint) {
            Box(Modifier.testTag("sheet-print")) {
                DeskPrimaryButton("Print", onPrint)
            }
        }
        DeskOutlineButton("Close", onClose)
    }
}

/**
 * 52dp band holding only controls that work. The sort segments refetch (the
 * order comes from the server); the column chips do not (the body is already
 * here, and hiding a column is a class on `<html>`).
 */
@Composable
private fun SheetControlBand(
    export: SheetExport?,
    sortOptions: List<SheetSort>,
    sort: SheetSort,
    onSort: (SheetSort) -> Unit,
    columns: List<SheetStylesheet.Column>,
    hidden: Set<String>,
    onToggleColumn: (SheetStylesheet.Column) -> Unit,
    fetchedAt: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Industry.neutral100)
            .bottomHairline(Industry.neutral300)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sortOptions.size > 1 && export != null) {
            Text(
                if (export == SheetExport.Day0List) "SORT" else "ORDER",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.14.em,
                color = Industry.neutral500,
            )
            Row(
                Modifier
                    .height(38.dp)
                    .deskCard(
                        shape = RoundedCornerShape(5.dp),
                        fill = Color.Transparent,
                        border = Industry.neutral300,
                        elevation = 0.dp,
                    )
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("sheet-sort"),
            ) {
                sortOptions.forEach { option ->
                    val selected = option == sort
                    Box(
                        Modifier
                            .height(38.dp)
                            .background(if (selected) Industry.accent else Color.Transparent)
                            .clickable { if (!selected) onSort(option) }
                            .padding(horizontal = 14.dp)
                            .testTag("sheet-sort-option"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            SheetSort.labelFor(export, option),
                            fontSize = 13.sp,
                            maxLines = 1,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = if (selected) Color.White else Industry.neutral700,
                        )
                    }
                }
            }
        }
        if (columns.isNotEmpty()) {
            Text(
                "COLUMNS",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.14.em,
                color = Industry.neutral500,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                columns.forEach { col ->
                    val on = col.name !in hidden
                    Box(
                        Modifier
                            .height(38.dp)
                            .deskCard(
                                shape = RoundedCornerShape(19.dp),
                                fill = if (on) Industry.accent100 else Color.Transparent,
                                border = if (on) Industry.accent300 else Industry.neutral300,
                                elevation = 0.dp,
                            )
                            .clickable { onToggleColumn(col) }
                            .padding(horizontal = 15.dp)
                            .testTag("sheet-column-chip"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            col.label,
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = if (on) Industry.accent800 else Industry.neutral600,
                        )
                    }
                }
            }
        }
        if (!fetchedAt.isNullOrBlank()) {
            Spacer(Modifier.weight(1f))
            Text(
                "FROM THE DESK · $fetchedAt",
                fontFamily = DipiMono,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.14.em,
                color = Industry.neutral500,
                modifier = Modifier.testTag("sheet-freshness"),
            )
        }
    }
}

/**
 * The viewer's WebView hardening, in one named place so a test can assert on
 * it directly. These pages carry health disclosures and contact data, and
 * every control on them is JavaScript — turning JS back on to revive the
 * desk's toolbars would revive the seat-editor writes with them.
 */
fun WebView.hardenForSheets() {
    settings.javaScriptEnabled = false
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.domStorageEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
}

private fun printSheet(context: Context, title: String, webView: WebView) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    manager.print(title, webView.createPrintDocumentAdapter(title), NativePrint.a4Attributes())
}
