package org.dhamma.dipi.staff.desk

import org.dhamma.dipi.staff.model.AccoRoom

fun nextOccupiedRoomCode(
    orderedCodes: List<String>,
    occupied: Set<String>,
    current: String?,
): String? {
    if (orderedCodes.isEmpty()) return null
    val start = orderedCodes.indexOf(current)
    for (offset in 1..orderedCodes.size) {
        val code = orderedCodes[(start + offset).mod(orderedCodes.size)]
        if (code in occupied) return code
    }
    return null
}

fun resolveRoomJump(rooms: List<AccoRoom>, query: String): AccoRoom? {
    val q = query.trim()
    if (q.isEmpty()) return null
    val byCode = rooms.filter { it.code.equals(q, ignoreCase = true) }
    if (byCode.size == 1) return byCode.single()
    if (byCode.size > 1) return null
    val byDisplay = rooms.filter { it.displayNo.equals(q, ignoreCase = true) }
    return byDisplay.singleOrNull()
}
