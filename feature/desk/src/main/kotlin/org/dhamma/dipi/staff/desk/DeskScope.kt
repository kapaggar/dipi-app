package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.AccoRoom
import org.dhamma.dipi.staff.model.ApplicantCard
import org.dhamma.dipi.staff.model.ApplicantId
import org.dhamma.dipi.staff.model.ApplicantType
import org.dhamma.dipi.staff.model.CheckInRecord
import org.dhamma.dipi.staff.model.Gender

data class RoomBlockKey(val gender: Gender, val section: String)

data class AuditOpenContext(val ruleId: String, val applicantId: ApplicantId)

data class ArrivalCounts(val roll: Int, val arrived: Int, val pending: Int, val left: Int) {
    val eligible: Int get() = arrived + pending
}

data class RoomAvailability(val block: RoomBlockKey, val total: Int, val occupied: Int) {
    val free: Int get() = total - occupied
}

fun deskIsLeft(card: ApplicantCard): Boolean = card.status.normalize() == "left"

fun deskArrivalCounts(
    scope: List<ApplicantCard>,
    records: Map<ApplicantId, CheckInRecord>,
): ArrivalCounts {
    val left = scope.count(::deskIsLeft)
    val arrived = scope.count { !deskIsLeft(it) && deskCheckedIn(it, records) }
    return ArrivalCounts(scope.size, arrived, scope.size - left - arrived, left)
}

fun deskRoomAvailability(
    fullRoll: List<ApplicantCard>,
    records: Map<ApplicantId, CheckInRecord>,
    inventory: List<AccoRoom>,
    block: RoomBlockKey,
): RoomAvailability {
    val codes = inventory.filter { it.gender == block.gender && it.section == block.section }
        .map { it.code }.toSet()
    val occupied = deskOccupied(fullRoll, records).intersect(codes).size
    return RoomAvailability(block, codes.size, occupied)
}

fun deskCallRound(roll: List<ApplicantCard>): List<ApplicantCard> =
    deskCallList(roll).filterNot(::deskIsLeft)

fun deskCallHeldOutCount(roll: List<ApplicantCard>): Int =
    deskCallList(roll).count(::deskIsLeft)

fun deskTypeCounts(roll: List<ApplicantCard>): Pair<Int, Int> {
    val students = roll.count { it.type == ApplicantType.Student }
    val sevaks = roll.count { it.type == ApplicantType.Sevak }
    return students to sevaks
}
