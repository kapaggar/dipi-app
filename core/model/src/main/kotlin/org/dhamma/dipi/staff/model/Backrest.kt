package org.dhamma.dipi.staff.model

/**
 * Per-student backrest marker (owner ruling 2026-09-05). U+2310 — a
 * chair-back profile that survives the monochrome 5i print. Prefix
 * position so the right-aligned seat column keeps its edge.
 */
const val BACKREST_GLYPH = "⌐"

/** `⌐ CW-A3` when [backrest], else the seat unchanged. */
fun backrestSeatLabel(seat: String, backrest: Boolean): String =
    if (backrest && seat.isNotBlank()) "$BACKREST_GLYPH $seat" else seat
