package org.dhamma.dipi.staff.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CentreHallParserTest {

    @Test
    fun readsSudhaMainPlanFromHarShapedEditHtml() {
        val hall = CentreHallParser.settingsOrNull(MockFixtures.centreEditHallHtml)
        assertEquals(true, hall?.combinedHall)
        assertEquals(1, hall?.seatNaming)
        assertEquals(5, hall?.maleSeatsPerRow)
        assertEquals(2, hall?.femaleSeatsPerRow)
        assertEquals(5, hall?.malePlan?.columns)
        assertEquals(1, hall?.malePlan?.chowkyColumns)
        assertEquals("right", hall?.malePlan?.direction)
        assertEquals("", hall?.malePlan?.chowkyPosition)
        assertEquals(0, hall?.malePlan?.emptySeatCount())
        assertEquals(2, hall?.femalePlan?.columns)
        assertEquals(2, hall?.femalePlan?.chowkyColumns)
        assertEquals("left", hall?.femalePlan?.direction)
        assertEquals("", hall?.femalePlan?.chowkyPosition)
        assertEquals(0, hall?.femalePlan?.emptySeatCount())
    }

    @Test
    fun readsSeparateHallSamePlanNumbers() {
        val hall = CentreHallParser.settingsOrNull(MockFixtures.centreEditHallSeparateHtml)
        assertEquals(false, hall?.combinedHall)
        assertEquals(1, hall?.seatNaming)
        assertEquals(5, hall?.malePlan?.columns)
        assertEquals(1, hall?.malePlan?.chowkyColumns)
        assertEquals(2, hall?.femalePlan?.columns)
        assertEquals(2, hall?.femalePlan?.chowkyColumns)
    }

    @Test
    fun readsOlderIniTextareaWhenSeatcfgFieldsAreMissing() {
        val html = """
            <form>
              <input type="radio" name="cs_hall_combined" value="0" checked="checked" />
              <input type="radio" name="cs_seat_naming_conv" value="1" checked="checked" />
              <textarea name="cs_seat_config">[RIGHT]
SeatsPerRow = 5
SeatsPerRowChowky = 1
SeatDirection = right
ChowkyPosition = right

[LEFT]
SeatsPerRow = 2
SeatsPerRowChowky = 2
SeatDirection = left
ChowkyPosition = left
</textarea>
            </form>
        """.trimIndent()
        val hall = CentreHallParser.settingsOrNull(html)
        assertEquals(false, hall?.combinedHall)
        assertEquals(5, hall?.malePlan?.columns)
        assertEquals(1, hall?.malePlan?.chowkyColumns)
        assertEquals("right", hall?.malePlan?.direction)
        assertEquals(2, hall?.femalePlan?.columns)
        assertEquals(2, hall?.femalePlan?.chowkyColumns)
        assertEquals("left", hall?.femalePlan?.direction)
    }

    @Test
    fun loginHtmlIsIgnored() {
        assertNull(CentreHallParser.settingsOrNull("<html><body>Access denied</body></html>"))
        assertNull(CentreHallParser.settingsOrNull(""))
    }

    @Test
    fun sudhaStyleAccoInventoryKeepsSectionSpaceNumberCodes() {
        val body = """{"data":[
          {"DT_RowId":"row_1","dh_center_setting_acco":{"csa_gender":"M","csa_section":"Mbk","csa_room":"1, 51, 70","csa_deleted":"0"}},
          {"DT_RowId":"row_2","dh_center_setting_acco":{"csa_gender":"F","csa_section":"Fbk","csa_room":"1, 3","csa_deleted":"0"}},
          {"DT_RowId":"row_3","dh_center_setting_acco":{"csa_gender":"M","csa_section":"Guest","csa_room":"GR1,GR2,GR3","csa_deleted":"0"}}
        ],"options":{},"files":[]}"""
        val rooms = AccoHandlerParser.roomsOrNull(body).orEmpty()
        assertEquals(listOf("Mbk 1", "Mbk 51", "Mbk 70", "Fbk 1", "Fbk 3", "Guest GR1", "Guest GR2", "Guest GR3"), rooms.map { it.code })
        assertTrue(rooms.any { it.section == "Guest" })
    }
}
