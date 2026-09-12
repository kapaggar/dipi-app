package org.dhamma.dipi.staff

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import org.dhamma.dipi.staff.desk.ApplicationsPane
import org.dhamma.dipi.staff.desk.AuditPane
import org.dhamma.dipi.staff.desk.BoardPane
import org.dhamma.dipi.staff.desk.CallingPane
import org.dhamma.dipi.staff.desk.CheckInPane
import org.dhamma.dipi.staff.desk.DeskCourse
import org.dhamma.dipi.staff.desk.DeskRail
import org.dhamma.dipi.staff.desk.DeskSection
import org.dhamma.dipi.staff.desk.DeskShell
import org.dhamma.dipi.staff.desk.RoomsPane
import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.Centre
import org.dhamma.dipi.staff.model.CentreId
import org.dhamma.dipi.staff.model.Gender
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.settings.SettingsScreen
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.dhamma.dipi.staff.ui.theme.Industry
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.deskColors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Rendered regression coverage for the theme boundary. This deliberately
 * samples actual modifier backgrounds after composing desk panes, rather than
 * asserting composition locals alone. It uses synthetic empty data plus one
 * available room; no server fixture or student data is involved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1240dp-h844dp-land")
class ThemeHonestyTest {
    @get:Rule val rule = createComposeRule()

    @After fun restoreSavedSkin() = Industry.apply(DeskSkin.Steel)

    @Test
    fun allSkinsAndModesRenderEveryDeskPaneAndSettingsFromTheEffectiveTheme() {
        val inspectorWasEnabled = isDebugInspectorInfoEnabled
        isDebugInspectorInfoEnabled = true
        try {
            val darkState = mutableStateOf(false)
            val paneState = mutableStateOf(ThemePane.Board)
            val skinState = mutableStateOf(DeskSkin.Steel)
            rule.setContent {
                // The saved preference is mutated by the test before each case;
                // skinState makes that snapshot change visible in this one composition.
                skinState.value
                DipiTheme(dark = darkState.value) {
                    when (paneState.value) {
                        ThemePane.Settings -> SettingsScreen(
                            session = session,
                            dark = darkState.value,
                            lastSync = null,
                            queued = 0,
                            offline = false,
                            onToggleTheme = {},
                            onLogout = {},
                            skin = skinState.value,
                            mode = org.dhamma.dipi.staff.model.TabletMode.COURSE_OPS,
                            runningCourseName = "10 Day",
                            runningCourseDates = "1-10 Jan",
                        )
                        else -> DeskShell(
                            section = paneState.value.section,
                            rail = DeskRail("Test", "Online"),
                            course = DeskCourse("10 Day", "DAY 0"),
                            clock = "09:00",
                            onSection = {},
                        ) {
                            when (paneState.value) {
                                ThemePane.Board -> BoardPane(emptyList(), emptyMap(), emptyList(), emptyMap(), {}, {})
                                ThemePane.Applications -> ApplicationsPane(emptyList(), emptyMap(), null, {}, {}, {}, {})
                                ThemePane.Audit -> AuditPane(emptyList(), null, {}, { _, _ -> }, {})
                                ThemePane.Calling -> CallingPane(emptyList(), emptyMap(), "To call", {}, { _, _ -> }, {}, {}, { _, _ -> })
                                ThemePane.CheckIn -> CheckInPane(emptyList(), emptyMap(), emptyList(), "", "All", emptySet(), onScan = {}, onFilter = {}, onOpen = {})
                                ThemePane.Rooms -> RoomsPane(emptyList(), emptyMap(), listOf(AccoRoom("A 1", Gender.F, "A", number = "1")))
                                ThemePane.Settings -> error("Settings renders outside the desk shell")
                            }
                        }
                    }
                }
            }
            for (skin in DeskSkin.entries) for (dark in listOf(false, true)) {
                rule.runOnIdle {
                    Industry.apply(skin)
                    skinState.value = skin
                    darkState.value = dark
                    paneState.value = ThemePane.Board
                }
                val expected = if (dark) IndustryPalette.SteelNight else IndustryPalette.of(skin)
                val roles = deskColors(IndustryPalette.of(skin), dark)

                rule.onNodeWithTag("theme-desk-ground").assertIsDisplayed()
                assertEquals(expected.bg, rule.onNodeWithTag("theme-desk-ground").backgroundColor())
                for (index in 0..3) assertEquals(roles.cardFill, rule.onAllNodesWithTag("board-stat")[index].backgroundColor())
                for (index in 0..3) {
                    assertEquals(
                        roles.caption,
                        rule.onNodeWithTag("theme-board-caption-$index", useUnmergedTree = true).textColor(),
                    )
                }
                repeat(9) { index ->
                    assertEquals(roles.exportTile, rule.onAllNodesWithTag("export-chip")[index].backgroundColor())
                }

                listOf(ThemePane.Applications, ThemePane.Audit, ThemePane.Calling, ThemePane.CheckIn, ThemePane.Rooms).forEach { pane ->
                    rule.runOnIdle { paneState.value = pane }
                    rule.onNodeWithTag("theme-desk-ground").assertIsDisplayed()
                    assertEquals(expected.bg, rule.onNodeWithTag("theme-desk-ground").backgroundColor())
                }
                assertEquals(roles.subtleSurface, rule.onNodeWithTag("room-cell-free").backgroundColor())

                rule.runOnIdle { paneState.value = ThemePane.Settings }
                rule.onNodeWithTag("theme-settings-mode-desk").assertIsDisplayed()
                rule.onNodeWithTag("theme-settings-mode-course-ops").assertIsDisplayed()
                assertEquals(roles.subtleSurface, rule.onNodeWithTag("mode-desk", useUnmergedTree = true).backgroundColor())
                assertEquals(roles.whiteSurface, rule.onNodeWithTag("mode-course-ops", useUnmergedTree = true).backgroundColor())
                assertEquals(roles.whiteSurface, rule.onNodeWithTag("theme-settings-course").backgroundColor())
            }
        } finally {
            isDebugInspectorInfoEnabled = inspectorWasEnabled
        }
    }

    private enum class ThemePane(val section: DeskSection) {
        Board(DeskSection.Board), Applications(DeskSection.Applications), Audit(DeskSection.Audit),
        Calling(DeskSection.Calling), CheckIn(DeskSection.CheckIn), Rooms(DeskSection.Rooms),
        Settings(DeskSection.Board),
    }

    private fun SemanticsNodeInteraction.backgroundColor(): Color =
        fetchSemanticsNode().layoutInfo.getModifierInfo()
            .asSequence()
            .map { it.modifier }
            .filterIsInstance<InspectableValue>()
            .filter { it.nameFallback == "background" }
            .flatMap { modifier ->
                @Suppress("UNCHECKED_CAST")
                (InspectableValue::class.java.getMethod("getInspectableElements").invoke(modifier) as Sequence<Any>).asIterable()
            }
            .first { it.javaClass.getMethod("getName").invoke(it) == "color" }
            .let { it.javaClass.getMethod("getValue").invoke(it) as Color }

    /** TextLayoutResult records the composed TextStyle passed to the renderer. */
    private fun SemanticsNodeInteraction.textColor(): Color {
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style.color
    }

    private val session = Session(0, "theme", "theme", listOf(Centre(CentreId(1), "Theme Centre")), false)
}
