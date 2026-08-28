package org.dhamma.dipi.staff.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.R
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

@Composable
fun SyncBannerStrips(offline: Boolean, queued: Int, onRetry: () -> Unit) {
    for (banner in syncBanners(offline, queued)) {
        when (banner) {
            SyncBanner.Offline -> OfflineStrip()
            is SyncBanner.Queued -> QueuedStrip(banner.count, onRetry)
        }
    }
}

@Composable
private fun OfflineStrip() {
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Text(
            text = stringResource(R.string.offline_strip),
            color = Industry.neutral700,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .background(Industry.neutral200)
                .padding(horizontal = 16.dp, vertical = 7.dp),
        )
        HorizontalDivider(thickness = 1.dp, color = Industry.neutral300)
    }
}

@Composable
private fun QueuedStrip(count: Int, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().testTag("queued-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Industry.accent100)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                count.toString(),
                fontFamily = DipiMono,
                fontSize = 11.sp,
                color = Industry.accent800,
            )
            Text(
                pluralStringResource(R.plurals.changes_waiting, count),
                fontSize = 12.5.sp,
                color = Industry.accent800,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
            )
            Text(
                stringResource(R.string.retry_sync),
                fontSize = 11.5.sp,
                letterSpacing = 0.06.em,
                color = Industry.accent700,
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .clickable(onClick = onRetry, role = Role.Button)
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(start = 8.dp)
                    .testTag("retry-sync"),
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Industry.accent300)
    }
}
