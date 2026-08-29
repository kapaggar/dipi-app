package org.dhamma.dipi.staff.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dhamma.dipi.staff.ui.R
import org.dhamma.dipi.staff.ui.theme.DeskSkin
import org.dhamma.dipi.staff.ui.theme.DipiColors
import org.dhamma.dipi.staff.ui.theme.DipiCondensed
import org.dhamma.dipi.staff.ui.theme.DipiMono
import org.dhamma.dipi.staff.ui.theme.DipiSans
import org.dhamma.dipi.staff.ui.theme.LocalDipi
import org.dhamma.dipi.staff.ui.theme.LoginLotusRelief
import org.dhamma.dipi.staff.ui.theme.deskCard

/** README frame 1b: the login card alone is radius 10dp. */
private val cardShape = RoundedCornerShape(10.dp)

/** README frame 1b: fields, the error strip and SIGN IN are radius 6dp. */
private val controlShape = RoundedCornerShape(6.dp)

private val checkboxShape = RoundedCornerShape(3.dp)

private const val CAPTION = "Your centre is read from your account after sign-in."

/**
 * Sign-in (v4 frame 1b): the form lives in a centred card 380dp wide — never
 * full-screen-width fields — floating over the lotus relief (the vector mark at
 * readable opacity with a vertical gradient fade, see [LoginLotusRelief]).
 * **No photo hero, never full-bleed** — that was removed at 1.15.0 and sits on
 * the handover's do-not-re-propose list.
 *
 * The card collapses while the IME is up so SIGN IN and the server error stay
 * reachable: brand block to one row, the orientation caption hidden, remember-me
 * moved onto the button row. The manifest sets `adjustResize` and the activity
 * goes edge-to-edge, so [WindowInsets.isImeVisible] is the whole trigger — no
 * second screen, no navigation (spec R8). The arrangement itself is a pure
 * function of [LoginCard]'s `imeVisible`, which is what the tests drive.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    username: String,
    password: String,
    error: String?,
    loading: Boolean,
    onUser: (String) -> Unit,
    onPass: (String) -> Unit,
    onSubmit: () -> Unit,
    remember: Boolean = false,
    onRemember: (Boolean) -> Unit = {},
    skin: DeskSkin = DeskSkin.Steel,
    lotus: Boolean = true,
) {
    val c = LocalDipi.current
    // `isImeVisible` alone is not enough: Robolectric's stub insets report every
    // type visible at zero height, which would ship the compact card to a device
    // with no keyboard up. Under `adjustResize` the keyboard is exactly the space
    // the ime inset takes, so require both.
    val imeVisible = WindowInsets.isImeVisible &&
        WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Box(Modifier.fillMaxSize().background(c.background)) {
        if (lotus) {
            LoginLotusRelief()
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (imeVisible) 12.dp else 30.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoginCard(
                imeVisible = imeVisible,
                username = username,
                password = password,
                error = error,
                loading = loading,
                onUser = onUser,
                onPass = onPass,
                onSubmit = onSubmit,
                remember = remember,
                onRemember = onRemember,
            )
        }
    }
}

/**
 * The 380dp card of frame 1b. Public, not private, for one reason: Robolectric
 * cannot raise a real IME, so the compact/tall split has to be drivable as a
 * plain parameter from the unit tests. [LoginScreen] is the only production
 * caller and supplies the flag from the window insets.
 *
 * Tall (`imeVisible == false`): stacked brand block → error strip → fields →
 * remember-me on its own row → SIGN IN → the orientation caption.
 * Compact (`imeVisible == true`): one-row brand → error strip → fields →
 * a single 44dp action row carrying remember-me and SIGN IN; caption dropped.
 */
