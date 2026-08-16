package org.dhamma.dipi.staff.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.LocalDipi

const val DESK_ACTION_PLACEHOLDER =
    "This control is wired to the desk path; implementation is the next slice."

@Composable
fun DeskActionScreen(
    title: String,
    route: String,
    onBack: () -> Unit = {},
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(20.dp),
    ) {
        Text(title, fontFamily = DipiCondensed, fontSize = 22.sp, color = c.foreground)
        Text(
            route,
            fontFamily = DipiMono,
            fontSize = 13.sp,
            color = c.accent,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            DESK_ACTION_PLACEHOLDER,
            color = c.muted,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
            Text("Back")
        }
    }
}

@Composable
fun ComingScreen(
    title: String,
    route: String,
    onBack: () -> Unit = {},
) = DeskActionScreen(title, route, onBack)
