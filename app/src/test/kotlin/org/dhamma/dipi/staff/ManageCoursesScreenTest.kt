package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.dhamma.dipi.staff.course.ManageCoursesScreen
import org.dhamma.dipi.staff.model.ManagedCourse
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ManageCoursesScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showsTypeDatesCohortsAndFinalized() {
        rule.setContent {
            DipiTheme {
                ManageCoursesScreen(
                    rows = listOf(
                        ManagedCourse(
                            id = 10,
                            type = "10-Day",
                            start = "20 Aug 2026",
                            end = "31 Aug 2026",
                            cancelled = false,
                            status = "Open",
                            statusNm = "Open",
                            statusOm = "Open",
                            statusNf = "FastFilling",
                            statusOf = "Open",
                            statusSvrM = "Open",
                            statusSvrF = "Open",
                            comments = "",
                            description = "",
                            finalized = false,
                        ),
                        ManagedCourse(
                            id = 8,
                            type = "10-Day",
                            start = "06 Aug 2026",
                            end = "17 Aug 2026",
                            cancelled = false,
                            status = "Completed",
                            statusNm = "Closed",
                            statusOm = "Closed",
                            statusNf = "Closed",
                            statusOf = "Closed",
                            statusSvrM = "Closed",
                            statusSvrF = "Closed",
                            comments = "",
                            description = "",
                            finalized = true,
                        ),
                    ),
                    loading = false,
                    error = null,
                    onBack = {},
                )
            }
        }
        rule.onNodeWithText("Manage Courses").assertIsDisplayed()
        rule.onNodeWithText("20 Aug 2026 – 31 Aug 2026").assertIsDisplayed()
        rule.onNodeWithText("M Open/Open · F FastFilling/Open · Sevak Open/Open").assertIsDisplayed()
        rule.onNodeWithText("Finalized").assertIsDisplayed()
    }
}
