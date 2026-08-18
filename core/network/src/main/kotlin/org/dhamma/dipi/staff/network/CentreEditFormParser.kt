package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.CentreFormSettings

/**
 * Current values of `dh_ma_centre_form` on `GET /centre/{cid}/edit`
 * (`centre.inc:69`). Display-only — the app never POSTs this form.
 */
object CentreEditFormParser {
    const val FORM_ID = "dh_ma_centre_form"

    fun parse(html: String): CentreFormSettings? {
        if (!html.contains(FORM_ID) && !html.contains("c_name")) return null
        return CentreFormSettings(
            name = HtmlForms.inputValue(html, "c_name").orEmpty(),
            trust = HtmlForms.inputValue(html, "c_trust").orEmpty(),
            address = HtmlForms.textarea(html, "c_address").orEmpty(),
            pincode = HtmlForms.inputValue(html, "c_pincode").orEmpty(),
            city = HtmlForms.inputValue(html, "c_city").orEmpty(),
            state = HtmlForms.inputValue(html, "c_state").orEmpty(),
            country = HtmlForms.inputValue(html, "c_country").orEmpty(),
            phone = HtmlForms.inputValue(html, "c_phone").orEmpty(),
            fax = HtmlForms.inputValue(html, "c_fax").orEmpty(),
            email = HtmlForms.inputValue(html, "c_email").orEmpty(),
            website = HtmlForms.inputValue(html, "c_url").orEmpty(),
            emailFrom = HtmlForms.inputValue(html, "cs_email_from_name").orEmpty(),
            emailReplyTo = HtmlForms.inputValue(html, "cs_email_reply_to").orEmpty(),
            announcement = HtmlForms.textarea(html, "c_announcement").orEmpty(),
            preconf = HtmlForms.yesNo(html, "cs_preconf_enable"),
            reconf = HtmlForms.yesNo(html, "cs_reconf_enable"),
            expectedMail = HtmlForms.yesNo(html, "cs_expected_enable"),
            whatsappPreconf = HtmlForms.yesNo(html, "cs_whatsapp_preconf"),
            whatsappReconf = HtmlForms.yesNo(html, "cs_whatsapp_reconf"),
            whatsappMsg = HtmlForms.yesNo(html, "cs_whatsapp_msg"),
            preconfDays = HtmlForms.inputValue(html, "cs_preconf_days").orEmpty(),
            reconfDays = HtmlForms.inputValue(html, "cs_reconf_days").orEmpty(),
            reconfCancelDays = HtmlForms.inputValue(html, "cs_reconf_cancel").orEmpty(),
            expectedDays = HtmlForms.inputValue(html, "cs_expected_days").orEmpty(),
        )
    }
}
