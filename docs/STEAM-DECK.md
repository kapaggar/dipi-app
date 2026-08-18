# DIPI Staff on Steam Deck OLED (Linux)

Native **Compose Desktop** client (`:desktop`). Same live Drupal desk as the Android tablet — not Waydroid, not a WebView, not `/staff/*`.

**Version:** 2.0.1  
**Panel:** 1280×800 OLED. Default theme is true-black for the OLED panel.  
**OS:** SteamOS 3 (Arch) Desktop Mode, or any x86_64 Linux.

## Put the icon on the Steam Deck (not on a Mac)

The launcher is **`dipi-staff.desktop`**. It is **not** named `dpstaff.desktop`, and it will **not** appear on macOS Finder or a Mac Desktop.

On the Deck it lives at:

```
/home/deck/Desktop/dipi-staff.desktop
```

This cloud/Linux build machine cannot write to your Steam Deck. Copy the bundle over, then install **in Desktop Mode**.

### 1. Pack on a Linux box (this repo)

```bash
./desktop/packaging/make-steam-deck-bundle.sh
```

That writes `desktop/dist/DIPI-Staff-SteamDeck/` (bundled JRE, no Android SDK, no JDK on the Deck).

### 2. Copy that folder onto the Deck

USB stick, KDE Connect, or `scp -r desktop/dist/DIPI-Staff-SteamDeck deck@steamdeck.local:~/Desktop/`

### 3. On the Steam Deck (Desktop Mode)

Power button → **Switch to Desktop**. Open Konsole:

```bash
cd ~/Desktop/DIPI-Staff-SteamDeck
./install-on-steam-deck.sh
```

Or double-click `Install-DIPI-Staff.desktop` inside that folder (right-click → **Allow Launching** if Plasma asks).

### 4. Double-click DIPI Staff

Look on the **Deck wallpaper** for **DIPI Staff** / `dipi-staff.desktop`. You can also open the Application Launcher and search **DIPI Staff**.

If the wallpaper is empty:

1. Right-click the wallpaper → **Configure Desktop and Wallpaper**
2. **Layout → Folder View** → Apply  
   (SteamOS defaults to a widget desktop that hides `~/Desktop` files.)
3. Right-click the icon → **Allow Launching**
4. Double-click **DIPI Staff**

## What it is

Login → centre from `dh_user_center` → upcoming courses → v2 desk (Board, Applications, Audit, Calling, Check-in, Rooms) → `GET /change-status` → settings.

Hard rules stay: no client ACL, never send `Approved`, no NPI on disk, no `r=` on sheet GETs, HTML sheets stay in memory.

## Run from the repo (Linux only)

Needs JDK 17+ (21 is fine). No Android SDK. This path is for development, not the Deck icon.

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
| `--windowed` | Decorated window (Desktop Mode shortcut uses this) |
| `--data-dir` / `DIPI_DATA_DIR` | Override `~/.local/share/dipi-staff` |

The Desktop shortcut always passes `--windowed` so `SteamDeck=1` does not force fullscreen in Desktop Mode.

`desktop/packaging/install-steam-deck.sh` copies a built distribution into `~/.local/opt/dipi-staff` when you are already on a Linux machine that has `createDistributable` output.

Steam Game Mode: *Add a non-Steam game* → `~/.local/opt/dipi-staff/bin/dipi-staff`. Set resolution 1280×800. Steam Input as mouse is enough; Esc is Back. `install-on-steam-deck.sh` calls `steamos-add-to-steam` when that tool exists.

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
