package org.dhamma.dipi.staff.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/** IBM Plex Mono 600 / 9.5sp / .16em kicker — the system's all-caps label. */
@Composable
fun DeskKicker(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        fontFamily = DipiMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.5.sp,
        letterSpacing = 0.16.em,
        color = color,
        modifier = modifier,
    )
}
