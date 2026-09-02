package org.dhamma.dipi.staff.model

/** Maps a status change to existing /change-status query params. */
object StatusWrite {
    fun query(status: String, letterId: Int = 0, comment: String = ""): Map<String, String> {
        require(!ApplicantStatus.isForbiddenWrite(status)) { "never send Approved" }
        return mapOf(
            "s" to status,
            "l" to letterId.toString(),
            "c" to comment,
        )
    }

    fun parseResult(
        status: String?,
        msg: String?,
        confno: String?,
        newstatus: String?,
    ): StatusChangeResult {
        val ok = status.equals("OK", ignoreCase = true)
        return StatusChangeResult(
            ok = ok,
            status = status.orEmpty(),
            msg = msg.orEmpty(),
            confNo = confno?.takeIf { it.isNotBlank() },
            newStatus = newstatus?.takeIf { it.isNotBlank() },
        )
    }
}
