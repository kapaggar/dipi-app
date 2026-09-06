package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CentreOpsEffectTest {

    @Test
    fun allThreeOnListsEveryQuestionAndDropsTheHallSentence() {
        val prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = true)
        assertEquals(
            "Check-in: room, seating, laundry, valuables and group.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun groupsOffAddsTheHallSentence() {
        val prefs = CentreOpsPrefs(laundry = true, valuables = true, groups = false)
        assertEquals(
            "Check-in: room, seating, laundry and valuables. " +
                "Hall: Main Dhamma Hall.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun allOffStillAsksRoomAndSeating() {
        val prefs = CentreOpsPrefs(laundry = false, valuables = false, groups = false)
        assertEquals(
            "Check-in: room and seating. " +
                "Hall: Main Dhamma Hall.",
            centreOpsEffect(prefs),
        )
    }

    @Test
    fun laundryOffKeepsTheRestInOrder() {
        val prefs = CentreOpsPrefs(laundry = false, valuables = true, groups = true)
        assertEquals(
            "Check-in: room, seating, valuables and group.",
            centreOpsEffect(prefs),
        )
    }
}
