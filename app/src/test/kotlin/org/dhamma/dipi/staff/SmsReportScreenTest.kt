package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.dhamma.dipi.staff.course.SmsReportScreen
import org.dhamma.dipi.staff.model.SmsCourseRow
import org.dhamma.dipi.staff.model.SmsLetterRow
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsReportScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun tapExpandsLetterBreakdown() {
        var opened: Int? = null
        rule.setContent {
            DipiTheme {
                SmsReportScreen(
                    rows = listOf(SmsCourseRow(10, "10-Day", 42)),
                    openId = 10,
                    letters = listOf(SmsLetterRow("12", "Confirmed", "30")),
                    lettersLoading = false,
                    loading = false,
                    error = null,
                    onExpand = { opened = it },
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("SMS Report").assertIsDisplayed()
        rule.onNodeWithText("10-Day").assertIsDisplayed()
        rule.onNodeWithText("12 · Confirmed · 30").assertIsDisplayed()
        rule.onNodeWithText("10-Day").performClick()
        assertEquals(10, opened)
    }
}
