package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.ui.SyncBanner
import org.dhamma.dipi.staff.ui.SyncBannerStrips
import org.dhamma.dipi.staff.ui.syncBanners
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
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
}
