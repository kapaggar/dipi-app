package org.dhamma.dipi.staff.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.model.Session
import org.dhamma.dipi.staff.ui.theme.DeskKicker
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DeskStyle
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.IndustryPalette
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LocalIndustry
import org.dhamma.dipi.staff.ui.theme.chipGradientColors
import org.dhamma.dipi.staff.ui.theme.deskCard

/**
 * Settings, design frames **1d** (light) and **1e** (dark) — `docs/DESIGN.md`.
 *
 * Two columns at tablet width — appearance on the left, account and session on
 * the right (428dp) — stacking into the one scrolling page below 600dp. Same
 * items the screen always had: nothing was added to the page's job. What
 * changed is that the two text links which read like labels ("Theme: Light",
 * "Simulate offline: off") became a two-way segmented control and a real
 * switch, and dark mode now says out loud that it is the Steel night ramp
 * rather than silently ignoring the saved skin.
 *
 * Callbacks are unchanged `() -> Unit`s: the segmented control calls
 * [onToggleTheme] only when the tapped segment is not the live one, and the
 * switch rows use the desk's established single-fire pattern (row carries the
 * toggle semantics, `Switch` is display-only).
 */
@Composable
fun SettingsScreen(
    session: Session?,
    dark: Boolean,
    lastSync: String?,
    queued: Int,
    offline: Boolean,
    onToggleTheme: () -> Unit,
    onToggleOffline: () -> Unit = {},
    onLogout: () -> Unit,
    onFactoryReset: () -> Unit = {},
    appVersion: String = "",
    skin: DeskSkin = DeskSkin.Steel,
    lotus: Boolean = true,
    onSkin: (DeskSkin) -> Unit = {},
    onToggleLotus: () -> Unit = {},
) {
    val c = LocalDipi.current
    var confirmReset by remember { mutableStateOf(false) }
    // Two columns need room for both: the right column is a hard 428dp, and the
    // widest thing in the left one is a 258dp ramp strip beside its 36dp of card
    // padding. Below ~788dp the left column cannot hold that, so the whole page
    // stacks — the 600dp breakpoint the rest of the app uses would overflow the
    // 600–787dp band (7"–9" tablets, split-screen) from the theme control down.
    val wide = LocalConfiguration.current.screenWidthDp >= 800
    Column(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Box(Modifier.height(56.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                "Settings",
                fontFamily = DipiCondensed,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 0.012.em,
                color = c.foreground,
            )
        }
        if (session?.modeTest == true) {
            Text(
                "TEST MODE — sandbox. Status changes hit the mock (or a sandbox host). The strip stays on every screen.",
                color = c.muted,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppearanceCard(dark, skin, lotus, onToggleTheme, onSkin, onToggleLotus)
                    TestingCard(offline, onToggleOffline)
                }
                AccountCard(
                    session = session,
                    dark = dark,
                    lastSync = lastSync,
                    queued = queued,
                    appVersion = appVersion,
                    onLogout = onLogout,
                    onErase = { confirmReset = true },
                    modifier = Modifier.width(428.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppearanceCard(dark, skin, lotus, onToggleTheme, onSkin, onToggleLotus)
                TestingCard(offline, onToggleOffline)
                AccountCard(
                    session = session,
                    dark = dark,
                    lastSync = lastSync,
                    queued = queued,
                    appVersion = appVersion,
                    onLogout = onLogout,
                    onErase = { confirmReset = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Erase everything on this tablet?") },
            text = {
                Text("This is a factory reset of the app. You will need to sign in again.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        onFactoryReset()
                    },
                ) { Text("Erase") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

// ---------------------------------------------------------------- APPEARANCE

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceCard(
    dark: Boolean,
    skin: DeskSkin,
    lotus: Boolean,
    onToggleTheme: () -> Unit,
    onSkin: (DeskSkin) -> Unit,
    onToggleLotus: () -> Unit,
) {
    val c = LocalDipi.current
    val industry = LocalIndustry.current
    SettingsCard(Modifier.testTag("card-appearance")) {
        DeskKicker("APPEARANCE", c.muted)
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Theme", fontSize = 15.5.sp, color = c.foreground, modifier = Modifier.weight(1f))
            ThemeSegments(dark = dark, onChange = onToggleTheme)
        }
        if (dark) {
            // Frame 1e: the night ramp is a decision, so the screen states it
            // instead of leaving a lit Blossom chip looking broken.
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(c.tint, DeskStyle.controlShape)
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Dark runs the Steel night ramp.",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = NightAccentText,
                )
                Text(
                    "${skin.label} is remembered and comes back the moment you switch to Light. " +
                        "One night ramp keeps contrast and the fixed status hexes honest.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = c.muted,
                )
            }
        }
        SettingsRule(dark, Modifier.padding(top = if (dark) 16.dp else 4.dp, bottom = 14.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DeskKicker("SKIN", c.muted)
            if (dark) {
                Text(
                    "APPLIES IN LIGHT",
                    fontFamily = DipiMono,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.126.em,
                    color = c.muted.copy(alpha = 0.7f),
                )
            }
        }
        FlowRow(
            Modifier.padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            DeskSkin.entries.forEach { s ->
                SkinButton(s, selected = s == skin, dark = dark, onClick = { onSkin(s) })
            }
        }
        Text(
            "Accent ramp, paper, neutrals and the lotus wash all move together.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.muted,
            modifier = Modifier.padding(top = 11.dp),
        )
        FlowRow(
            Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (dark) {
                RampStrip("NIGHT ACCENT", NightAccentRamp, dark, "ramp-accent")
                RampStrip("NIGHT NEUTRALS", nightNeutralRamp(), dark, "ramp-neutral")
            } else {
                RampStrip(
                    "ACCENT 100–900",
                    listOf(
                        industry.accent100, industry.accent200, industry.accent300,
                        industry.accent400, industry.accent500, industry.accent600,
                        industry.accent700, industry.accent800, industry.accent900,
                    ),
                    dark,
                    "ramp-accent",
                )
                RampStrip(
                    "NEUTRAL 100–900",
                    listOf(
                        industry.neutral100, industry.neutral200, industry.neutral300,
                        industry.neutral400, industry.neutral500, industry.neutral600,
                        industry.neutral700, industry.neutral800, industry.neutral900,
                    ),
                    dark,
                    "ramp-neutral",
                )
            }
        }
        SettingsRule(dark, Modifier.padding(top = 16.dp, bottom = 4.dp))
        SwitchRow(
            label = "Lotus watermark",
            on = lotus,
            onToggle = onToggleLotus,
            testTag = "toggle-lotus",
        )
        Text(
            "Status colours stay put; they carry meaning, not mood.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.muted,
        )
    }
}

/**
 * The two-way `Light | Dark` control. [onChange] is the existing
 * `onToggleTheme`, so it fires only when the tapped segment is not the live
 * one — tapping the selected segment is a no-op, not a toggle back.
 */
@Composable
private fun ThemeSegments(dark: Boolean, onChange: () -> Unit) {
    val c = LocalDipi.current
    val shape = DeskStyle.controlShape
    Row(
        Modifier
            .height(38.dp)
            .border(1.dp, c.hairlineStrong, shape)
            .clip(shape),
    ) {
        Segment("Light", selected = !dark, onSelect = { if (dark) onChange() })
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(c.hairlineStrong),
        )
        Segment("Dark", selected = dark, onSelect = { if (!dark) onChange() })
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onSelect: () -> Unit) {
    val c = LocalDipi.current
    Box(
        Modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(if (selected) c.accent else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onSelect),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) Color.White else c.foreground,
        )
    }
}

/**
 * Nine 26×18dp swatches — what a skin actually changes, which no sentence can
 * say. The strip is a fixed 258dp `Row` that does not wrap, so it is the widest
 * thing in the APPEARANCE card and the piece the layout breakpoint has to keep
 * room for; [tag] lets a test assert it still fits inside its card.
 */
@Composable
private fun RampStrip(label: String, ramp: List<Color>, dark: Boolean, tag: String) {
    val c = LocalDipi.current
    val industry = LocalIndustry.current
    Column {
        Text(
            label,
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.sp,
            letterSpacing = 0.178.em,
            color = if (dark) c.muted.copy(alpha = 0.7f) else industry.neutral500,
            modifier = Modifier.padding(bottom = 7.dp),
        )
        Row(Modifier.testTag(tag), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            ramp.forEach { swatch ->
                Box(
                    Modifier
                        .width(26.dp)
                        .height(18.dp)
                        .background(swatch, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

/**
 * A skin chip: 15dp gradient swatch + condensed label. Selected in Light is an
 * accent fill; selected in Dark keeps its true-colour swatch on the night tint
 * and carries a `SAVED` tag, because the skin is stored rather than applied.
 */
@Composable
private fun SkinButton(skin: DeskSkin, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val c = LocalDipi.current
    val shape = DeskStyle.controlShape
    val chip = remember(skin) { skin.chipGradientColors() }
    val fill = when {
        !selected -> Color.Transparent
        dark -> c.tint
        else -> c.accent
    }
    val label = when {
        selected && dark -> NightAccentText
        selected -> Color.White
        dark -> c.muted
        else -> c.foreground
    }
    Row(
        Modifier
            .height(40.dp)
            .background(fill, shape)
            .border(1.dp, if (selected) c.accent else c.hairlineStrong, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier
                .size(15.dp)
                .background(
                    Brush.linearGradient(0f to chip[0], 0.55f to chip[1], 1f to chip[2]),
                    RoundedCornerShape(3.dp),
                )
                .border(1.dp, Color(0x1F000000), RoundedCornerShape(3.dp)),
        )
        Text(
            skin.label.uppercase(),
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            letterSpacing = 0.096.em,
            color = label,
        )
        if (selected && dark) {
            Text(
                "SAVED",
                fontFamily = DipiMono,
                fontSize = 9.sp,
                letterSpacing = 0.122.em,
                color = IndustryPalette.Steel.accent500,
            )
        }
    }
}

// ------------------------------------------------------------------- TESTING

@Composable
private fun TestingCard(offline: Boolean, onToggleOffline: () -> Unit) {
    val c = LocalDipi.current
    SettingsCard {
        DeskKicker("TESTING", c.muted)
        SwitchRow(
            label = "Simulate offline",
            on = offline,
            onToggle = onToggleOffline,
            testTag = "toggle-offline",
            height = 44.dp,
        )
        Text(
            "Forces the cached list and the queue strip without pulling the plug.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = c.muted,
        )
    }
}

// --------------------------------------------------------- ACCOUNT & SESSION

@Composable
private fun AccountCard(
    session: Session?,
    dark: Boolean,
    lastSync: String?,
    queued: Int,
    appVersion: String,
    onLogout: () -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalDipi.current
    SettingsCard(modifier) {
        DeskKicker("ACCOUNT & SESSION", c.muted)
        Text(
            session?.displayName ?: "—",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = c.foreground,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            session?.centres?.firstOrNull()?.name ?: "",
            fontSize = 14.sp,
            color = c.muted,
            modifier = Modifier.padding(top = 6.dp),
        )
        SettingsRule(dark, Modifier.padding(top = 14.dp, bottom = 2.dp))
        SessionRow("Last synced", lastSync ?: "just now")
        SessionRow("Queue", "$queued waiting")
        if (appVersion.isNotBlank()) SessionRow("App version", appVersion)
        Button(
            onClick = onLogout,
            modifier = Modifier
                .padding(top = 18.dp)
                .height(44.dp),
            shape = DeskStyle.pillShape,
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = Color.White),
            contentPadding = PaddingValues(horizontal = 30.dp),
        ) { Text("Log out", fontSize = 15.sp) }
        SettingsRule(dark, Modifier.padding(top = 18.dp, bottom = 14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clickable(onClick = onErase),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The fixed severity pair — never follows the skin.
            Text("Erase all local data", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.hard)
        }
        Text(
            "Removes the saved password, session cookie, course cache, and queued status changes from this tablet.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = c.muted,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

@Composable
private fun SessionRow(key: String, value: String) {
    val c = LocalDipi.current
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(key, fontSize = 13.5.sp, color = c.muted, modifier = Modifier.weight(1f))
            Text(value, fontFamily = DipiMono, fontSize = 12.5.sp, color = c.foreground)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.hairline.copy(alpha = 0.45f)),
        )
    }
}

// -------------------------------------------------------------------- pieces

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = LocalDipi.current
    Column(
        modifier
            .fillMaxWidth()
            .deskCard(fill = c.field, border = c.hairline, elevation = 0.dp)
            .padding(start = 18.dp, end = 18.dp, top = 15.dp, bottom = 17.dp),
    ) { content() }
}

/**
 * The inner rule: the design's neutral-200 in Light, the night ramp's divider
 * step in Dark. Card-internal, so it sits one step softer than the card border.
 */
@Composable
private fun SettingsRule(dark: Boolean, modifier: Modifier = Modifier) {
    val c = LocalDipi.current
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (dark) c.hairline else c.hover),
    )
}

/**
 * The desk's single-fire switch row (the pattern `CentreOpsScreen.ToggleRow`
 * established): the row carries the click AND the On/Off semantics, and the
 * `Switch` is display-only so a thumb tap cannot double-fire.
 */
@Composable
private fun SwitchRow(
    label: String,
    on: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    height: Dp = 48.dp,
) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(height)
            .toggleable(value = on, onValueChange = { onToggle() }, role = Role.Switch)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.5.sp, color = c.foreground, modifier = Modifier.weight(1f))
        Switch(
            checked = on,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = c.muted,
                uncheckedTrackColor = c.field,
                uncheckedBorderColor = c.hairlineStrong,
            ),
        )
    }
}

/**
 * Dark's accent text — Steel accent-300 `#B5D9FD`. Fixed, not skin-derived:
 * in Dark the live Industry palette still holds the *stored* skin's light
 * ladder, and frame 1e runs one night ramp for every skin.
 */
private val NightAccentText = IndustryPalette.Steel.accent300

/**
 * Frame 1e's `NIGHT ACCENT` strip, ground → light. Six rungs are the Steel
 * accent ladder read from its token block; rungs 2, 4 and 6 are literals
 * **invented by the frame** — interpolations between the Steel rungs with no
 * token and no README source behind them. They are parked as drawn: if the
 * accent ladder ever gains those steps, replace them with the tokens.
 */
private val NightAccentRamp = listOf(
    IndustryPalette.Steel.accent900,
    Color(0xFF25384B), // no token — frame interpolation between accent900/800
    IndustryPalette.Steel.accent800,
    Color(0xFF365670), // no token — frame interpolation between accent800/700
    IndustryPalette.Steel.accent700,
    Color(0xFF4E7195), // no token — frame interpolation between accent700/accent
    IndustryPalette.Steel.accent,
    IndustryPalette.Steel.accent500,
    IndustryPalette.Steel.accent300,
)

/**
 * The Steel night ladder, read live off the theme rather than copied out of it —
 * this strip's whole job is to show what the night ramp actually is, so a hand-
 * kept list would start lying the moment `DarkDipi` moved. Only rung 7 has no
 * token of its own; it is derived from the two either side of it.
 */
@Composable
private fun nightNeutralRamp(): List<Color> {
    val c = LocalDipi.current
    return listOf(
        c.background, c.field, c.hover, c.hairline, c.hairlineStrong, c.snack,
        lerp(c.snack, c.muted, 0.45f),
        c.muted, c.foreground,
    )
}
