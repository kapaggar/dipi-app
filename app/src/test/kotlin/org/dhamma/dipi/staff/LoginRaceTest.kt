package org.dhamma.dipi.staff

import androidx.compose.ui.test.junit4.createComposeRule
import okhttp3.mockwebserver.*
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.ui.DeskScreen
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class LoginRaceTest {
    @get:Rule val rule = createComposeRule()
    @Test fun manualSignInWaitsForExpiredRestoreAndDuplicateTapsDoNotPostTwice() {
        val release = CountDownLatch(1)
        val entered = CountDownLatch(1)
        val posts = AtomicInteger()
        val sessions = AtomicInteger()
        val delegate = DipiMockDispatcher()
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path == "/staff/session" && sessions.incrementAndGet() == 1) {
                    entered.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    return MockResponse().setResponseCode(403).setBody("Access denied")
                }
                if (request.path?.startsWith("/api/user/login") == true) posts.incrementAndGet()
                return delegate.dispatch(request)
            }
        }
        server.start()
        try {
            val built = buildTestVm(server, cookie = "SESS=expired")
            val prefs = org.robolectric.RuntimeEnvironment.getApplication()
                .getSharedPreferences("login_race_secure", 0)
            built.sessionStore.javaClass.getDeclaredField("secure\$delegate").apply {
                isAccessible = true
                set(built.sessionStore, lazy { prefs })
            }
            val vm = built.vm
            rule.awaitTrue("restore started") { entered.count == 0L }
            rule.runOnIdle {
                vm.onUser("sudha.user"); vm.onPass("password")
                vm.signIn(); vm.signIn()
            }
            rule.waitForIdle()
            assertEquals("login must wait until stale-session cleanup is finished", 0, posts.get())
            release.countDown()
            try {
                val deadline = System.currentTimeMillis() + 20_000
                while (vm.state.value.screen != DeskScreen.Centre && System.currentTimeMillis() < deadline) {
                    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
                    Thread.sleep(20)
                }
                assertEquals(DeskScreen.Centre, vm.state.value.screen)
            } catch (e: AssertionError) {
                throw AssertionError("screen=${vm.state.value.screen} loading=${vm.state.value.loginLoading} error=${vm.state.value.loginError} posts=${posts.get()} requests=${server.requestCount}", e)
            }
            assertEquals(1, posts.get())
            assertNull(vm.state.value.loginError)
            assertFalse(vm.state.value.loginLoading)
        } finally { release.countDown(); server.shutdown() }
    }
}
