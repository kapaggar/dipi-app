package org.dhamma.dipi.staff.model

import kotlinx.serialization.Serializable

/**
 * How many room tiles sit in one chart row, per gender+section block. Rooms
 * wrap, so the row count is derived: ceil(rooms / columns). Device-local, and
 * wiped by Erase-all along with the rest of CentreOpsPrefs.
 */
@Serializable
data class RoomLayout(
    val columns: Map<String, Int> = emptyMap(),
) {
    fun columnsFor(gender: Gender, section: String, hallDefault: Int? = null): Int {
        val stored = columns[key(gender, section)]
        if (stored != null) return stored.coerceIn(MIN_COLUMNS, MAX_COLUMNS)
        val fromHall = hallDefault?.takeIf { it in MIN_COLUMNS..MAX_COLUMNS }
        return fromHall ?: DEFAULT_COLUMNS
    }

    fun withColumns(gender: Gender, section: String, n: Int): RoomLayout =
        copy(columns = columns + (key(gender, section) to n.coerceIn(MIN_COLUMNS, MAX_COLUMNS)))

    companion object {
        const val DEFAULT_COLUMNS = 4
        const val MIN_COLUMNS = 1
        const val MAX_COLUMNS = 12
        fun key(gender: Gender, section: String): String = "${gender.name}|$section"
        fun rowsFor(rooms: Int, columns: Int): Int =
            if (rooms <= 0 || columns <= 0) 0 else (rooms + columns - 1) / columns
    }
}
