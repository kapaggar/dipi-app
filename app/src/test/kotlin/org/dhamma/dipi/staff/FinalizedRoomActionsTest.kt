package org.dhamma.dipi.staff

import androidx.compose.ui.test.junit4.createComposeRule
import okhttp3.mockwebserver.MockWebServer
import org.dhamma.dipi.staff.model.*
import org.dhamma.dipi.staff.network.DipiMockDispatcher
import org.dhamma.dipi.staff.ui.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FinalizedRoomActionsTest {
    @get:Rule val rule = createComposeRule()
    @Test fun finalizedCourseCannotEditUndoSyncOrFetchZeroDay() {
        val server = MockWebServer().apply { dispatcher = DipiMockDispatcher(); start() }
        try {
            val vm = buildTestVm(server).vm
            val card = org.dhamma.dipi.staff.network.SearchPageParser.parse("""<script>var dataset = [{"aid":31,"centreid":63,"courseid":77,"name":"Test Student","gender":"M","confno":"OM1","app_status":"Attended","finalized":1,"section":"Mbk","acc":"12"}];</script>""").dataset.single().toModel()
            val records = mapOf(card.id to CheckInRecord(checkedIn = true, room = "Mbk 9"))
            rule.runOnIdle {
                vm.seedForTest(DeskUiState(course = Course(CourseId(77), CentreId(63), "Test", "", ""),
                    rows = listOf(card), courseFinalized = true, checkIns = records))
                vm.openDeskMark(card)
                assertNull(vm.state.value.deskMarkId)
                vm.seedForTest(vm.state.value.copy(deskMarkId = card.id))
                vm.setDeskRoom("Mbk 5"); vm.saveDeskMark(); vm.undoDeskMark()
                vm.syncRooms(); vm.pullRooms(userInitiated = false)
            }
            rule.waitForIdle()
            assertEquals(records, vm.state.value.checkIns)
            assertEquals(0, server.requestCount)
        } finally { server.shutdown() }
    }
}
