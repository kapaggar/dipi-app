package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.DailyActivityScreen
import org.dhamma.dipi.staff.model.DailyActivityPage
import org.dhamma.dipi.staff.model.DailyActivityRow
import org.dhamma.dipi.staff.model.NamedOption
import org.dhamma.dipi.staff.model.DailyActivityForm
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DailyActivityScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun listsRowsAndFiresEventFilter() {
        var event = ""
        var applied = false
        val page = DailyActivityPage(
            form = DailyActivityForm(
                action = "/daily-activity/1",
                formBuildId = "x",
                formToken = "t",
                formId = "dh_daily_activity_form",
                courses = emptyList(),
                events = listOf(NamedOption("Letter", "Letter")),
                users = emptyList(),
                startDate = "2026-08-17",
                endDate = "2026-08-17",
            ),
            rows = listOf(
                DailyActivityRow("Meera Deshpande", "10-Day", "Status Change", "Confirmed", "sudha.user", "2026-08-16 10:22:00"),
            ),
        )
        rule.setContent {
            DipiTheme {
                DailyActivityScreen(
                    page = page,
                    event = event,
                    loading = false,
                    error = null,
                    onEvent = { event = it },
                    onApply = { applied = true },
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Daily Activity").assertIsDisplayed()
        rule.onNodeWithText("Meera Deshpande").assertIsDisplayed()
        rule.onNodeWithText("Confirmed").assertIsDisplayed()
        rule.onNodeWithText("Letter").performClick()
        assertEquals("Letter", event)
        rule.onNodeWithText("Apply").performClick()
        assertTrue(applied)
    }
}
