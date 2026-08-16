package org.dhamma.dipi.staff

import org.dhamma.dipi.staff.ui.DeskScreen
import org.dhamma.dipi.staff.ui.deskAfterLogin
import org.dhamma.dipi.staff.ui.deskAfterPickCourse
import org.dhamma.dipi.staff.ui.deskBack
import org.junit.Assert.assertEquals
import org.junit.Test

class DeskNavTest {
    @Test
    fun loginAndPickCourseLandOnHubs() {
        assertEquals(DeskScreen.Centre, deskAfterLogin())
        assertEquals(DeskScreen.CourseHub, deskAfterPickCourse())
    }

    @Test
    fun backStackCardPhotosSummaryToTodayToHubToCentre() {
        assertEquals(DeskScreen.Today, deskBack(DeskScreen.Card, null))
        assertEquals(DeskScreen.Today, deskBack(DeskScreen.Photos, null))
        assertEquals(DeskScreen.Today, deskBack(DeskScreen.Summary, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.Today, null))
        assertEquals(DeskScreen.Centre, deskBack(DeskScreen.CourseHub, null))
        assertEquals(DeskScreen.Centre, deskBack(DeskScreen.Centre, null))
    }

    @Test
    fun settingsAndPlaceholderReturnWhereverTheyCameFrom() {
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.Settings, DeskScreen.CourseHub))
        assertEquals(DeskScreen.Centre, deskBack(DeskScreen.Settings, DeskScreen.Centre))
        assertEquals(DeskScreen.Today, deskBack(DeskScreen.Settings, DeskScreen.Today))
        assertEquals(DeskScreen.Centre, deskBack(DeskScreen.Settings, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.DeskAction, DeskScreen.CourseHub))
        assertEquals(DeskScreen.Centre, deskBack(DeskScreen.DeskAction, DeskScreen.Centre))
    }

    @Test
    fun newScreensBackToHubRoomsToOpener() {
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.ZeroDay, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.Audit, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.Calling, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.CentreOps, null))
        assertEquals(DeskScreen.CourseHub, deskBack(DeskScreen.ZeroDay, DeskScreen.CourseHub))
        assertEquals(DeskScreen.ZeroDay, deskBack(DeskScreen.Rooms, null))
        assertEquals(DeskScreen.ZeroDay, deskBack(DeskScreen.Rooms, DeskScreen.ZeroDay))
        assertEquals(DeskScreen.CentreOps, deskBack(DeskScreen.Rooms, DeskScreen.CentreOps))
    }
}
