package org.dhamma.dipi.staff

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Settings → Simulate offline must flip the live ViewModel, not only the
 * DataStore value a cold start would read after process death.
 */
@RunWith(RobolectricTestRunner::class)
class SimulateOfflineTest {
    @get:Rule
    val rule = createComposeRule()

    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun toggleOfflineUpdatesTheSameViewModelWithoutRestart() {
        server.start()
        val t = primedVm("simulate_offline_toggle")

        t.vm.toggleOffline()
        assertTrue("simulate on must apply on the same ViewModel", t.vm.state.value.offline)
        rule.awaitTrue("force flag persisted") { runBlocking { t.sessionStore.forceOffline.first() } }

        t.vm.toggleOffline()
        assertFalse("simulate off must apply on the same ViewModel", t.vm.state.value.offline)
        rule.awaitTrue("force flag cleared") { runBlocking { !t.sessionStore.forceOffline.first() } }
    }

    @Test
    fun forceOfflineDataStoreWriteIsObservedLive() {
        server.start()
        val t = primedVm("simulate_offline_ds")

        runBlocking { t.sessionStore.setForceOffline(true) }
        rule.awaitTrue("DataStore write reaches the live ViewModel") { t.vm.state.value.offline }

        runBlocking { t.sessionStore.setForceOffline(false) }
        rule.awaitTrue("clearing the flag reaches the live ViewModel") { !t.vm.state.value.offline }
    }

    /** Shared prefs can retain a prior test's flag; wait until the collector is live and off. */
    private fun primedVm(prefs: String): TestVm {
        val t = buildTestVm(server, pinPrefsName = prefs)
        t.connectivity.setOnlineForTest(true)
        runBlocking { t.sessionStore.setForceOffline(true) }
        rule.awaitTrue("collector applied simulate on") { t.vm.state.value.offline }
        runBlocking { t.sessionStore.setForceOffline(false) }
        rule.awaitTrue("collector applied simulate off") { !t.vm.state.value.offline }
        return t
    }
}
