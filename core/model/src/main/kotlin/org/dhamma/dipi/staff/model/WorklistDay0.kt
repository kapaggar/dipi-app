package org.dhamma.dipi.staff.model

import java.time.LocalDate

/** Statuses that count as expected-today on the phone Day 0 summary. */
private val DAY_SUMMARY_EXPECTED = setOf("confirmed", "expected")

/** Worklist statuses that are not arrivals on Zero Day. */
private val NOT_ARRIVAL = setOf("cancelled", "rejected", "duplicate", "left")

data class WorklistDaySummary(
    val students: Int,
    val servers: Int,
    val arrived: Int,
) {
    val expected: Int get() = students + servers
}

/**
 * Phone Day 0 summary counts. Confirmed and Expected both count as expected
 * today; arrived is anyone already marked attended on the worklist.
 */
fun worklistDaySummary(rows: List<ApplicantCard>): WorklistDaySummary {
    val incoming = rows.filter { it.status.normalize() in DAY_SUMMARY_EXPECTED }
    return WorklistDaySummary(
        students = incoming.count { it.type == ApplicantType.Student },
        servers = incoming.count { it.type == ApplicantType.Sevak },
        arrived = rows.count { it.attended },
    )
}

/** Date for the phone Day 0 title: ISO start, else the name window. */
fun daySummaryDateLine(course: Course, today: LocalDate = LocalDate.now()): String {
    val iso = course.start.takeIf { it.isNotBlank() }
        ?: parseCourseWindow(course.name, today)?.start?.toString()
        ?: return ""
    return runCatching {
        val d = LocalDate.parse(iso)
        val mon = d.month.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
        "${d.dayOfMonth} $mon ${d.year}"
    }.getOrDefault(iso)
}

/** Cancelled / Duplicate / Rejected / Left are not Zero Day arrivals. */
fun isZeroDayUnattended(card: ApplicantCard): Boolean =
    !card.attended && card.status.normalize() !in NOT_ARRIVAL