@Composable
fun LoginCard(
    imeVisible: Boolean,
    username: String,
    password: String,
    error: String?,
    loading: Boolean,
    onUser: (String) -> Unit,
    onPass: (String) -> Unit,
    onSubmit: () -> Unit,
    remember: Boolean = false,
    onRemember: (Boolean) -> Unit = {},
) {
    val c = LocalDipi.current
    Column(
        Modifier
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .deskCard(shape = cardShape, fill = c.field, border = c.hairline, elevation = 3.dp)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
    ) {
        LoginBrand(imeVisible)

        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(13.dp))
            LoginErrorStrip(error)
        }

        Spacer(Modifier.height(13.dp))
        LoginField(
            label = "Username",
            value = username,
            onValue = onUser,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(11.dp))
        LoginField(
            label = "Password",
            value = password,
            onValue = onPass,
            password = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )

        Spacer(Modifier.height(14.dp))
        if (imeVisible) {
            Row(
                Modifier.fillMaxWidth().height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RememberMe(remember, onRemember, Modifier.weight(1f))
                SignInButton(loading, onSubmit)
            }
        } else {
            RememberMe(remember, onRemember, Modifier.fillMaxWidth().height(44.dp))
            Spacer(Modifier.height(14.dp))
            SignInButton(loading, onSubmit)
            Spacer(Modifier.height(16.dp))
            Text(CAPTION, fontSize = 12.sp, lineHeight = 18.sp, color = c.muted)
        }
    }
}

/**
 * Brand: stacked (mark, wordmark, kicker) with room to breathe, or one 34dp
 * row while the IME is up.
 */
@Composable
private fun LoginBrand(imeVisible: Boolean) {
    val c = LocalDipi.current
    val wordmark: @Composable () -> Unit = {
        Text(
            "DIPI Staff",
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.Bold,
            fontSize = if (imeVisible) 21.sp else 32.sp,
            lineHeight = if (imeVisible) 23.sp else 34.sp,
            color = c.foreground,
        )
    }
    val kicker: @Composable () -> Unit = {
        Text(
            "Centre admin desk",
            fontSize = if (imeVisible) 11.5.sp else 13.sp,
            color = c.muted,
        )
    }
    if (imeVisible) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.lotus_mark),
                contentDescription = "DIPI",
                modifier = Modifier.size(34.dp),
            )
            Column(Modifier.padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                wordmark()
                kicker()
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .clip(controlShape)
                    .background(c.background.copy(alpha = 0.78f))
                    .border(1.dp, c.accent, controlShape)
                    .padding(7.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.lotus_mark),
                    contentDescription = "DIPI",
                    modifier = Modifier.size(46.dp),
                )
            }
            wordmark()
            kicker()
        }
    }
}

/**
 * The failure strip: a 3dp severity bar, a title and the server's **verbatim**
 * text. Every colour is a fixed hex. Severity never follows the skin, and a
 * blend off a skin-derived token (`field`, `muted`) would have made it do
 * exactly that — so the light container is the README's own hexes, verbatim,
 * and the night container is a companion pair chosen the same way. Contrast on
 * the container each one sits on: body 5.9:1 light, 8.8:1 dark; title 5.9:1
 * light, 5.5:1 dark — all clear of AA-small at 12/12.5sp, which matters here
 * because this is the one string a locked-out registrar has to read.
 */
