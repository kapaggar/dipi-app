package org.dhamma.dipi.staff.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Hand a call-round message to WhatsApp itself rather than to a browser tab.
 *
 * `whatsapp://send` is WhatsApp's own documented scheme; naming the package
 * turns it into a direct hand-off — no chooser, no web.whatsapp.com, and no
 * QR-code detour on a tablet that has never paired a browser session.
 * WhatsApp Business is the second candidate, and only when neither app is
 * installed does the wa.me link fall through to whatever handles https.
 *
 * Targeting a package needs the two `<queries>` entries in the manifest:
 * since targetSdk 30 the platform filters unlisted packages out, and
 * `startActivity` would raise [ActivityNotFoundException] even for an
 * installed WhatsApp.
 */
private const val WHATSAPP = "com.whatsapp"
private const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"

/** Candidates in hand-off order: WhatsApp, WhatsApp Business, then the web link. */
fun whatsAppIntents(number: String, text: String): List<Intent> {
    val encoded = Uri.encode(text)
    val deepLink = Uri.parse("whatsapp://send?phone=$number&text=$encoded")
    val webLink = Uri.parse("https://wa.me/$number?text=$encoded")
    return listOf(
        Intent(Intent.ACTION_VIEW, deepLink).setPackage(WHATSAPP),
        Intent(Intent.ACTION_VIEW, deepLink).setPackage(WHATSAPP_BUSINESS),
        Intent(Intent.ACTION_VIEW, webLink),
    )
}

/** Try each candidate in turn; false only when nothing on the device took it. */
fun Context.openWhatsApp(number: String, text: String): Boolean {
    for (intent in whatsAppIntents(number, text)) {
        val sent = try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
        if (sent) return true
    }
    return false
}
