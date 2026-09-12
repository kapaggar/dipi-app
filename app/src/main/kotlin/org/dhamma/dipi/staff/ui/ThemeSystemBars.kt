package org.dhamma.dipi.staff.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

/** Edge-to-edge's original light navigation scrim, retained for Light mode. */
fun systemNavigationBarColor(dark: Boolean): Int =
    (if (dark) Color(0xFF14171A) else Color(0xFFE8E8E9)).toArgb()

/** Keeps edge-to-edge system-bar icons readable from the app's saved appearance choice. */
@Composable
fun ThemeSystemBars(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity() ?: return
    SideEffect {
        activity.window.navigationBarColor = systemNavigationBarColor(dark)
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
