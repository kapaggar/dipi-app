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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import java.util.Locale
import org.dhamma.dipi.staff.R
import org.dhamma.dipi.staff.ui.theme.DipiColors
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.LocalDipi

/** One sync strip on the phone shell. */
sealed interface SyncBanner {
    data object Offline : SyncBanner
    data class Queued(val count: Int) : SyncBanner
}

/**
 * Which strips the phone shell shows. Offline and queued are independent —
 * an online device with queued rows must never claim to be offline. See
 * the 08-26 v3-conformance spec S1.2 (docs/DECISIONS.md, Design system).
 */
fun syncBanners(offline: Boolean, queued: Int): List<SyncBanner> = buildList {
    if (offline) add(SyncBanner.Offline)
    if (queued > 0) add(SyncBanner.Queued(queued))
}

/**
 * "last try HH:MM" for the queued strip — 24-hour, device zone, mono. Null
 * until the first flush attempt of the process: a fresh process shows the
 * strip without a last-try line. See docs/DESIGN.md frame 1g, spec R7.
 */
fun lastTryLabel(epochMs: Long?, zone: ZoneId = ZoneId.systemDefault()): String? =
    epochMs?.let {
        val at = Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
        // Locale.ROOT: a clock reading stays Latin digits even where the
        // device locale would render eastern-Arabic or Devanagari numerals.
        String.format(Locale.ROOT, "last try %02d:%02d", at.hour, at.minute)
    }

/**
 * True on the Steel night ramp. Mirrors the same helper in
 * `feature/auth/.../LoginScreen.kt`: `DipiColors` carries no dark flag, so the
 * places that legitimately need one read it off the ground's own luminance.
 */
private fun DipiColors.isNight(): Boolean = background.luminance() < 0.5f

/**
 * Night text on tinted accent grounds — Steel `accent300` `#B5D9FD`, the same
 * choice `SettingsScreen`'s `NightAccentText` makes. 9.6:1 on `tint`.
 */
private val NightAccentText = IndustryPalette.Steel.accent300

/**
 * The dimmer step of that family — Steel `accent500` `#749DC4`, 4.9:1 on
 * `tint`, so the secondary "last try" line and the RETRY border sit below the
 * body copy without dropping under 4.5:1.
 */
private val NightAccentDim = IndustryPalette.Steel.accent500

/** The queued strip's night hairline — Steel `accent700`, a rule on `tint`. */
private val NightAccentRule = IndustryPalette.Steel.accent700

/**
 * Offline-strip body on the night ramp: `#C3C9D0` verbatim from
 * `docs/DESIGN.md` frame 1e ("the offline strip was still Blossom pink —
 * now `#22272C` / `#C3C9D0`"). It has no ramp token of its own; it sits
 * between night `muted` `#9BA1A8` and `foreground` `#E4E6E9`. 9.0:1 on
 * `#22272C`.
 */
private val NightOfflineText = Color(0xFFC3C9D0)

// The strips are chrome, not cards, so they paint from the active skin's
// Industry block in light — and from the night ramp in dark, where Industry
// has no branch at all. Split out as pure functions so the pairing is
// testable without pixel capture (as LoginScreen.kt does for its error strip).

/** Offline-strip ground: `neutral200` by day, night `hover` `#22272C` (frame 1e). */
fun offlineStripFill(c: DipiColors): Color = if (c.isNight()) c.hover else Industry.neutral200

/** Offline-strip copy: `neutral700` by day, `#C3C9D0` at night. */
fun offlineStripText(c: DipiColors): Color = if (c.isNight()) NightOfflineText else Industry.neutral700

/** Offline-strip rule: `neutral300` by day, the night `hairline`. */
fun offlineStripRule(c: DipiColors): Color = if (c.isNight()) c.hairline else Industry.neutral300

/** Queued-strip ground: `accent100` by day, night `tint` `#1D2D3D`. */
fun queuedStripFill(c: DipiColors): Color = if (c.isNight()) c.tint else Industry.accent100

/** Queued-strip count and copy: `accent800` by day, `#B5D9FD` at night. */
fun queuedStripText(c: DipiColors): Color = if (c.isNight()) NightAccentText else Industry.accent800

/** The dimmer "last try" line: `accent700` by day, `#749DC4` at night. */
fun queuedStripDim(c: DipiColors): Color = if (c.isNight()) NightAccentDim else Industry.accent700

/** RETRY's 1dp border: `accent400` by day, `#749DC4` at night. */
fun queuedStripBorder(c: DipiColors): Color = if (c.isNight()) NightAccentDim else Industry.accent400

/** Queued-strip bottom hairline: `accent200` by day, `accent700` at night. */
fun queuedStripRule(c: DipiColors): Color = if (c.isNight()) NightAccentRule else Industry.accent200

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
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth().testTag("offline-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(offlineStripFill(c))
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.offline_strip),
                color = offlineStripText(c),
                fontSize = 14.sp,
            )
        }
        HorizontalDivider(thickness = 1.dp, color = offlineStripRule(c))
    }
}

/**
 * Deeper than the offline strip (56dp against 38dp) because it is the only
 * strip you can tap: RETRY is a bordered 48dp button, not a text link.
 */
@Composable
private fun QueuedStrip(count: Int, lastTryAtMs: Long?, onRetry: () -> Unit) {
    val c = LocalDipi.current
    Column(Modifier.fillMaxWidth().testTag("queued-strip")) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(queuedStripFill(c))
                .padding(start = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(count.toString(), fontFamily = DipiMono, fontSize = 14.sp, color = queuedStripText(c))
                Text(
                    pluralStringResource(R.plurals.changes_waiting, count),
                    fontSize = 14.sp,
                    color = queuedStripText(c),
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
                lastTryLabel(lastTryAtMs)?.let { line ->
                    Text(
                        line,
                        fontFamily = DipiMono,
                        fontSize = 12.5.sp,
                        color = queuedStripDim(c),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Row(
                Modifier
                    .height(48.dp)
                    .border(1.dp, queuedStripBorder(c), RoundedCornerShape(5.dp))
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
                    color = queuedStripText(c),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = queuedStripRule(c))
    }
}
