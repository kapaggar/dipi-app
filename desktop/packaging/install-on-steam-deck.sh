#!/usr/bin/env bash
# Install DIPI Staff ON THIS Steam Deck (Desktop Mode).
# Does nothing on macOS. Copy the DIPI-Staff-SteamDeck folder to the Deck first.
set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
DEST="${DIPI_INSTALL_DIR:-$HOME/.local/opt/dipi-staff}"
APP=""

if [[ -x "$HERE/app/bin/dipi-staff" ]]; then
  APP="$HERE/app"
elif [[ -x "$HERE/../build/compose/binaries/main/app/dipi-staff/bin/dipi-staff" ]]; then
  APP="$(cd "$HERE/../build/compose/binaries/main/app/dipi-staff" && pwd)"
elif [[ -x "$HERE/../../desktop/build/compose/binaries/main/app/dipi-staff/bin/dipi-staff" ]]; then
  APP="$(cd "$HERE/../../desktop/build/compose/binaries/main/app/dipi-staff" && pwd)"
fi

if [[ -z "$APP" ]]; then
  echo "No packaged desk app next to this script." >&2
  echo "On a Linux build machine: ./desktop/packaging/make-steam-deck-bundle.sh" >&2
  echo "Then copy desktop/dist/DIPI-Staff-SteamDeck/ onto this Steam Deck and run this script again." >&2
  if command -v kdialog >/dev/null 2>&1; then
    kdialog --error "DIPI Staff is not packed yet. Copy DIPI-Staff-SteamDeck onto this Deck and run install-on-steam-deck.sh from that folder."
  fi
  exit 1
fi

mkdir -p "$DEST" "$HOME/.local/bin"
if command -v rsync >/dev/null 2>&1; then
  rsync -a --delete "$APP/" "$DEST/"
else
  rm -rf "$DEST"
  mkdir -p "$DEST"
  cp -a "$APP/." "$DEST/"
fi
chmod +x "$DEST/bin/dipi-staff" 2>/dev/null || true
ln -sfn "$DEST/bin/dipi-staff" "$HOME/.local/bin/dipi-staff-bin"

export DIPI_BIN="$DEST/bin/dipi-staff"
ICON="$HERE/icons/dipi-staff.png"
if [[ ! -f "$ICON" ]]; then
  ICON="$HERE/app/lib/dipi-staff.png"
fi
if [[ -f "$ICON" ]]; then
  export DIPI_ICON="$ICON"
fi

# Reuse the shared shortcut writer when we are still inside the git tree.
if [[ -x "$HERE/install-desktop-icon.sh" ]]; then
  "$HERE/install-desktop-icon.sh"
else
  echo "Missing install-desktop-icon.sh next to this script." >&2
  exit 1
fi

if command -v steamos-add-to-steam >/dev/null 2>&1; then
  steamos-add-to-steam "$DEST/bin/dipi-staff" 2>/dev/null || true
fi

MSG="DIPI Staff is on THIS Steam Deck.

Desktop files:
  $HOME/Desktop/dipi-staff.desktop
  $HOME/Desktop/DIPI Staff.desktop

Also in the Application Launcher — search “DIPI Staff”.

If you do not see the icon on the wallpaper:
  1. Right-click the wallpaper → Configure Desktop and Wallpaper
  2. Layout → Folder View → Apply
  3. Right-click the icon → Allow Launching
  4. Double-click DIPI Staff"

echo "$MSG"
if command -v kdialog >/dev/null 2>&1; then
  kdialog --msgbox "$MSG" || true
elif command -v zenity >/dev/null 2>&1; then
  zenity --info --text="$MSG" || true
fi
