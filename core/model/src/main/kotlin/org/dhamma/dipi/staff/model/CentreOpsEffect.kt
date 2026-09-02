package org.dhamma.dipi.staff.model

/**
 * The centre-settings RESULT line: what check-in will ask for, given the three
 * switches. Room and seating are unconditional (CheckInPane renders ROOM and
 * SEATING always); the switches only append. See
 * the 08-26 v3-conformance spec S3.4 (docs/DECISIONS.md, Design system).
 */
fun centreOpsEffect(prefs: CentreOpsPrefs): String {
    val asks = buildList {
        add("room")
        add("seating")
        if (prefs.laundry) add("laundry")
        if (prefs.valuables) add("valuables")
        if (prefs.groups) add("group")
    }
    val list = asks.dropLast(1).joinToString(", ") + " and " + asks.last()
    val head = "Check-in asks for $list."
    return if (prefs.groups) {
        head
    } else {
        "$head Everyone sits in $MAIN_DHAMMA_HALL and Zero Day hides group chips."
    }
}
