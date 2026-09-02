package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable

/**
 * Hall-grid config (spec 2c S1, axes flipped by the r2 orientation spec —
 * the live web seating page is the authority). Registrar-configured
 * device-locally per gender — it mirrors the server's per-centre INI, which
 * no readable endpoint exposes. **Letters are COLUMNS** (A, B, C… left to
 * right) and **numbers are DEPTH rows** — seat 1 sits nearest the teacher.
 * Clamped 1..26 columns / 1..40 depth on read AND write (the RoomLayout
 * pattern). The config is a starting shape only: seat labels beyond it
 * EXTEND the drawn plan — data wins over config, a seated student is never
 * dropped.
 *
 * The field names are new (the 2c originals were `rows`/`seatsPerRow`): a
 * persisted day-one blob decodes to the defaults via `ignoreUnknownKeys`
 * and the registrar re-saves — recorded, acceptable.
 */
@Serializable
data class HallGrid(
    val columns: Int = DEFAULT_COLUMNS,
    val depth: Int = DEFAULT_DEPTH,
) {
    fun clamped(): HallGrid = HallGrid(
        columns = columns.coerceIn(MIN_COLUMNS, MAX_COLUMNS),
        depth = depth.coerceIn(MIN_DEPTH, MAX_DEPTH),
    )

    companion object {
        const val DEFAULT_COLUMNS = 7
        const val DEFAULT_DEPTH = 5
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 26
        const val MIN_DEPTH = 1
        const val MAX_DEPTH = 40
    }
}

/**
 * One placed hall position: the label verbatim plus its slot — [column] is
 * the 0-based letter index (A=0, AA=26), [depth] the 1-based row number,
 * 1 nearest the teacher.
 */
data class SeatCell(val id: String, val column: Int, val depth: Int)

/** One occupied seat — hall grid or the chowky/chair rail. Old/new is the row's group seniority. */
data class PlacedSeat(val row: RollRow, val old: Boolean)

/** A hall-grid slot: the seat id (verbatim when occupied, synthesized when empty). */
data class HallCell(val id: String, val seated: PlacedSeat?)

/** A row with no placeable seat. [reason] is the roleTag or [UNSEATED_NO_REASON]. */
data class UnseatedRow(val row: RollRow, val reason: String)

/** The only "reason" the page carries besides the name-suffix role tag. */
const val UNSEATED_NO_REASON = "—"

/**
 * The drawn seating plan for one hall, derived purely from the roll's seat
 * labels — never from a server layout. [cells] is indexed
 * `[depth-1][column]` — index 0 is depth row 1, NEAREST the teacher; render
 * depth descending so row 1 lands at the bottom. [chowkyChair] is the CW-
 * (chowky, a low seat) and CH- (chair) positions — hall furniture, NOT
 * pagoda cells (the pagoda is a separate building, unmodelled) — occupied
 * only, ordered by trailing number DESCENDING so `…-1` ends nearest the
 * teacher. [unseated] keeps roll order. [oldCount]/[newCount] tally every
 * row fed in, matching the header sub-line.
 */
data class HallPlan(
    val columns: Int,
    val depth: Int,
    val columnLetters: List<String>,
    val cells: List<List<HallCell>>,
    val chowkyChair: List<PlacedSeat>,
    val unseated: List<UnseatedRow>,
    val oldCount: Int,
    val newCount: Int,
) {
    /**
     * The UNSEATED rows the plan actually lists (r2 S4): sevaks sit on
     * cushions the plan does not draw, so `roleTag == "Sevak"` (any case)
     * hides from the section — every other unseated row keeps its reason.
     * The old/new tally is untouched: sevaks are in the hall and stay
     * counted.
     */
    val unseatedVisible: List<UnseatedRow>
        get() = unseated.filterNot { it.row.roleTag?.equals("Sevak", ignoreCase = true) == true }
}

private val ALPHANUMERIC_SEAT = Regex("""([A-Za-z]{1,2})[ -]?(\d+)""")

/**
 * Extension ceiling: twice the configurable maximum on each axis. A label
 * past this is garbage, not a hall — the student keeps a row in UNSEATED
 * instead of stretching the grid into the thousands.
 */
const val MAX_PLAN_COLUMNS = HallGrid.MAX_COLUMNS * 2
const val MAX_PLAN_DEPTH = HallGrid.MAX_DEPTH * 2

/** `"A"`→0 … `"Z"`→25, `"AA"`→26 (spreadsheet letters). */
private fun columnIndex(letters: String): Int =
    letters.uppercase().fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1

