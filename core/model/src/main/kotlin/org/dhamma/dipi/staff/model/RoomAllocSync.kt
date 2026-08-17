package org.dhamma.dipi.staff.model

/**
 * Sync bookkeeping: any material edit clears the synced flag so the record
 * queues again; an unchanged record keeps whatever flag it had.
 */
fun CheckInRecord.clearSyncedIfChanged(prev: CheckInRecord?): CheckInRecord =
    if (this == prev) this else copy(synced = false, syncedAt = null)

/** One applicant's outcome from the desk's allocation update endpoint. */
sealed class RoomPostOutcome {
    object Ok : RoomPostOutcome()

    /** The server answered `status:false` — its `msg` verbatim (e.g. "Room has already been alloted"). */
    data class Rejected(val reason: String) : RoomPostOutcome()

    /** 401/403 — the bulk walk stops and the caller surfaces sign-in. */
    object AuthExpired : RoomPostOutcome()

    /** Connectivity dropped mid-walk — the rest stays queued for the next run. */
    object Offline : RoomPostOutcome()
}

data class RoomSyncFailure(val id: ApplicantId, val reason: String)

data class RoomSyncResult(
    val attempted: Int = 0,
    val synced: Int = 0,
    val failures: List<RoomSyncFailure> = emptyList(),
    val authExpired: Boolean = false,
    val offline: Boolean = false,
) {
    val failed: Int get() = failures.size
}

/**
 * Owner amendment (2026-08-16): bulk, user-initiated replication of the desk
 * worklist dialog's own allocation write — `POST /app-update-attended/{id}`
 * (`dh_app_update_attended`, plain Drupal menu callback, session cookie only,
 * no form token). [params] mirrors the dialog's jQuery post exactly:
 * `s,r,g,l,v,c,cf,chow,chai,back,comment,a`, booleans as the literal strings
 * "true"/"false" the PHP compares against.
 *
 * What the app does NOT track it posts EMPTY — the desk's laundry/valuable
 * token numbers (`l`,`v`; the app's switches are booleans, a fabricated token
 * would trip the server's duplicate-token check), cell (`c`,`cf`) and
 * comment. The endpoint overwrites every `aa_*` field unconditionally, so a
 * sync clears any desk-entered values for those fields — accepted, since the
 * tablet is the allocation source. Never a status parameter, never NPI:
 * on first insert the SERVER itself confirms the applicant; the client sends
 * nothing status-shaped and never "Approved".
 */
object RoomAllocSync {

    /** The queue: checked-in, not yet synced, and holding a room to send. */
    fun pending(records: Map<ApplicantId, CheckInRecord>): Map<ApplicantId, CheckInRecord> =
        records.filterValues { it.checkedIn && !it.synced && it.room.isNotBlank() }

    /** "Mbk 12" → section "Mbk" + room no "12" (the pair the dialog sends). */
    fun splitRoom(code: String): Pair<String, String> {
        val trimmed = code.trim()
        val cut = trimmed.lastIndexOf(' ')
        return if (cut < 0) "" to trimmed else trimmed.substring(0, cut).trim() to trimmed.substring(cut + 1)
    }

    /** App room code: section + space + number (`Fbk 36`). Empty either side → `""`. */
    fun joinRoom(section: String, number: String): String {
        val s = section.trim()
        val n = number.trim()
        if (s.isEmpty() || n.isEmpty()) return ""
        return "$s $n"
    }

    /**
     * Desk RoomNo cell (`Fbk-36`) → app code (`Fbk 36`). Splits on the last
     * dash so `A-Block-7` becomes `A-Block 7`. Already-spaced values stay;
     * `-` / blank / empty section+acco → `""`.
     */
    fun parseDeskRoom(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-") return ""
        if (' ' in trimmed) return trimmed
        val cut = trimmed.lastIndexOf('-')
        if (cut < 0) return ""
        return joinRoom(trimmed.substring(0, cut), trimmed.substring(cut + 1))
    }

    /**
     * Pull merge: tablet unsynced rooms win; everyone else on the attended
     * table adopts the server room/seat/group/flags as already-synced.
     * Local-only records stay; missing-from-server is never unchecked.
     */
    fun mergePulled(
        local: Map<ApplicantId, CheckInRecord>,
        pulled: Map<ApplicantId, CheckInRecord>,
    ): Map<ApplicantId, CheckInRecord> {
        val out = local.toMutableMap()
        for ((id, remote) in pulled) {
            val cur = out[id]
            if (cur != null && !cur.synced && cur.room.isNotBlank()) continue
            out[id] = if (cur == null) {
                remote.copy(checkedIn = true, synced = true)
            } else {
                cur.copy(
                    checkedIn = true,
                    room = remote.room,
                    seat = remote.seat,
                    group = remote.group,
                    laundry = remote.laundry,
                    valuables = remote.valuables,
                    synced = true,
                )
            }
        }
        return out
    }

    fun params(record: CheckInRecord): Map<String, String> {
        val (section, number) = splitRoom(record.room)
        return linkedMapOf(
            "s" to section,
            "r" to number,
            "g" to record.group,
            "l" to "",
            "v" to "",
            "c" to "",
            "cf" to "false",
            "chow" to (record.seat == "Chowky").toString(),
            "chai" to (record.seat == "Chair").toString(),
            "back" to (record.seat == "Backrest").toString(),
            "comment" to "",
            "a" to "true",
        )
    }

    /**
     * The bulk walk: post every pending record, mark each success as it
     * lands (so a partial run keeps its progress), collect per-row refusals,
     * and stop dead on auth loss or connectivity loss.
     */
    suspend fun walk(
        pending: Map<ApplicantId, CheckInRecord>,
        post: suspend (ApplicantId, CheckInRecord) -> RoomPostOutcome,
        markSynced: suspend (ApplicantId, CheckInRecord) -> Unit,
    ): RoomSyncResult {
        var attempted = 0
        var synced = 0
        val failures = mutableListOf<RoomSyncFailure>()
        var authExpired = false
        var offline = false
        for ((id, record) in pending) {
            attempted += 1
            when (val outcome = post(id, record)) {
                is RoomPostOutcome.Ok -> {
                    markSynced(id, record)
                    synced += 1
                }
                is RoomPostOutcome.Rejected -> failures += RoomSyncFailure(id, outcome.reason)
                RoomPostOutcome.AuthExpired -> {
                    authExpired = true
                    break
                }
                RoomPostOutcome.Offline -> {
                    offline = true
                    break
                }
            }
        }
        return RoomSyncResult(attempted, synced, failures, authExpired, offline)
    }
}
