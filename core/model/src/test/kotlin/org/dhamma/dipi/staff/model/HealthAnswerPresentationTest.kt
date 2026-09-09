package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthAnswerPresentationTest {

    @Test
    fun presenceDoesNotMeanYes() {
        assertEquals(
            HealthAnswerKind.EXPLICIT_NO,
            healthAnswerKind(HealthRow("Medication", "No"), Gender.F),
        )
        assertEquals(
            HealthAnswerKind.NOT_PROVIDED,
            healthAnswerKind(HealthRow("Physical", ""), Gender.F),
        )
        assertEquals(
            HealthAnswerKind.RECORDED,
            healthAnswerKind(HealthRow("Physical", "Yes - source narrative"), Gender.F),
        )
        assertEquals(
            HealthAnswerKind.RECORDED,
            healthAnswerKind(HealthRow("Physical", "No longer taking medication"), Gender.F),
        )
    }

    @Test
    fun blankWhitespaceDashAndEnDashAreNotProvided() {
        assertEquals(HealthAnswerKind.NOT_PROVIDED, healthAnswerKind(HealthRow("Physical", ""), Gender.F))
        assertEquals(HealthAnswerKind.NOT_PROVIDED, healthAnswerKind(HealthRow("Physical", "   "), Gender.F))
        assertEquals(HealthAnswerKind.NOT_PROVIDED, healthAnswerKind(HealthRow("Physical", "-"), Gender.F))
        assertEquals(HealthAnswerKind.NOT_PROVIDED, healthAnswerKind(HealthRow("Physical", "–"), Gender.F))
        assertEquals("No answer in the source", healthSourceCaption(HealthRow("Physical", ""), HealthAnswerKind.NOT_PROVIDED))
        assertEquals("Source value: -", healthSourceCaption(HealthRow("Physical", "-"), HealthAnswerKind.NOT_PROVIDED))
        assertEquals("Source value: –", healthSourceCaption(HealthRow("Physical", "–"), HealthAnswerKind.NOT_PROVIDED))
    }

    @Test
    fun exactYesNoAnyCaseKeepsOriginalBody() {
        val yes = HealthRow("Mental", " YES ")
        assertEquals(HealthAnswerKind.EXPLICIT_YES, healthAnswerKind(yes, Gender.F))
        assertEquals(" YES ", healthSourceCaption(yes, HealthAnswerKind.EXPLICIT_YES))
        val no = HealthRow("Mental", "no")
        assertEquals(HealthAnswerKind.EXPLICIT_NO, healthAnswerKind(no, Gender.F))
        assertEquals("Yes · as supplied", healthBadgeLabel(HealthAnswerKind.EXPLICIT_YES))
        assertEquals("No · as supplied", healthBadgeLabel(HealthAnswerKind.EXPLICIT_NO))
    }

    @Test
    fun prefixAndArbitraryTextStayRecorded() {
        assertEquals(
            HealthAnswerKind.RECORDED,
            healthAnswerKind(HealthRow("Physical", "Yesterday the pain returned"), Gender.F),
        )
        assertEquals(
            HealthAnswerKind.RECORDED,
            healthAnswerKind(HealthRow("Physical", "Not applicable"), Gender.F),
        )
        assertEquals("Response recorded", healthBadgeLabel(HealthAnswerKind.RECORDED))
    }

    @Test
    fun malePregnancyIsNotApplicableAndExcludedFromCounts() {
        val empty = ApplicationCard.HEALTH_ORDER.map { HealthRow(it, "") }
        val counts = healthAnswerCounts(empty, Gender.M)
        assertEquals(0, counts.recorded)
        assertEquals(5, counts.notProvided)
        assertEquals(1, counts.notApplicable)
        assertEquals(6, counts.total)
        assertEquals(HealthAnswerKind.NOT_APPLICABLE, healthAnswerKind(HealthRow("Pregnancy", ""), Gender.M))
        assertEquals(HealthAnswerKind.NOT_APPLICABLE, healthAnswerKind(HealthRow("Pregnancy", "Yes"), Gender.M))
        val unexpected = HealthRow("Pregnancy", "source note")
        assertEquals("source note", healthSourceCaption(unexpected, HealthAnswerKind.NOT_APPLICABLE))
    }

    @Test
    fun femaleFixtureCountsFourRecordedTwoMissing() {
        val rows = listOf(
            HealthRow("Physical", ""),
            HealthRow("Mental", "-"),
            HealthRow("Medication", "No"),
            HealthRow("Intoxicants", "invented multi-paragraph text"),
            HealthRow("Other Techniques", "Yes - source narrative"),
            HealthRow("Pregnancy", "Yes"),
        )
        val counts = healthAnswerCounts(rows, Gender.F)
        assertEquals(4, counts.recorded)
        assertEquals(2, counts.notProvided)
        assertEquals(0, counts.notApplicable)
        assertEquals("4 responses recorded · 2 not provided", healthSummaryText(counts))
        assertEquals("1 response recorded · 5 not provided", healthSummaryText(HealthAnswerCounts(1, 5, 0)))
    }

    @Test
    fun positionsAlwaysSixEvenWhenPartial() {
        val card = ApplicationCard(name = "Synthetic Applicant 01", health = listOf(HealthRow("Physical", "No")))
        val positions = healthAnswerPositions(card)
        assertEquals(ApplicationCard.HEALTH_ORDER, positions.map { it.label })
        assertEquals("No", positions[0].answer)
        assertEquals("", positions[1].answer)
    }

    @Test
    fun completeZeroHistoryRequiresAllTenValidZeroKeys() {
        val zeros = ApplicationCard.HISTORY_ORDER.map { it to 0 }
        val complete = ApplicationCard(
            name = "Synthetic Applicant 01",
            historyCounts = zeros,
            historyCountsPresent = ApplicationCard.HISTORY_ORDER.toSet(),
        )
        assertTrue(complete.completeZeroHistory())
        val missing = complete.copy(historyCountsPresent = ApplicationCard.HISTORY_ORDER.drop(1).toSet())
        assertFalse(missing.completeZeroHistory())
        val positive = complete.copy(historyCounts = ApplicationCard.HISTORY_ORDER.map { it to if (it == "10-Day") 1 else 0 })
        assertFalse(positive.completeZeroHistory())
        val legacy = complete.copy(historyCountsPresent = null)
        assertFalse(legacy.completeZeroHistory())
        assertEquals("Not provided", missing.historyTileValue(ApplicationCard.HISTORY_ORDER.first()))
        assertEquals("0", legacy.historyTileValue("Teen"))
    }
}
