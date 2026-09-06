package org.dhamma.dipi.staff.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Hands a native Compose surface to the Android print framework as a
 * print-only HTML document. The rail and accent fills stay off paper.
 * The WebView is retained until the page finishes loading so the adapter
 * is not collected mid-job.
 */
object NativePrint {
    private var hold: WebView? = null

    /** A4, CSS `@page` margins — the system default on the Pixel C is Letter. */
    fun a4Attributes(): PrintAttributes =
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

    /** A4 landscape for the native seating plan; other sheets stay portrait. */
    fun a4LandscapeAttributes(): PrintAttributes =
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

    fun printHtml(
        context: Context,
        jobName: String,
        html: String,
        attributes: PrintAttributes = a4Attributes(),
    ) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val wv = WebView(context)
        hold = wv
        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                manager.print(
                    jobName,
                    wv.createPrintDocumentAdapter(jobName),
                    attributes,
                )
            }
        }
        wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }
}
