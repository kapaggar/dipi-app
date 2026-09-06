package org.dhamma.dipi.staff.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The per-student backrest marker (owner ruling 2026-09-05): one shared
 * glyph + label fn, so every surface (teacher list, student card, hall,
 * rail, 5i print) marks the same way — and a tofu fallback swap is a
 * one-line change in `Backrest.kt`.
 */
class BackrestTest {
    @Test
    fun marksOnlyWhenFlagged() {
        assertEquals("$BACKREST_GLYPH CW-A3", backrestSeatLabel("CW-A3", true))
        assertEquals("CW-A3", backrestSeatLabel("CW-A3", false))
        assertEquals("", backrestSeatLabel("", true)) // no seat, no marker
    }
}
