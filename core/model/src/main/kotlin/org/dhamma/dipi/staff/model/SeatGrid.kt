package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable

/**
 * Hall-grid config (spec 2c S1). Registrar-configured device-locally per
 * gender — it mirrors the server's per-centre INI, which no readable endpoint
 * exposes. Clamped 1..26 rows / 1..20 seats per row on read AND write
 * (the RoomLayout pattern). The config is a starting shape only: seat labels
 * beyond it EXTEND the drawn plan — data wins over config, a seated student
 * is never dropped.
 */
@Serializable
data class HallGrid(
    val rows: Int = DEFAULT_ROWS,
    val seatsPerRow: Int = DEFAULT_SEATS_PER_ROW,
) {
    fun clamped(): HallGrid = HallGrid(
        rows = rows.coerceIn(MIN_ROWS, MAX_ROWS),
        seatsPerRow = seatsPerRow.coerceIn(MIN_SEATS_PER_ROW, MAX_SEATS_PER_ROW),
    )

    companion object {
        const val DEFAULT_ROWS = 5
        const val DEFAULT_SEATS_PER_ROW = 7
        const val MIN_ROWS = 1
        const val MAX_ROWS = 26
        const val MIN_SEATS_PER_ROW = 1
        const val MAX_SEATS_PER_ROW = 20
    }
}

/** One placed hall position: the label verbatim plus its 0-based grid slot. */
data class SeatCell(val id: String, val row: Int, val col: Int)

/** One occupied seat — hall grid or cell/pagoda column. Old/new is the row's group seniority. */
data class PlacedSeat(val row: RollRow, val old: Boolean)

/** A hall-grid slot: the seat id (verbatim when occupied, synthesized when empty). */
data class HallCell(val id: String, val seated: PlacedSeat?)

/** A row with no placeable seat. [reason] is the roleTag or [UNSEATED_NO_REASON]. */
data class UnseatedRow(val row: RollRow, val reason: String)

/** The only "reason" the page carries besides the name-suffix role tag. */
const val UNSEATED_NO_REASON = "—"

/**
 * The drawn seating plan for one hall (spec 2c S2), derived purely from the
 * roll's seat labels — never from a server layout. [cells] is `rows × cols`;
 * [cellColumn] is the CW-/CH- seats (occupied only) in label order;
 * [unseated] keeps roll order. [oldCount]/[newCount] tally every row fed in,
 * matching the header sub-line.
 */
data class HallPlan(
    val rows: Int,
    val cols: Int,
    val rowLetters: List<String>,
    val cells: List<List<HallCell>>,
    val cellColumn: List<PlacedSeat>,
    val unseated: List<UnseatedRow>,
    val oldCount: Int,
    val newCount: Int,
)

private val ALPHANUMERIC_SEAT = Regex("""([A-Za-z]{1,2})[ -]?(\d+)""")

/**
 * Extension ceiling: twice the configurable maximum in each direction. A
 * label past this is garbage, not a hall — the student keeps a row in
 * UNSEATED instead of stretching the grid into the thousands.
 */
const val MAX_PLAN_ROWS = HallGrid.MAX_ROWS * 2
const val MAX_PLAN_COLS = HallGrid.MAX_SEATS_PER_ROW * 2

/** `"A"`→0 … `"Z"`→25, `"AA"`→26 (spreadsheet letters). */
private fun rowIndex(letters: String): Int =
    letters.uppercase().fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1

/** Inverse of [rowIndex]: 0→`"A"` … 25→`"Z"`, 26→`"AA"`. */
fun rowLetter(index: Int): String {
    var n = index + 1
    val sb = StringBuilder()
    while (n > 0) {
        n--
        sb.append('A' + n % 26)
        n /= 26
    }
    return sb.reverse().toString()
}

/**
 * Where one seat label sits in the hall grid, or null when the label does not
 * place there: blank, `CW-`/`CH-` prefixed (those go to the cell/pagoda
 * column), or unparseable. Alphanumeric labels are letter(s)→row,
 * number→column-1 — `"E4"`→(4,3), `"AA3"`→(26,2). Numeric-only labels flow by
 * [seatsPerRow]: `"12"` at 7/row →(1,4).
 */
fun seatPlacement(label: String, seatsPerRow: Int = HallGrid.DEFAULT_SEATS_PER_ROW): SeatCell? {
    val id = label.trim()
    if (id.isEmpty()) return null
    if (id.startsWith("CW-", ignoreCase = true) || id.startsWith("CH-", ignoreCase = true)) return null
    ALPHANUMERIC_SEAT.matchEntire(id)?.let { m ->
        val col = m.groupValues[2].toIntOrNull()?.minus(1) ?: return null
        val row = rowIndex(m.groupValues[1])
        if (col < 0 || col >= MAX_PLAN_COLS || row >= MAX_PLAN_ROWS) return null
        return SeatCell(id, row, col)
    }
    val n = id.toIntOrNull() ?: return null
    if (n < 1) return null
    val spr = seatsPerRow.coerceAtLeast(1)
    val row = (n - 1) / spr
    if (row >= MAX_PLAN_ROWS) return null
    return SeatCell(id, row, (n - 1) % spr)
}

