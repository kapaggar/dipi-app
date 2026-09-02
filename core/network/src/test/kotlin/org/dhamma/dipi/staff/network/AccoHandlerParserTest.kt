package org.dhamma.dipi.staff.network

import org.dhamma.dipi.staff.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture mirrors the DataTables Editor GET payload of
 * `/centre/{cid}/acco-handler` (dh_manageapp `dh_acco_handler()`): rows keyed
 * `DT_RowId` with fields nested under `dh_center_setting_acco`, plus the
 * gender `options` block. Token semantics follow `_ajax_get_acco_options()`:
 * comma-separated, `a:b` numeric range, anything else literal.
 */
class AccoHandlerParserTest {

    private fun row(id: Int, gender: String, section: String, rooms: String, deleted: String = "0") =
        """{"DT_RowId":"row_$id","dh_center_setting_acco":{"csa_id":"$id","csa_center":"91",""" +
            """"csa_gender":"$gender","csa_section":"$section","csa_room":"$rooms","csa_deleted":"$deleted"}}"""

    private fun payload(vararg rows: String) =
        """{"data":[${rows.joinToString(",")}],""" +
            """"options":{"dh_center_setting_acco.csa_gender":[{"label":"Male","value":"M"},{"label":"Female","value":"F"}]},"files":[]}"""

    @Test
    fun expandsRangesIntoSectionSpaceNumberCodes() {
        val rooms = AccoHandlerParser.roomsOrNull(payload(row(1, "M", "Mbk", "1:3"), row(2, "F", "Fbk", "1:2"))).orEmpty()
        assertEquals(listOf("Mbk 1", "Mbk 2", "Mbk 3", "Fbk 1", "Fbk 2"), rooms.map { it.code })
        assertEquals(listOf("1", "2", "3", "1", "2"), rooms.map { it.number })
        assertEquals(Gender.M, rooms.first { it.code == "Mbk 2" }.gender)
        assertEquals(Gender.F, rooms.first { it.code == "Fbk 1" }.gender)
        assertEquals("Mbk", rooms.first().section)
    }

    @Test
    fun literalTokensAndWhitespaceMatchTheServerSplit() {
        // The server explodes on "," and trims — "1:2, 7 , 12A" is rooms 1, 2, 7, 12A.
        val rooms = AccoHandlerParser.roomsOrNull(payload(row(1, "M", "Mbk", "1:2, 7 , 12A"))).orEmpty()
        assertEquals(listOf("Mbk 1", "Mbk 2", "Mbk 7", "Mbk 12A"), rooms.map { it.code })
    }

    @Test
    fun amenityMarksMapToRoomFeatures() {
        val rooms = AccoHandlerParser.roomsOrNull(payload(row(1, "M", "Mbk", "1:2W, 3IC, 4G, 5"))).orEmpty()
        assertEquals(listOf("Mbk 1", "Mbk 2", "Mbk 3", "Mbk 4", "Mbk 5"), rooms.map { it.code })
        // Range mark applies to the whole band, like the chart's W rows.
        assertTrue(rooms.first { it.code == "Mbk 1" }.features.westernToilet)
        assertTrue(rooms.first { it.code == "Mbk 2" }.features.westernToilet)
        assertTrue(rooms.first { it.code == "Mbk 3" }.features.indianToilet)
        assertTrue(rooms.first { it.code == "Mbk 4" }.features.geyser)
        val plain = rooms.first { it.code == "Mbk 5" }
        assertFalse(plain.features.geyser || plain.features.indianToilet || plain.features.westernToilet)
        assertEquals("W", rooms.first { it.code == "Mbk 1" }.amenityMark)
        assertEquals("IC", rooms.first { it.code == "Mbk 3" }.amenityMark)
        assertEquals("", plain.amenityMark)
    }

    @Test
    fun deletedRowsAndBlankTokensAreSkipped() {
        val rooms = AccoHandlerParser.roomsOrNull(
            payload(row(1, "F", "Old", "1:5", deleted = "1"), row(2, "F", "Fbk", "1,,2, ")),
        ).orEmpty()
        assertEquals(listOf("Fbk 1", "Fbk 2"), rooms.map { it.code })
    }

    @Test
    fun genderFallsBackToTheSectionPrefixWhenBlank() {
        val rooms = AccoHandlerParser.roomsOrNull(payload(row(1, "", "Fbk", "1"), row(2, "", "Mbk", "1"))).orEmpty()
        assertEquals(Gender.F, rooms.first { it.code == "Fbk 1" }.gender)
        assertEquals(Gender.M, rooms.first { it.code == "Mbk 1" }.gender)
    }

    @Test
    fun duplicateCodesKeepTheFirstRowAndBadRangesDrop() {
        val rooms = AccoHandlerParser.roomsOrNull(payload(row(1, "M", "Mbk", "1:2, 2, 9:5"))).orEmpty()
        assertEquals(listOf("Mbk 1", "Mbk 2"), rooms.map { it.code })
    }

    @Test
    fun nonJsonAndLoginHtmlYieldNoRooms() {
        assertEquals(emptyList<Any>(), AccoHandlerParser.roomsOrNull("<html><body>Access denied</body></html>").orEmpty())
        assertEquals(emptyList<Any>(), AccoHandlerParser.roomsOrNull("""{"msg":"not here"}""").orEmpty())
        assertEquals(emptyList<Any>(), AccoHandlerParser.roomsOrNull("").orEmpty())
    }

    @Test
    fun mockFixtureParsesLikeTheLiveShape() {
        val rooms = AccoHandlerParser.roomsOrNull(MockFixtures.accoHandlerJson).orEmpty()
        assertEquals(6, rooms.count { it.gender == Gender.F })
        assertEquals(10, rooms.count { it.gender == Gender.M })
        assertTrue(rooms.first { it.code == "Fbk 3" }.features.westernToilet)
        assertTrue(rooms.first { it.code == "Mbk 9" }.features.indianToilet)
        assertTrue(rooms.first { it.code == "Mbk 10" }.features.westernToilet)
    }
}