/** Inverse of [columnIndex]: 0→`"A"` … 25→`"Z"`, 26→`"AA"`. */
fun columnLetter(index: Int): String {
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
 * Where one seat label sits in the hall grid, or null when the label does
 * not place there: blank, `CW-`/`CH-` prefixed (those go to the chowky/chair
 * rail), or unparseable. Alphanumeric labels are letter(s)→column,
 * number→depth — `"E4"`→column 4, depth 4; `"AA3"`→column 26, depth 3.
 * Numeric-only labels flow ACROSS the columns: `"12"` at 7 columns →
 * column 4, depth 2.
 */
fun seatPlacement(label: String, columns: Int = HallGrid.DEFAULT_COLUMNS): SeatCell? {
    val id = label.trim()
    if (id.isEmpty()) return null
    if (id.startsWith("CW-", ignoreCase = true) || id.startsWith("CH-", ignoreCase = true)) return null
    ALPHANUMERIC_SEAT.matchEntire(id)?.let { m ->
        val depth = m.groupValues[2].toIntOrNull() ?: return null
        val column = columnIndex(m.groupValues[1])
        if (depth < 1 || depth > MAX_PLAN_DEPTH || column >= MAX_PLAN_COLUMNS) return null
        return SeatCell(id, column, depth)
    }
    val n = id.toIntOrNull() ?: return null
    if (n < 1) return null
    val cols = columns.coerceAtLeast(1)
    val depth = (n - 1) / cols + 1
    if (depth > MAX_PLAN_DEPTH) return null
    return SeatCell(id, (n - 1) % cols, depth)
}

/**
 * The pure layout pass for one hall. Callers pass the roll groups of ONE
 * gender, in parse order; old/new comes from each group's band seniority.
 *
 * - Blank seat → [HallPlan.unseated] with the row's roleTag (or an em-dash) —
 *   roll order kept, never re-sorted.
 * - `CW-`/`CH-` seats → [HallPlan.chowkyChair], trailing number descending
 *   (chairs join the chowky rail with their label shown — recorded ruling;
 *   the design never knew the `CH-` prefix).
 * - Every other label places by [seatPlacement]; labels past the configured
 *   grid EXTEND it on either axis. A label that cannot place at all
 *   (unparseable, or a duplicate of an already-taken slot) falls to
 *   unseated — data wins over config and a seated student is never dropped.
 * - Empty slots synthesize their id: `{letter}{depth}`, or the flowing
 *   number when every placed hall seat is numeric-only.
 */
fun hallLayout(groups: List<RollGroup>, grid: HallGrid): HallPlan {
    val g = grid.clamped()
    val placed = LinkedHashMap<Pair<Int, Int>, PlacedSeat>()
    val railSeats = mutableListOf<PlacedSeat>()
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
                    seat.startsWith("CH-", ignoreCase = true) -> railSeats += PlacedSeat(row, old)
                else -> {
                    val cell = seatPlacement(seat, g.columns)
                    if (cell == null) {
                        unseat(row)
                    } else {
                        anyHallSeat = true
                        if (seat.any { it.isLetter() }) allNumeric = false
                        val key = (cell.depth - 1) to cell.column
                        if (placed.containsKey(key)) unseat(row) else placed[key] = PlacedSeat(row, old)
                    }
                }
            }
        }
    }

    val depth = maxOf(g.depth, (placed.keys.maxOfOrNull { it.first } ?: -1) + 1)
    val columns = maxOf(g.columns, (placed.keys.maxOfOrNull { it.second } ?: -1) + 1)
    val numericFlow = anyHallSeat && allNumeric
    val letters = (0 until columns).map { columnLetter(it) }
    val cells = (0 until depth).map { d ->
        (0 until columns).map { c ->
            val seated = placed[d to c]
            val id = seated?.row?.seat?.trim()
                ?: if (numericFlow) (d * g.columns + c + 1).toString() else "${letters[c]}${d + 1}"
            HallCell(id, seated)
        }
    }
    val chowkyChair = railSeats.sortedWith { a, b -> compareRailLabels(a.row.seat.trim(), b.row.seat.trim()) }
    return HallPlan(columns, depth, letters, cells, chowkyChair, unseated, oldCount, newCount)
}

/**
 * Chowky/chair rail order (r2 S2): prefix ascending (case-insensitive, digit
 * runs numeric so `CH-2` groups before `CH-12`'s prefix ties), then the
 * TRAILING number DESCENDING — `…-6` at the top, `…-1` at the bottom,
 * nearest the teacher, matching the grid's bottom-up read.
 */
internal fun compareRailLabels(a: String, b: String): Int {
    val (prefixA, numA) = splitTrailingNumber(a)
    val (prefixB, numB) = splitTrailingNumber(b)
    val cmp = compareSeatLabels(prefixA, prefixB)
    if (cmp != 0) return cmp
    return numB.compareTo(numA)
}

/** `"CW-A12"` → `"CW-A"` to 12; a label with no trailing digits keeps number 0. */
private fun splitTrailingNumber(label: String): Pair<String, Int> {
    var i = label.length
    while (i > 0 && label[i - 1].isDigit()) i--
    val n = if (i == label.length) 0 else label.substring(i).toIntOrNull() ?: 0
    return label.substring(0, i) to n
}

/**
 * Label order for rail prefixes: case-insensitive, with digit runs compared
 * numerically so `CH-2` sorts before `CH-12`.
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
