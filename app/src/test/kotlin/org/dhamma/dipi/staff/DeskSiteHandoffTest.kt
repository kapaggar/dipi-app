package org.dhamma.dipi.staff

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Course
import org.dhamma.dipi.staff.model.CourseId
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.DeskSiteDestination
import org.dhamma.dipi.staff.ui.DeskSiteLauncher
import org.dhamma.dipi.staff.ui.DeskUiState
import org.dhamma.dipi.staff.ui.DipiAppUi
import org.dhamma.dipi.staff.ui.openDeskSite
import org.dhamma.dipi.staff.ui.url
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import okhttp3.mockwebserver.MockWebServer

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class DeskSiteHandoffTest {
    @get:Rule
    val rule = createComposeRule()

    private val server = MockWebServer().apply { dispatcher = DipiMockDispatcher() }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private val session = Session(
        uid = 5,
        name = "registrar.sudha",
        displayName = "registrar.sudha",
        centres = listOf(Centre(CentreId(12), "Dhamma Sudha")),
        modeTest = false,
    )

    private val course = Course(
        id = CourseId(77),
        centreId = CentreId(12),
        name = "10-Day",
        start = "2026-09-02",
        end = "2026-09-13",
    )

    private fun vmFor(state: DeskUiState) = buildTestVm(server, pinPrefsName = "desk_site_handoff").vm
        .also { it.seedForTest(state) }

    @Test
    fun destinationsBuildOnlyTheTwoApprovedDeskUrls() {
        assertEquals(
            "https://dipi.vridhamma.org/search-app/12",
            DeskSiteDestination.AdvancedSearch(12).url("https://dipi.vridhamma.org/"),
        )
        assertEquals(
            "https://dipi.vridhamma.org/app/add/12/77",
            DeskSiteDestination.AddApplication(12, 77).url("https://dipi.vridhamma.org/"),
        )
    }

    @Test
    fun launcherUsesAnUnpackagedViewIntentAndReturnsFalseWhenNoBrowserExists() {
        val opened = RecordingContext()
        assertTrue(opened.openDeskSite(DeskSiteDestination.AdvancedSearch(12)))
        assertEquals(Intent.ACTION_VIEW, opened.intent?.action)
        assertEquals("https://dipi.vridhamma.org/search-app/12", opened.intent?.data.toString())
        assertNull(opened.intent?.`package`)

        assertFalse(MissingBrowserContext().openDeskSite(DeskSiteDestination.AdvancedSearch(12)))
    }

    @Test
    fun advancedSearchHandoffOpensBrowserAndKeepsTheSearchScreen() {
        server.start()
        val vm = vmFor(DeskUiState(screen = DeskScreen.Search, session = session))
        var url: String? = null
        rule.setContent {
            DipiAppUi(vm, deskSiteLauncher = DeskSiteLauncher { destination ->
                url = destination.url()
                true
            })
        }

        rule.onNodeWithText("Full Advanced Search on the desk site ↗").performClick()

        assertEquals("${BuildConfig.BASE_URL}/search-app/12", url)
        assertEquals(DeskScreen.Search, vm.state.value.screen)
    }

    @Test
    fun addApplicationHandoffOpensBrowserAndKeepsTheCourseHub() {
        server.start()
        val vm = vmFor(DeskUiState(screen = DeskScreen.CourseHub, session = session, course = course))
        var url: String? = null
        rule.setContent {
            DipiAppUi(vm, deskSiteLauncher = DeskSiteLauncher { destination ->
                url = destination.url()
                true
            })
        }

        rule.onNodeWithContentDescription("Desk site links").performClick()
        rule.onNodeWithText("Add Application ↗").performScrollTo().performClick()

        assertEquals("${BuildConfig.BASE_URL}/app/add/12/77", url)
        assertEquals(DeskScreen.CourseHub, vm.state.value.screen)
    }

    @Test
    fun browserFailureShowsLocalMessageAndKeepsTheSearchScreen() {
        server.start()
        val vm = vmFor(DeskUiState(screen = DeskScreen.Search, session = session))
        rule.setContent {
            DipiAppUi(vm, deskSiteLauncher = DeskSiteLauncher { false })
        }

        rule.onNodeWithText("Full Advanced Search on the desk site ↗").performClick()

        rule.onNodeWithText("No browser can open the desk site").assertIsDisplayed()
        assertEquals(DeskScreen.Search, vm.state.value.screen)
    }

    private class RecordingContext : ContextWrapper(RuntimeEnvironment.getApplication()) {
        var intent: Intent? = null

        override fun startActivity(intent: Intent) {
            this.intent = intent
        }
    }

    private class MissingBrowserContext : ContextWrapper(RuntimeEnvironment.getApplication()) {
        override fun startActivity(intent: Intent) {
            throw ActivityNotFoundException()
        }
    }
}
