package org.dhamma.dipi.staff.ui.theme

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Historical name, new rendering (sleek pass, owner decision 2026-08-16):
 * the blueprint frame no longer draws the `+` corner registration marks —
 * it is a soft rounded hairline in the given colour. Surfaces that want the
 * full card treatment (fill + elevation) use [deskCard] instead.
 */
fun Modifier.blueprint(border: Color): Modifier =
    this.border(1.dp, border, DeskStyle.cardShape)
