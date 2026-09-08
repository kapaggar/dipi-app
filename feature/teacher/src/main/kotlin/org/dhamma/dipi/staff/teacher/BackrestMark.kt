package org.dhamma.dipi.staff.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Dark top-edge bar replacing the faint `⌐` glyph (owner 2026-09-08). */
internal val BackrestBarColor = Color(0xFF2B2B2D)

@Composable
internal fun BackrestTopBar(visible: Boolean, tag: String) {
    if (!visible) return
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(BackrestBarColor)
            .testTag(tag),
    )
}

@Composable
internal fun BackrestSeatMark(visible: Boolean, tag: String) {
    if (!visible) return
    Box(
        Modifier
            .width(28.dp)
            .height(3.dp)
            .background(BackrestBarColor)
            .testTag(tag),
    )
}
