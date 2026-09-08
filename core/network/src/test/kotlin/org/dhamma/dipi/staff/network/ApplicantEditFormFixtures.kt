package org.dhamma.dipi.staff.network

internal object ApplicantEditFormFixtures {
    const val NPI_DOC = "NPI-AADHAAR-FIXTURE"
    const val NPI_HEALTH = "NPI-HEALTH-FIXTURE"
    const val ORIGIN = "http://127.0.0.1:9"

    fun completeHtml(
        id: Int = 41,
        action: String = "/app/$id/edit",
        statusSelected: String = "Confirmed",
        includeFile: Boolean = true,
        includeToken: Boolean = true,
        includeBuild: Boolean = true,
        formId: String = ApplicantEditFormParser.LIVE_FORM_ID,
        extraHidden: String = "",
    ): String = """
        <html><body>
        <form action="$action" method="post" id="dh-ma-applicant-form" accept-charset="UTF-8" enctype="multipart/form-data">
          <input type="text" name="a_f_name" value="Priya" />
          <input value="Nair" name="a_l_name" type="text" />
          <input type="text" name="a_email" value="priya@example.test" />
          <input type="text" name="a_dob[date]" value="1990-01-15" />
          <textarea name="a_address">Pune lane</textarea>
          <input type="radio" name="a_gender" value="M" />
          <input type="radio" name="a_gender" value="F" checked="checked" />
          <input type="radio" name="a_old" value="0" checked="checked" />
          <input type="radio" name="a_old" value="1" />
          <input type="radio" name="attending" value="0" />
          <input type="radio" name="attending" value="1" checked="checked" />
          <input type="radio" name="special" value="" />
          <input type="radio" name="special" value="chowky" />
          <input type="radio" name="special" value="chair" checked="checked" />
          <input type="radio" name="special" value="backrest" />
          <input type="checkbox" name="a_monk" value="1" checked="checked" />
          <input type="checkbox" name="a_alist" value="1" />
          <select name="a_status">
            <option value="Received">Received</option>
            <option value="Confirmed" ${if (statusSelected == "Confirmed") "selected=\"selected\"" else ""}>Confirmed</option>
            <option value="Approved" ${if (statusSelected == "Approved") "selected=\"selected\"" else ""}>Approved</option>
          </select>
          <select name="document_type"><option value="a_aadhar" selected="selected">Aadhar</option></select>
          <input type="text" name="document_id" value="$NPI_DOC" />
          <textarea name="ae_desc_physical">$NPI_HEALTH</textarea>
          <input type="hidden" name="course" value="10" />
          <input type="hidden" name="centre" value="1" />
          ${if (includeBuild) """<input type="hidden" name="form_build_id" value="form-AppEdItBuIlD" />""" else ""}
          ${if (includeToken) """<input type="hidden" name="form_token" value="tok-photo-write" />""" else ""}
          <input type="hidden" name="form_id" value="$formId" />
          $extraHidden
          ${if (includeFile) """<input type="file" name="files[upload_photo]" size="60" class="form-file" />""" else ""}
          <input type="submit" id="edit-sub" name="op" value="Update" />
        </form>
        </body></html>
    """.trimIndent()

    fun loginHtml(): String = """
        <html><body>
        <form action="/home" method="post">
          <input type="text" name="name" />
          <input type="password" name="pass" />
          <input type="hidden" name="form_id" value="user_login_block" />
        </form>
        </body></html>
    """.trimIndent()
}
