package org.dhamma.dipi.staff.model

/**
 * One applicant's call-round log, keyed by applicant id. Device-local only —
 * the live desk has no calling endpoint, so nothing here is ever sent.
 * Mirrors [CheckInRecord]'s JSON-map-in-DataStore persistence.
 */
@kotlinx.serialization.Serializable
data class CallRecord(
    /** One of the CALL_OUTCOMES labels, or blank while the row is still in the To-call pile. */
    val outcome: String = "",
    /** Dial / WhatsApp taps plus no-answer marks — the tracker's attempts counter. */
    val attempts: Int = 0,
    /** Epoch millis of the last attempt or outcome; 0 = never touched. */
    val lastAttemptMs: Long = 0L,
    /** Short device-local note ("call after 6pm"). Never sent, never logged. */
    val note: String = "",
) {
    val logged: Boolean get() = outcome.isNotBlank()
}
