package org.dhamma.dipi.staff.model

data class FlushSnack(val text: String, val error: Boolean)

/** Server-wins snack after an outbox flush or live status write. */
object OutboxReconciler {
    fun snack(
        optimisticStatus: String,
        result: StatusChangeResult,
        serverStatus: String?,
    ): FlushSnack {
        if (!result.ok) {
            return FlushSnack(result.msg.ifBlank { "Failed" }, error = true)
        }
        val parts = mutableListOf("Status updated")
        result.confNo?.let { parts += "conf no $it" }
        val actual = serverStatus ?: result.newStatus
        if (actual != null && !actual.equals(optimisticStatus, ignoreCase = true)) {
            parts += "server now $actual"
        }
        return FlushSnack(parts.joinToString(" · "), error = false)
    }
}