/**
 * The pure layout pass for one hall. Callers pass the roll groups of ONE
 * gender, in parse order; old/new comes from each group's band seniority.
 *
 * - Blank seat → [HallPlan.unseated] with the row's roleTag (or an em-dash) —
 *   roll order kept, never re-sorted.
 * - `CW-`/`CH-` seats → [HallPlan.cellColumn] in label order (chairs join the
 *   cell/pagoda column with their label shown — recorded ruling; the design
 *   never knew the `CH-` prefix).
 * - Every other label places by [seatPlacement]; labels past the configured
 *   grid EXTEND it. A label that cannot place at all (unparseable, or a
 *   duplicate of an already-taken slot) falls to unseated — data wins over
 *   config and a seated student is never dropped.
 * - Empty slots synthesize their id: `{letter}{col+1}`, or the flowing number
 *   when every placed hall seat is numeric-only.
 */
fun hallLayout(groups: List<RollGroup>, grid: HallGrid): HallPlan {
    val g = grid.clamped()
    val placed = LinkedHashMap<Pair<Int, Int>, PlacedSeat>()
    val cellSeats = mutableListOf<PlacedSeat>()
    val unseated = mutableListOf<UnseatedRow>()
    var oldCount = 0
    var newCount = 0
    var anyHallSeat = false
    var allNumeric = true

    fun unseat(row: RollRow) {
        unseated += UnseatedRow(row, row.roleTag ?: UNSEATED_NO_REASON)
    }

    groups.forEach { group ->
        val old = group.seniority == RollSeniority.OLD
        group.rows.forEach { row ->
            if (old) oldCount++ else newCount++
            val seat = row.seat.trim()
            when {
                seat.isEmpty() -> unseat(row)
                seat.startsWith("CW-", ignoreCase = true) ||
                    seat.startsWith("CH-", ignoreCase = true) -> cellSeats += PlacedSeat(row, old)
                else -> {
                    val cell = seatPlacement(seat, g.seatsPerRow)
                    if (cell == null) {
                        unseat(row)
                    } else {
                        anyHallSeat = true
                        if (seat.any { it.isLetter() }) allNumeric = false
                        val key = cell.row to cell.col
                        if (placed.containsKey(key)) unseat(row) else placed[key] = PlacedSeat(row, old)
                    }
                }
            }
        }
    }

    val rows = maxOf(g.rows, (placed.keys.maxOfOrNull { it.first } ?: -1) + 1)
    val cols = maxOf(g.seatsPerRow, (placed.keys.maxOfOrNull { it.second } ?: -1) + 1)
    val numericFlow = anyHallSeat && allNumeric
    val letters = (0 until rows).map { rowLetter(it) }
    val cells = (0 until rows).map { r ->
        (0 until cols).map { c ->
            val seated = placed[r to c]
            val id = seated?.row?.seat?.trim()
                ?: if (numericFlow) (r * g.seatsPerRow + c + 1).toString() else "${letters[r]}${c + 1}"
            HallCell(id, seated)
        }
    }
    val cellColumn = cellSeats.sortedWith { a, b -> compareSeatLabels(a.row.seat.trim(), b.row.seat.trim()) }
    return HallPlan(rows, cols, letters, cells, cellColumn, unseated, oldCount, newCount)
}

/**
 * Label order for the cell/pagoda column: case-insensitive, with digit runs
 * compared numerically so `CH-2` sorts before `CH-12`.
 */
internal fun compareSeatLabels(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        val ca = a[i]
        val cb = b[j]
        if (ca.isDigit() && cb.isDigit()) {
            var ie = i
            while (ie < a.length && a[ie].isDigit()) ie++
            var je = j
            while (je < b.length && b[je].isDigit()) je++
            val na = a.substring(i, ie).trimStart('0')
            val nb = b.substring(j, je).trimStart('0')
            val cmp = if (na.length != nb.length) na.length - nb.length else na.compareTo(nb)
            if (cmp != 0) return cmp
            i = ie
            j = je
        } else {
            val cmp = ca.uppercaseChar().compareTo(cb.uppercaseChar())
            if (cmp != 0) return cmp
            i++
            j++
        }
    }
    return (a.length - i) - (b.length - j)
}
