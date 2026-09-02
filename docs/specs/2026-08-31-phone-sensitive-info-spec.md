# Phone card: ID verification + health disclosures — spec (T2)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42.
**Origin:** repo scan 2026-08-31 — `SensitiveInfo` (parsed ID docs + noise-filtered
health disclosures) is populated identically at every width, but only the tablet's
`ApplicationsPane` receives it. The phone `CardScreen` has no parameter for it, so
under 1100dp the NPI sits in memory all session and is never shown, and the
health-disclosure reminder snackbar never fires. The owner amendment of 2026-08-16
permits **display** for desk-side verification; the gap is UI wiring only.

## Governing rules (verbatim, non-negotiable)

- `SensitiveInfo` is display-only: **never** persisted (no Room/DataStore/DTO
  field), never logged, in-memory session map only. This spec adds zero new
  storage — it only threads the existing `state.sensitiveById` map to one more
  screen.
- Do NOT extract or reuse the desk's `IdVerificationBlock`/`HealthPanel`
  (`ApplicationsPane.kt:449, 490`). They are styled with `Industry` tokens, which
  are skin-aware but **not dark-aware**; `CardScreen` renders in dark mode via
  `LocalDipi`. Reusing them would repeat the "Blossom pink strip in dark" token
  bug class fixed on 2026-08-30. Write phone-native equivalents with `LocalDipi`.

## Changes

### 1. `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/CardScreen.kt`

Add a defaulted parameter so the existing call site keeps compiling:

```kotlin
fun CardScreen(
    card: ApplicantCard,
    photoNote: String,
    dark: Boolean,
    onChangeStatus: () -> Unit,
    onPhoto: () -> Unit,
    sensitive: SensitiveInfo? = null,
)
```

Render order on the card (matching the desk's priority — health above audit):
after the monk/photo lines and before the audit section, insert:

```kotlin
val health = sensitive?.health.orEmpty()
if (health.isNotEmpty()) {
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, c.accent, RoundedCornerShape(8.dp))
            .background(c.accentSoft, RoundedCornerShape(8.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("HEALTH · VERIFY WITH APPLICANT", fontFamily = DipiMono, fontWeight = FontWeight.Medium, fontSize = 9.5.sp, letterSpacing = 1.5.sp, color = c.accent)
        health.forEach { (label, text) ->
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = c.foreground)
                Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = c.foreground)
            }
        }
    }
}
Spacer(Modifier.height(12.dp))
Column(
    Modifier.fillMaxWidth()
        .border(1.dp, c.hairline, RoundedCornerShape(8.dp))
        .padding(horizontal = 15.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(7.dp),
) {
    Text("ID VERIFICATION", fontFamily = DipiMono, fontWeight = FontWeight.Medium, fontSize = 9.5.sp, letterSpacing = 1.5.sp, color = c.muted)
    val idLabel = sensitive?.idLabel
    val idNumber = sensitive?.idNumber
    if (idLabel != null && idNumber != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(idLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = c.foreground)
            Text(idNumber, fontFamily = DipiMono, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = c.foreground)
        }
    } else {
        Text("No ID on file", fontSize = 13.sp, color = c.muted)
    }
}
```

`c.accentSoft`: use the existing `DipiColors` member closest to the desk's
`accent100` wash — check `core/ui/.../DipiTheme.kt` for the token the status
sheet / queued strip uses (`c.snack` family is NOT it). If no soft-accent token
exists in `DipiColors`, use `c.accent.copy(alpha = 0.10f)` — do not add a new
token, and never inline hex.

### 2. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt` (`CardPane`, line 683)

```kotlin
CardScreen(
    card = card,
    photoNote = vm.photoNote(card),
    dark = state.dark,
    onChangeStatus = vm::openSheet,
    onPhoto = vm::openPhotos,
    sensitive = state.sensitiveById[card.id],
)
```

### 3. `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt` (`openCard`, line 968)

Fire the same one-shot health reminder the desk fires in `selectDeskApp`
(lines 665-684). `openCard` is the single phone entry (Today rows and
Advanced Search both route through it), and the phone snackbar host at
`DipiAppUi.kt:290` renders `state.snack` whenever the desk is not active.

In `openCard`, extend the first `_state.update`:

```kotlin
returnTo = DeskScreen.Search.takeIf { _state.value.screen == DeskScreen.Search }
_state.update { cur ->
    val hasHealth = cur.sensitiveById[card.id]?.health?.isNotEmpty() == true
    cur.copy(
        card = card,
        screen = DeskScreen.Card,
        snack = if (hasHealth) {
            FlushSnack("Health disclosures on file — review before confirming", error = false)
        } else {
            cur.snack
        },
    )
}
```

The wording must match `selectDeskApp`'s exactly. Leave `selectDeskApp` alone.

## Tests

New file `app/src/test/kotlin/org/dhamma/dipi/staff/CardSensitiveTest.kt`
(Robolectric, `@Config(qualifiers = "w411dp-h891dp")` — phone shape):

- `healthPanelRendersDisclosures` — compose `CardScreen` with
  `SensitiveInfo(idLabel = "Aadhaar", idNumber = "1234 5678 9012", health = mapOf("Medication" to "Insulin, morning"))`;
  assert "HEALTH · VERIFY WITH APPLICANT", "Medication" and "Insulin, morning"
  are displayed with real bounds (`getUnclippedBoundsInRoot` height > 0 — not
  the 1px weak-assertion trap).
- `idBlockRendersLabelAndNumber` — assert "Aadhaar" and "1234 5678 9012" displayed.
- `nullSensitiveShowsNoIdOnFile` — `sensitive = null`: "No ID on file" displayed,
  health kicker absent (`assertDoesNotExist`).
- `emptyHealthHidesPanel` — `SensitiveInfo(idLabel = "PAN", idNumber = "X", health = emptyMap())`:
  health kicker absent.

Never touched: `ApplicationsPane` and its tests, `SensitiveInfoParserTest`,
`HealthNoiseFilterTest`, `selectDeskApp` behaviour, `TodayScreenTest`.

## Constraints

No new persistence of any `SensitiveInfo` field; no logging of values (the model's
redacted `toString()` stays the only stringification); tokens via `LocalDipi`;
≥48dp on any new interactive control (none are added); no agent trailers;
never bare `./gradlew test`.
