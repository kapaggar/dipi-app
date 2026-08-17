package org.dhamma.dipi.staff.desk

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.dhamma.dipi.staff.model.SheetPayload
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.Industry

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
 */
@Composable
fun SheetViewerPane(
    title: String,
    html: SheetPayload.Html?,
    loading: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .background(Industry.bg)
            .testTag("sheet-viewer"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .bottomHairline(Industry.neutral300)
                .padding(horizontal = 26.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DeskKicker("SHEET · VIEW ONLY", Industry.neutral500)
                Text(
                    title,
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Industry.text,
                    modifier = Modifier.testTag("sheet-title"),
                )
            }
            if (html != null) {
                DeskPrimaryButton("Print", { webView?.let { printSheet(context, title, it) } })
            }
            DeskOutlineButton("Close", onClose)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val page = html
            when {
                page != null -> AndroidView(
                    modifier = Modifier.fillMaxSize().testTag("sheet-web"),
                    factory = { ctx -> WebView(ctx).apply { hardenForSheets() } },
                    update = { wv ->
                        webView = wv
                        // The tag tracks what is loaded so recompositions don't reload.
                        if (wv.tag != page) {
                            wv.tag = page
                            wv.loadDataWithBaseURL(page.baseUrl, page.html, "text/html", "utf-8", null)
                        }
                    },
                )
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

private fun WebView.hardenForSheets() {
    settings.javaScriptEnabled = false
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    settings.domStorageEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
}

private fun printSheet(context: Context, title: String, webView: WebView) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    manager.print(title, webView.createPrintDocumentAdapter(title), PrintAttributes.Builder().build())
}
