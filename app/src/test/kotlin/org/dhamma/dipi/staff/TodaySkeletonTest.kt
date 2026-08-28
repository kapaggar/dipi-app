package org.dhamma.dipi.staff

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import org.dhamma.dipi.staff.applicants.TodaySkeleton
import org.dhamma.dipi.staff.ui.theme.DipiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodaySkeletonTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun drawsEightRows() {
        rule.setContent { DipiTheme { TodaySkeleton() } }
        rule.onNodeWithTag("today-skeleton").assertIsDisplayed()
        rule.onAllNodesWithTag("skeleton-row").assertCountEquals(8)
    }
}
