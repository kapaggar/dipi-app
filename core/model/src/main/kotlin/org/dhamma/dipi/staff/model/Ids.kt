package org.dhamma.dipi.staff.model

@JvmInline value class CentreId(val value: Int)
@JvmInline value class CourseId(val value: Int)
@JvmInline value class ApplicantId(val value: Int)
@JvmInline value class ConfNo(val value: String) {
    fun display(): String = value.ifBlank { "—" }
    companion object {
        fun parseOrNull(raw: String?): ConfNo? {
            val t = raw?.trim().orEmpty()
            if (t.isEmpty()) return null
            return ConfNo(t)
        }
    }
}
