package org.dhamma.dipi.staff.model

/** Metadata only; bodies and bearer URLs must never enter a serializable model. */
data class ManagedLetter(
    val id: Int,
    val centreId: Int,
    val name: String,
    val event: String,
    val courseType: String,
    val subject: String,
)

/** Server-rendered text, scoped to one request and deliberately not serializable. */
class RenderedLetter(val applicantId: Int, val letterId: Int, val text: String) {
    override fun toString(): String = "RenderedLetter([redacted])"
}
