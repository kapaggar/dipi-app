package org.dhamma.dipi.staff

import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.width
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.dp
import org.dhamma.dipi.staff.ui.theme.deskCard
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.course.CentreScreen
import org.dhamma.dipi.staff.desk.SheetViewerPane
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.teacher.StudentCardScreen
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Review-only probes. Synthetic fixtures. Source is outside the repository. */
@RunWith(RobolectricTestRunner::class)
class CourseOpsResponsiveTest {
    @get:Rule val rule = createComposeRule()

    @Test
    @Config(qualifiers = "w412dp-h900dp-mdpi")
    fun loadedPhoneAtLargeFontHasOneScrollAndReachableAnswers() {
        val row = RollRow(
            sn = 1, applicantId = ApplicantId(4), name = "Synthetic Applicant With A Long Family Name", roleTag = null,
            room = "M8", age = "51", city = "Synthetic", courses = listOf("10D" to 11),
            cell = "", seat = "A3", seatKind = SeatKind.CELL, backrest = true,
            occupation = "Test", education = "Test", languages = "Test",
        )
        val group = RollGroup(at = "Synthetic Teacher", code = "TAM", gender = Gender.M,
            seniority = RollSeniority.OLD, group = "1", total = 1, rows = listOf(row))
        val card = ApplicationCard(name = row.name, conf = "OM42", hasPhoto = false,
            health = ApplicationCard.HEALTH_ORDER.map { HealthRow(it, "A synthetic answer.\n".repeat(24)) })
        rule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.3f),
            ) { DipiTheme { StudentCardScreen(row = row, group = group, card = card, canNext = true) } }
        }
        rule.waitForIdle()
        rule.onNodeWithTag("card-phone-scroll").assertExists()
        rule.onNodeWithTag("answer-body-Physical").performScrollTo().assertIsDisplayed()
        val next = rule.onNodeWithTag("card-next").getUnclippedBoundsInRoot()
        assertTrue(next.width.value >= 48f && next.height.value >= 48f)
        val name = rule.onNodeWithTag("card-name").getUnclippedBoundsInRoot()
        assertTrue(name.width.value >= 200f)
        rule.onNodeWithTag("card-next").assertIsDisplayed()
    }


    @Test
    @Config(qualifiers = "w1240dp-h844dp-land-mdpi")
    fun teacherNameWrapsAtLargeFontWithoutTakingSeatTrack() {
        val row = RollRow(sn = 1, applicantId = ApplicantId(4),
            name = "Synthetic Applicant\nWith Long Family Name", roleTag = "Sevak",
            room = "M8", age = "51", city = "Synthetic", courses = emptyList(),
            cell = "", seat = "A3", seatKind = SeatKind.CELL, backrest = true,
            occupation = "Test", education = "Test", languages = "Test")
        val group = RollGroup(at = "Synthetic Teacher", code = "TAM", gender = Gender.M,
            seniority = RollSeniority.OLD, group = "1", total = 1, rows = listOf(row))
        rule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.3f),
            ) { DipiTheme { org.dhamma.dipi.staff.teacher.TeacherListScreen(
                roll = TeacherRoll(listOf(group)), courseLine = "Synthetic course") } }
        }
        val name = rule.onNodeWithTag("roll-name-1", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Two name lines must remain visible: $name", name.height.value >= 34f)
        assertTrue("Name must retain a useful track: $name", name.width.value >= 160f)
        rule.onNodeWithText("A3", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun steelNightUsesActualUiSurfacesAndRestoresRememberedBlossom() {
        val dark = androidx.compose.runtime.mutableStateOf(false)
        var background = androidx.compose.ui.graphics.Color.Transparent
        var caption = background
        var card = background
        org.dhamma.dipi.staff.ui.theme.Industry.apply(org.dhamma.dipi.staff.ui.theme.DeskSkin.Blossom)
        try {
            rule.setContent { DipiTheme(dark = dark.value) {
                background = org.dhamma.dipi.staff.ui.theme.ThemeIndustry.bg
                caption = org.dhamma.dipi.staff.ui.theme.ThemeIndustry.caption
                card = org.dhamma.dipi.staff.ui.theme.ThemeIndustry.card
            } }
            rule.waitForIdle()
            val lightBackground = background
            rule.runOnIdle { dark.value = true }
            rule.waitForIdle()
            org.junit.Assert.assertEquals(org.dhamma.dipi.staff.ui.theme.DarkDipi.background, background)
            org.junit.Assert.assertEquals(org.dhamma.dipi.staff.ui.theme.DarkDipi.field, card)
            org.junit.Assert.assertEquals(org.dhamma.dipi.staff.ui.theme.DarkDipi.muted, caption)
            rule.runOnIdle { dark.value = false }
            rule.waitForIdle()
            org.junit.Assert.assertEquals(lightBackground, background)
            org.junit.Assert.assertEquals(org.dhamma.dipi.staff.ui.theme.DeskSkin.Blossom,
                org.dhamma.dipi.staff.ui.theme.Industry.skin)
        } finally {
            org.dhamma.dipi.staff.ui.theme.Industry.apply(org.dhamma.dipi.staff.ui.theme.DeskSkin.Steel)
        }
    }

    @Test
    fun sharedDeskCardUsesDarkGroundWithReadableText() {
        var foreground = androidx.compose.ui.graphics.Color.Transparent
        var ground = foreground
        rule.setContent { DipiTheme(dark = true) {
            foreground = org.dhamma.dipi.staff.ui.theme.ThemeIndustry.text
            ground = org.dhamma.dipi.staff.ui.theme.DeskStyle.cardFill
            androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.size(100.dp).deskCard().testTag("native-card"),
            ) {
                androidx.compose.material3.Text("Synthetic", color = foreground)
            }
        } }
        rule.onNodeWithTag("native-card").assertIsDisplayed()
        val a = foreground.luminance().toDouble()
        val b = ground.luminance().toDouble()
        assertTrue("Shared card text contrast", (maxOf(a, b) + .05) / (minOf(a, b) + .05) >= 4.5)
        assertTrue("Shared deskCard must use a dark ground", b < .03)
    }
}
