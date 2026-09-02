package org.dhamma.dipi.staff.model

/**
 * The Day 0 summary as the desk states it, parsed out of the `#day-summary`
 * fragment of `GET /zero-day/{cid}/{courseId}` (v5 frame `5d`).
 *
 * This is roll arithmetic, not student data: every field is a count. Nothing
 * here is NPI and nothing here is persisted — the fragment is fetched, parsed
 * and held for the life of the screen.
 *
 * The three tables are fixed on the desk side, so the shape is fixed here:
 * confirmed, attended, and the special-seating instruction. A missing table
 * yields zeros rather than an error, because on day −1 the real page is all
 * zeros too and the screen must not look broken.
 */
data class DaySummary(
    val confirmed: RollMatrix = RollMatrix(),
    val attended: RollMatrix = RollMatrix(),
    val specialSeating: SpecialSeating = SpecialSeating(),
) {
    /** The headline gap: everyone confirmed who has not walked in yet. */
    val stillToArrive: Int get() = (confirmed.total.total - attended.total.total).coerceAtLeast(0)

    /** True when the desk has nothing yet — day −1, and that is not an error. */
    val isEmpty: Boolean
        get() = confirmed.total.total == 0 && attended.total.total == 0 && specialSeating.isEmpty
}

/** One `#table-conf` / `#table-totals` block: male, female and their total. */
data class RollMatrix(
    val male: DayRollRow = DayRollRow(),
    val female: DayRollRow = DayRollRow(),
    val total: DayRollRow = DayRollRow(),
)

/**
 * One row of the desk's roll matrix. `server` is the desk's own separate
 * count and **sits outside the total** — it is shown in `neutral600` for
 * exactly that reason.
 */
data class DayRollRow(
    val old: Int = 0,
    val new: Int = 0,
    val total: Int = 0,
    val server: Int = 0,
)

/**
 * `#table-special` — an instruction to the hall team (put out this many low
 * seats, chairs and backrests), not a roll count. Each cell arrives as a
 * string like `1 (O) + 1 (N)`, so old and new are kept apart.
 */
data class SpecialSeating(
    val male: SpecialRow = SpecialRow(),
    val female: SpecialRow = SpecialRow(),
    val total: SpecialRow = SpecialRow(),
) {
    val isEmpty: Boolean get() = total.chowky.isEmpty && total.chair.isEmpty && total.backrest.isEmpty
}

data class SpecialRow(
    val chowky: OldNew = OldNew(),
    val chair: OldNew = OldNew(),
    val backrest: OldNew = OldNew(),
)

/** A `n (O) + m (N)` cell, kept as two figures rather than flattened. */
data class OldNew(val old: Int = 0, val new: Int = 0) {
    val sum: Int get() = old + new
    val isEmpty: Boolean get() = old == 0 && new == 0
}
