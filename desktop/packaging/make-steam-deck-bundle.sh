#!/usr/bin/env bash
# Build a folder you copy onto the Steam Deck (not macOS).
# Output: desktop/dist/DIPI-Staff-SteamDeck/ and a .tar.gz next to it.
set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
APP="$ROOT/desktop/build/compose/binaries/main/app/dipi-staff"
OUT="$ROOT/desktop/dist/DIPI-Staff-SteamDeck"

if [[ ! -x "$APP/bin/dipi-staff" ]]; then
  echo "Packaging the Linux desk first…"
  (
    cd "$ROOT"
    ./gradlew -Pdipi.desktopOnly=true :desktop:createDistributable
  )
fi
if [[ ! -x "$APP/bin/dipi-staff" ]]; then
  echo "Missing $APP/bin/dipi-staff" >&2
  exit 1
fi

rm -rf "$OUT"
mkdir -p "$OUT/app" "$OUT/icons"
cp -a "$APP/." "$OUT/app/"
install -m 0644 "$HERE/icons/dipi-staff.png" "$OUT/icons/dipi-staff.png"
install -m 0755 "$HERE/install-on-steam-deck.sh" "$OUT/install-on-steam-deck.sh"
install -m 0755 "$HERE/install-desktop-icon.sh" "$OUT/install-desktop-icon.sh"
install -m 0755 "$HERE/launch-dipi-staff.sh" "$OUT/launch-dipi-staff.sh"
install -m 0644 "$HERE/dipi-staff.desktop" "$OUT/dipi-staff.desktop"

cat > "$OUT/Install-DIPI-Staff.desktop" <<'EOF'
[Desktop Entry]
Version=1.0
Type=Application
Name=Install DIPI Staff on this Steam Deck
Comment=Writes dipi-staff.desktop onto this Deck's Desktop
Exec=/bin/bash -c 'p="%k"; p="${p#file://}"; cd "$(dirname "$p")" && ./install-on-steam-deck.sh'
Icon=icons/dipi-staff.png
Terminal=false
StartupNotify=true
Categories=Utility;
EOF
chmod 0755 "$OUT/Install-DIPI-Staff.desktop"

cat > "$OUT/README.txt" <<'EOF'
DIPI Staff — install on THIS Steam Deck (not on a Mac)

1. Switch the Deck to Desktop Mode (power button → Switch to Desktop).
2. Copy this whole DIPI-Staff-SteamDeck folder onto the Deck
   (USB stick, KDE Connect, scp, or Dolphin).
3. Open Konsole on the Deck:

     cd ~/Desktop/DIPI-Staff-SteamDeck   # or wherever you copied it
     ./install-on-steam-deck.sh

   Or double-click Install-DIPI-Staff.desktop (Allow Launching if asked).

4. Look on the Deck wallpaper for "DIPI Staff" / dipi-staff.desktop.
   You can also open the Application Launcher and search "DIPI Staff".

If the wallpaper stays empty: right-click it → Configure Desktop and
Wallpaper → Layout → Folder View. Then right-click the icon → Allow Launching.
EOF

tar -C "$ROOT/desktop/dist" -czf "$ROOT/desktop/dist/DIPI-Staff-SteamDeck.tar.gz" DIPI-Staff-SteamDeck
echo "Copy this folder onto the Steam Deck:"
echo "  $OUT"
echo "Or the archive:"
echo "  $ROOT/desktop/dist/DIPI-Staff-SteamDeck.tar.gz"
