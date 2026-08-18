# DIPI Staff on Steam Deck OLED (Linux)

Native **Compose Desktop** client (`:desktop`). Same live Drupal desk as the Android tablet — not Waydroid, not a WebView, not `/staff/*`.

**Version:** 2.0.0  
**Panel:** 1280×800 OLED. Default theme is true-black for the OLED panel.  
**OS:** SteamOS 3 (Arch) Desktop Mode, or any x86_64 Linux.

## What it is

Login → centre from `dh_user_center` → upcoming courses → v2 desk (Board, Applications, Audit, Calling, Check-in, Rooms) → `GET /change-status` → settings.

Hard rules stay: no client ACL, never send `Approved`, no NPI on disk, no `r=` on sheet GETs, HTML sheets stay in memory.

## Run from the repo

Needs JDK 17+ (21 is fine). No Android SDK.

```bash
./gradlew -Pdipi.desktopOnly=true :core:model:test :core:audit:test :core:protocol:test :desktop:test
./gradlew -Pdipi.desktopOnly=true :desktop:run
# fixtures only (no live host):
./gradlew -Pdipi.desktopOnly=true :desktop:run --args='--mock'
```

`-Pdipi.desktopOnly=true` (or `DIPI_DESKTOP_ONLY=true`) skips the Android modules so you do not need an Android SDK.

Flags / env:

| Flag / env | Meaning |
|---|---|
| `--mock` / `DIPI_USE_MOCK=true` | In-process MockWebServer |
| `--base-url URL` / `DIPI_BASE_URL` | Debug host override (login has no URL field) |
| `--deck` / `--fullscreen` / `SteamDeck=1` | Undecorated 1280×800 window |
| `--windowed` | Decorated window |
| `--data-dir` / `DIPI_DATA_DIR` | Override `~/.local/share/dipi-staff` |

## Install on the Deck (Desktop Mode)

1. Switch to **Desktop Mode**.
2. Build a Linux bundle on any x86_64 machine with JDK 17+:

```bash
./gradlew :desktop:packageDistributionForCurrentOS
```

Artifacts land under `desktop/build/compose/binaries/`. Prefer the **AppImage** if the toolchain produced one; otherwise use the unpacked `dipi-staff` directory (bundled JRE).

3. Copy to the Deck (`~/Applications/DIPI Staff.AppImage` or `~/.local/opt/dipi-staff/`).
4. `chmod +x` the AppImage / launcher.
5. Optional: install the `.desktop` file from `desktop/packaging/dipi-staff.desktop`.
6. Steam Game Mode: *Add a non-Steam game* → the AppImage or `dipi-staff` binary. Set resolution 1280×800. Steam Input as mouse is enough; Esc is Back.

`desktop/packaging/install-desktop-icon.sh` puts a lotus **DIPI Staff** icon on `~/Desktop` so you can double-click to launch (also writes `~/.local/share/applications`). It enables XFCE file icons if they were hidden.

`desktop/packaging/install-steam-deck.sh` copies a built distribution into `~/.local/opt/dipi-staff` and then runs the desktop-icon installer.

## Data on disk

`~/.local/share/dipi-staff/` (0600):

- `.key` + `secret.bin` — AES-GCM cookies + remember-me
- `prefs.json` — theme, check-ins, call log, centre ops (no NPI)
- `worklist.json` / `outbox.json` — public card fields only
- `cache/sheets/` — streamed PDF/Excel/CSV only; wiped on logout / erase-all

ID documents and health disclosures are in-memory `SensitiveInfo` only.

## Calling / documents

`xdg-open` for `tel:`, `https://wa.me/…`, and cached sheet files. In Game Mode, install a dialer/WhatsApp in Desktop Mode first or use the Applications pane without the hand-off.

## Not ported

Photo review/upload (mock-only on Android; live desk has no upload). Phone-stacked hub — the Deck is always the 1280-wide desk.