@Composable
private fun LoginErrorStrip(error: String) {
    val c = LocalDipi.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(controlShape)
            .background(errorFill(c))
            .border(1.dp, errorBorder(c), controlShape),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(c.hard))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                "Sign-in failed",
                fontWeight = FontWeight.Medium,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                color = c.hard,
            )
            Text(
                error,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = errorBody(c),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

/**
 * True on the Steel night ramp. `DipiColors` carries no dark flag, so the two
 * places that legitimately need one (the fixed-severity container and the
 * focused field's lift) read it off the ground's own luminance rather than
 * having a boolean threaded down from `DipiTheme` into every call site.
 */
private fun DipiColors.isNight(): Boolean = background.luminance() < 0.5f

// Fixed severity, light — README frame 1b, verbatim. These are companions to
// the `hard` pair (#A33A34 / #E0796F) and follow no skin.
private val ErrorFillLight = Color(0xFFFBEFEE)
private val ErrorBorderLight = Color(0xFFE8CDC9)
private val ErrorBodyLight = Color(0xFF7A5450)

// Fixed severity, night. The README draws only the light strip, so these are
// its companions on the Steel night ramp: a warm ground one step above the
// night card (#1A1E22), a rule that reads against it, and a body tint at
// 8.8:1 on that ground (and 8.5:1 on the bare card, had it been transparent).
private val ErrorFillDark = Color(0xFF2A1D1D)
private val ErrorBorderDark = Color(0xFF5A3B38)
private val ErrorBodyDark = Color(0xFFE3B3AC)

private fun errorFill(c: DipiColors): Color = if (c.isNight()) ErrorFillDark else ErrorFillLight

private fun errorBorder(c: DipiColors): Color = if (c.isNight()) ErrorBorderDark else ErrorBorderLight

private fun errorBody(c: DipiColors): Color = if (c.isNight()) ErrorBodyDark else ErrorBodyLight

/** 19dp checkbox + label, the whole row toggling as one control. */
@Composable
private fun RememberMe(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = LocalDipi.current
    Row(
        modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(19.dp)
                .clip(checkboxShape)
                .background(if (checked) c.accent else Color.Transparent)
                .border(1.5.dp, if (checked) c.accent else c.hairlineStrong, checkboxShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Canvas(Modifier.size(11.dp)) {
                    val w = size.width
                    val h = size.height
                    val stroke = 2.dp.toPx()
                    drawLine(
                        Color.White,
                        Offset(0.06f * w, 0.52f * h),
                        Offset(0.40f * w, 0.84f * h),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        Color.White,
                        Offset(0.40f * w, 0.84f * h),
                        Offset(0.94f * w, 0.16f * h),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
        Text(
            "Remember me",
            modifier = Modifier.padding(start = 9.dp),
            color = c.foreground,
            fontSize = 13.5.sp,
        )
    }
}

/** 148×44dp solid-accent primary (README frame 1b). */
@Composable
private fun SignInButton(loading: Boolean, onSubmit: () -> Unit) {
    val c = LocalDipi.current
    Box(
        Modifier
            .width(148.dp)
            .height(44.dp)
            .clip(controlShape)
            .background(if (loading) c.accentPressed else c.accent)
            .clickable(enabled = !loading, onClick = onSubmit),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (if (loading) "Signing in…" else "Sign in").uppercase(),
            fontFamily = DipiCondensed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 2.2.sp,
            color = Color.White,
        )
    }
}

/**
 * The focused field's fill — one step *away* from the card ground, in whichever
 * direction the running ramp lifts. The README's `#FFF` is a light-mode
 * rendering of that idea; on the night ramp a white field would glare, so the
 * night side takes `hover` (`#22272C`), the step above `field` (`#1A1E22`) that
 * the ramp already reserves for raised chrome. Light has no token above `field`
 * — `#F5F5F8` is the top of the neutral ladder — so it takes the README's white
 * directly.
 */
private fun focusedFieldFill(c: DipiColors): Color = if (c.isNight()) c.hover else Color.White

/**
 * 40dp field under a mono kicker. Idle sits on the field fill behind a hairline;
 * focus lifts the fill ([focusedFieldFill]) and thickens the rule to 2dp accent,
 * which is the only chromatic mark on the card besides SIGN IN.
 */
@Composable
private fun LoginField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    password: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val c = LocalDipi.current
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val lifted = focusedFieldFill(c)
    Column {
        Text(
            label.uppercase(),
            fontFamily = DipiMono,
            fontWeight = FontWeight.Medium,
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            letterSpacing = 1.5.sp,
            color = c.muted,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            interactionSource = interactions,
            textStyle = TextStyle(
                fontFamily = DipiSans,
                fontSize = 14.5.sp,
                color = c.foreground,
            ),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(if (focused) lifted else c.field, controlShape)
                        .border(
                            if (focused) 2.dp else 1.dp,
                            if (focused) c.accent else c.hairline,
                            controlShape,
                        )
                        .padding(horizontal = if (focused) 11.dp else 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}
