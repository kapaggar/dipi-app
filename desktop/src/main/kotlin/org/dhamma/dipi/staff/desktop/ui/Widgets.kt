package org.dhamma.dipi.staff.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.desktop.theme.LocalDipi
import org.dhamma.dipi.staff.desktop.theme.statusColors
import org.dhamma.dipi.staff.model.ApplicantStatus

val DeskShape = RoundedCornerShape(10.dp)

@Composable
fun DeskButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val c = LocalDipi.current
    val bg = when {
        !enabled -> c.field
        primary -> c.accent
        else -> c.field
    }
    val fg = when {
        !enabled -> c.muted
        primary -> androidx.compose.ui.graphics.Color.White
        else -> c.foreground
    }
    Box(
        modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(DeskShape)
            .background(bg)
            .border(1.dp, if (primary) c.accent else c.hairline, DeskShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DeskField(
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    password: Boolean = false,
    singleLine: Boolean = true,
    onSubmit: (() -> Unit)? = null,
) {
    val c = LocalDipi.current
    BasicTextField(
        value = value,
        onValueChange = onValue,
        singleLine = singleLine,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = TextStyle(color = c.foreground, fontSize = 16.sp, fontFamily = FontFamily.SansSerif),
        cursorBrush = SolidColor(c.accent),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(DeskShape)
            .background(c.field)
            .border(1.dp, c.hairlineStrong, DeskShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
fun StatusChip(status: String, dark: Boolean, modifier: Modifier = Modifier) {
    val (bg, fg) = statusColors(ApplicantStatus(status).tone, dark)
    Text(
        status,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
fun Segmented(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<String, Int> = emptyMap(),
) {
    val c = LocalDipi.current
    Row(
        modifier
            .clip(DeskShape)
            .border(1.dp, c.hairline, DeskShape)
            .height(IntrinsicSize.Min),
    ) {
        options.forEach { opt ->
            val on = opt == selected
            val label = counts[opt]?.let { "$opt $it" } ?: opt
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .background(if (on) c.tint else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) c.accent else c.muted,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun FilterRow(
    counts: Map<String, Int>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val c = LocalDipi.current
    val keys = (listOf("All") + counts.keys.filter { it != "All" }).distinct()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.take(8).forEach { key ->
            val on = if (key == "All") selected.isEmpty() else selected.any { it.equals(key, true) }
            val n = counts[key]
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) c.tint else c.field)
                    .border(1.dp, if (on) c.accent else c.hairline, RoundedCornerShape(8.dp))
                    .clickable { onToggle(key) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    if (n != null && key != "All") "$key $n" else key,
                    color = if (on) c.accent else c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.height(1.dp).fillMaxWidth().background(LocalDipi.current.hairline))
}

@Composable
fun VHairline(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).background(LocalDipi.current.hairline))
}
