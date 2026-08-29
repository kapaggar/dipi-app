package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.dhamma.dipi.staff.ui.SyncBanner
import org.dhamma.dipi.staff.ui.SyncBannerStrips
import org.dhamma.dipi.staff.ui.lastTryLabel
import org.dhamma.dipi.staff.ui.offlineStripFill
import org.dhamma.dipi.staff.ui.offlineStripText
import org.dhamma.dipi.staff.ui.queuedStripBorder
import org.dhamma.dipi.staff.ui.queuedStripDim
import org.dhamma.dipi.staff.ui.queuedStripFill
import org.dhamma.dipi.staff.ui.queuedStripText
import org.dhamma.dipi.staff.ui.syncBanners
import org.dhamma.dipi.staff.ui.theme.DarkDipi
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.lightDipi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncBannersTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun quietWhenOnlineAndNothingQueued() {
        assertEquals(emptyList<SyncBanner>(), syncBanners(offline = false, queued = 0))
    }

    @Test
    fun offlineAloneShowsOnlyTheOfflineStrip() {
        assertEquals(listOf(SyncBanner.Offline), syncBanners(offline = true, queued = 0))
    }

    @Test
    fun queuedWhileOnlineNeverClaimsOffline() {
        assertEquals(listOf(SyncBanner.Queued(2)), syncBanners(offline = false, queued = 2))
    }

    @Test
    fun offlineAndQueuedShowsOfflineFirst() {
        assertEquals(
            listOf(SyncBanner.Offline, SyncBanner.Queued(3)),
            syncBanners(offline = true, queued = 3),
        )
    }

    @Test
    fun onlineWithQueuedRendersCountAndRetryButNotOfflineCopy() {
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 2, onRetry = {}) }
        }
        rule.onNodeWithTag("queued-strip").assertIsDisplayed()
        rule.onNodeWithText("changes waiting to sync").assertIsDisplayed()
        rule.onNodeWithText("RETRY").assertIsDisplayed()
        rule.onAllNodesWithTag("offline-strip").assertCountEquals(0)
    }

    @Test
    fun singularCopyForOneQueuedChange() {
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 1, onRetry = {}) }
        }
        rule.onNodeWithText("change waiting to sync").assertIsDisplayed()
    }

    @Test
    fun retryFiresTheCallback() {
        var retried = false
        rule.setContent {
            DipiTheme { SyncBannerStrips(offline = false, queued = 2, onRetry = { retried = true }) }
        }
        rule.onNodeWithText("RETRY").performClick()
        assertTrue(retried)
    }

    @Test
    fun lastTryLabelIsTwentyFourHourHoursAndMinutes() {
        val evening = Instant.parse("2026-08-28T19:45:12Z").toEpochMilli()
        assertEquals("last try 19:45", lastTryLabel(evening, ZoneOffset.UTC))
        val morning = Instant.parse("2026-08-28T07:05:59Z").toEpochMilli()
        assertEquals("last try 07:05", lastTryLabel(morning, ZoneOffset.UTC))
    }

    @Test
    fun noLastTryLabelBeforeTheFirstAttempt() {
        assertNull(lastTryLabel(null, ZoneOffset.UTC))
    }

    @Test
    fun queuedStripRendersTheLastTryLineWhenAnAttemptIsStamped() {
        val at = Instant.parse("2026-08-28T09:30:00Z").toEpochMilli()
        rule.setContent {
            DipiTheme {
                SyncBannerStrips(offline = false, queued = 2, lastTryAtMs = at, onRetry = {})
            }
        }
        rule.onNodeWithText(lastTryLabel(at, ZoneId.systemDefault())!!).assertIsDisplayed()
    }

    /**
     * The strips used to paint from `Industry` alone, which is skin-aware but
     * has no night branch — so at night a Blossom user got the light pink
     * grounds that `version-4/README.md` frame 1e names as a fixed bug.
     */
    @Test
    fun stripColoursLeaveTheSkinRampForTheNightRampInDark() {
        Industry.apply(DeskSkin.Steel)
        val light = lightDipi(IndustryPalette.Steel)
        val steel = IndustryPalette.Steel

        assertEquals(steel.neutral200, offlineStripFill(light))
        assertEquals(steel.neutral700, offlineStripText(light))
        assertEquals(steel.accent100, queuedStripFill(light))
        assertEquals(steel.accent800, queuedStripText(light))
        assertEquals(steel.accent700, queuedStripDim(light))
        assertEquals(steel.accent400, queuedStripBorder(light))

        assertEquals(Color(0xFF22272C), offlineStripFill(DarkDipi))
        assertEquals(Color(0xFFC3C9D0), offlineStripText(DarkDipi))
        assertEquals(Color(0xFF1D2D3D), queuedStripFill(DarkDipi))
        assertEquals(Color(0xFFB5D9FD), queuedStripText(DarkDipi))
        assertEquals(Color(0xFF749DC4), queuedStripDim(DarkDipi))
        assertEquals(Color(0xFF749DC4), queuedStripBorder(DarkDipi))
    }

    @Test
    fun nightStripsNeverBorrowASkinsLightGrounds() {
        val blossom = IndustryPalette.of(DeskSkin.Blossom)
        assertNotEquals(blossom.neutral200, offlineStripFill(DarkDipi))
        assertNotEquals(blossom.accent100, queuedStripFill(DarkDipi))
        assertNotEquals(blossom.accent800, queuedStripText(DarkDipi))
    }

    @Test
    fun bothStripsStillRenderOnTheNightRamp() {
        val at = Instant.parse("2026-08-28T09:30:00Z").toEpochMilli()
        rule.setContent {
            DipiTheme(dark = true) {
                SyncBannerStrips(offline = true, queued = 2, lastTryAtMs = at, onRetry = {})
            }
        }
        rule.onNodeWithTag("offline-strip").assertIsDisplayed()
        rule.onNodeWithTag("queued-strip").assertIsDisplayed()
        rule.onNodeWithText("changes waiting to sync").assertIsDisplayed()
        rule.onNodeWithText(lastTryLabel(at, ZoneId.systemDefault())!!).assertIsDisplayed()
        rule.onNodeWithText("RETRY").assertIsDisplayed()
    }

    @Test
    fun queuedStripOmitsTheLastTryLineOnAFreshProcess() {
        rule.setContent {
            DipiTheme {
                SyncBannerStrips(offline = false, queued = 2, lastTryAtMs = null, onRetry = {})
            }
        }
        rule.onNodeWithTag("queued-strip").assertIsDisplayed()
        rule.onAllNodesWithText("last try", substring = true).assertCountEquals(0)
    }
}
