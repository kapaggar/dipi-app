package org.dhamma.dipi.staff.model

/**
 * The reconfirmation message the desk hands to WhatsApp. Centres word it
 * themselves (Centre settings); a blank template falls back to
 * [WHATSAPP_DEFAULT_TEMPLATE]. Tokens resolve at hand-off time from the card
 * already on screen — nothing here is stored per applicant and no number or
 * name is ever written to disk by this file.
 */

/** Bilingual default, mirroring the call tracker's fallback message. */
const val WHATSAPP_DEFAULT_TEMPLATE: String =
    "नमस्ते {name} जी, आपका {course} शिविर {dates} को {centre} में है। " +
        "कृपया अपनी उपस्थिति की पुष्टि करें। धन्यवाद।"

/** What the settings screen offers, in the order it lists them. */
val WHATSAPP_TOKENS: List<String> = listOf("{name}", "{course}", "{dates}", "{centre}", "{conf}")

/**
 * Substitute the tokens. [name] is trimmed to the first word so the greeting
 * reads naturally; an unknown token is left alone rather than blanked, so a
 * typo is visible in the preview instead of silently eating text.
 */
fun whatsAppMessage(
    template: String,
    name: String,
    course: String = "",
    dates: String = "",
    centre: String = "",
    conf: String = "",
): String = template.ifBlank { WHATSAPP_DEFAULT_TEMPLATE }
    .replace("{name}", name.trim().substringBefore(" "))
    .replace("{course}", course)
    .replace("{dates}", dates)
    .replace("{centre}", centre)
    .replace("{conf}", conf)
    .trim()
