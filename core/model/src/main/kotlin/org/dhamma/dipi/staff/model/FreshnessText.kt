package org.dhamma.dipi.staff.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun freshnessText(iso: String?, now: Instant, zone: ZoneId, locale: Locale): String {
    val stamp = iso?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return "Unknown"
    val absolute = DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm z", locale)
        .withZone(zone).format(stamp)
    if (stamp.isAfter(now)) return "$absolute · Device clock differs"
    val age = Duration.between(stamp, now)
    val relative = when {
        age.seconds < 60 -> "less than a minute ago"
        age.toMinutes() < 60 -> {
            val m = age.toMinutes()
            if (m == 1L) "1 minute ago" else "$m minutes ago"
        }
        age.toHours() < 24 -> {
            val h = age.toHours()
            if (h == 1L) "1 hour ago" else "$h hours ago"
        }
        else -> {
            val d = age.toDays()
            if (d == 1L) "1 day ago" else "$d days ago"
        }
    }
    return "$absolute · $relative"
}
