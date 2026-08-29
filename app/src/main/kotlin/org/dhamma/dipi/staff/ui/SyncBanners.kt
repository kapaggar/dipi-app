package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import org.dhamma.dipi.staff.R
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry

/** One sync strip on the phone shell. */
sealed interface SyncBanner {
    data object Offline : SyncBanner
    data class Queued(val count: Int) : SyncBanner
}

/**
 * Which strips the phone shell shows. Offline and queued are independent —
 * an online device with queued rows must never claim to be offline. See
 * docs/specs/2026-08-26-v3-conformance-spec.md S1.2.
 */
fun syncBanners(offline: Boolean, queued: Int): List<SyncBanner> = buildList {
    if (offline) add(SyncBanner.Offline)
    if (queued > 0) add(SyncBanner.Queued(queued))
}

/**
 * "last try HH:MM" for the queued strip — 24-hour, device zone, mono. Null
 * until the first flush attempt of the process: a fresh process shows the
 * strip without a last-try line. See version-4/README.md frame 1g, spec R7.
 */
fun lastTryLabel(epochMs: Long?, zone: ZoneId = ZoneId.systemDefault()): String? =
    epochMs?.let {
        val at = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
        "last try %02d:%02d".format(at.hour, at.minute)
    }

@Composable
fun SyncBannerStrips(
    offline: Boolean,
    queued: Int,
    lastTryAtMs: Long? = null,
    onRetry: () -> Unit,
) {
    for (banner in syncBanners(offline, queued)) {
        when (banner) {
            SyncBanner.Offline -> OfflineStrip()
            is SyncBanner.Queued -> QueuedStrip(banner.count, lastTryAtMs, onRetry)
        }
    }
}

@Composable
private fun OfflineStrip() {
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(Industry.neutral200)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.offline_strip),
                color = Industry.neutral700,
                fontSize = 14.sp,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.neutral300)
    }
}

/**
 * Deeper than the offline strip (56dp against 38dp) because it is the only
 * strip you can tap: RETRY is a bordered 48dp button, not a text link.
 */
@Composable
private fun QueuedStrip(count: Int, lastTryAtMs: Long?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("queued-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Industry.accent100)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        count.toString(),
                        fontFamily = DipiMono,
                        fontSize = 14.sp,
                        color = Industry.accent800,
                    )
                    Text(
                        pluralStringResource(R.plurals.changes_waiting, count),
                        fontSize = 14.sp,
                        color = Industry.accent800,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                lastTryLabel(lastTryAtMs)?.let { line ->
                    Text(
                        line,
                        fontFamily = DipiMono,
                        fontSize = 12.5.sp,
                        color = Industry.accent700,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
            Row(
                Modifier
                    .height(48.dp)
                    .border(1.dp, Industry.accent400, RoundedCornerShape(5.dp))
                    .clickable(onClick = onRetry, role = Role.Button)
                    .padding(horizontal = 22.dp)
                    .testTag("retry-sync"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.retry_sync),
                    fontFamily = DipiCondensed,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    letterSpacing = 0.133.em,
                    color = Industry.accent800,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.accent200)
    }
}
