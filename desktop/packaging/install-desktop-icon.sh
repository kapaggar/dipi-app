#!/usr/bin/env bash
# Put dipi-staff.desktop on THIS machine's Desktop (SteamOS/KDE or XFCE).
# Run on the Steam Deck in Desktop Mode — not on macOS.
set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
ICON_SRC="${DIPI_ICON:-$HERE/icons/dipi-staff.png}"
LAUNCHER="$HERE/launch-dipi-staff.sh"
# Steam Deck user is `deck`. Always use ~/Desktop (not a Mac Desktop).
DESKTOP_DIR="${XDG_DESKTOP_DIR:-$HOME/Desktop}"
DESKTOP_DIR="${DESKTOP_DIR/#\$HOME/$HOME}"
APP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/512x512/apps"
BIN_DIR="$HOME/.local/bin"
OPT_BIN="$HOME/.local/opt/dipi-staff/bin/dipi-staff"

if [[ ! -f "$ICON_SRC" ]]; then
  echo "Missing icon: $ICON_SRC" >&2
  exit 1
fi
chmod +x "$LAUNCHER" "$HERE/install-steam-deck.sh" "$HERE/install-on-steam-deck.sh" 2>/dev/null || true

mkdir -p "$DESKTOP_DIR" "$APP_DIR" "$ICON_DIR" "$BIN_DIR" "$HOME/.local/share/icons"
install -m 0644 "$ICON_SRC" "$ICON_DIR/dipi-staff.png"
install -m 0644 "$ICON_SRC" "$HOME/.local/share/icons/dipi-staff.png"
if [[ -x "$LAUNCHER" ]]; then
  ln -sfn "$LAUNCHER" "$BIN_DIR/dipi-staff"
fi

PACKAGED_BIN="${DIPI_BIN:-}"
if [[ -z "$PACKAGED_BIN" ]]; then
  for candidate in \
    "$OPT_BIN" \
    "$ROOT/desktop/build/compose/binaries/main/app/dipi-staff/bin/dipi-staff"
  do
    if [[ -x "$candidate" ]]; then
      PACKAGED_BIN="$candidate"
      break
    fi
  done
fi
if [[ -n "$PACKAGED_BIN" && -x "$PACKAGED_BIN" ]]; then
  ln -sfn "$PACKAGED_BIN" "$BIN_DIR/dipi-staff-bin"
fi

# Prefer the packaged binary so a Deck without JDK still launches.
if [[ -x "$BIN_DIR/dipi-staff-bin" ]]; then
  EXEC_LINE="$BIN_DIR/dipi-staff-bin --windowed"
elif [[ -x "$BIN_DIR/dipi-staff" ]]; then
  EXEC_LINE="$BIN_DIR/dipi-staff --windowed"
else
  echo "No dipi-staff binary yet. Run install-on-steam-deck.sh from the Deck bundle." >&2
  exit 1
fi

USER_DIRS="$HOME/.config/user-dirs.dirs"
mkdir -p "$HOME/.config"
if [[ -f "$USER_DIRS" ]]; then
  if grep -q '^XDG_DESKTOP_DIR=' "$USER_DIRS"; then
    sed -i 's|^XDG_DESKTOP_DIR=.*|XDG_DESKTOP_DIR="$HOME/Desktop"|' "$USER_DIRS"
  else
    printf 'XDG_DESKTOP_DIR="$HOME/Desktop"\n' >> "$USER_DIRS"
  fi
else
  printf 'XDG_DESKTOP_DIR="$HOME/Desktop"\n' > "$USER_DIRS"
fi
export XDG_DESKTOP_DIR="$HOME/Desktop"
DESKTOP_DIR="$HOME/Desktop"
mkdir -p "$DESKTOP_DIR"

write_desktop() {
  local dest="$1"
  cat > "$dest" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=DIPI Staff
Comment=Registrar desk for dipi.vridhamma.org
Exec=$EXEC_LINE
Icon=$HOME/.local/share/icons/dipi-staff.png
Path=$HOME
Terminal=false
StartupNotify=true
Categories=Office;
StartupWMClass=org-dhamma-dipi-staff-desktop-MainKt
EOF
  chmod 0755 "$dest"
}

# Application menu (always) + the two Desktop names people look for.
write_desktop "$APP_DIR/dipi-staff.desktop"
write_desktop "$DESKTOP_DIR/dipi-staff.desktop"
write_desktop "$DESKTOP_DIR/DIPI Staff.desktop"

trust_desktop() {
  local dest="$1"
  [[ -f "$dest" ]] || return 0
  if command -v gio >/dev/null 2>&1; then
    gio set "$dest" metadata::trusted true 2>/dev/null || true
    gio set "$dest" "metadata::xfce-exe-checksum" "$(sha256sum "$dest" | awk '{print $1}')" 2>/dev/null || true
  fi
}
trust_desktop "$DESKTOP_DIR/dipi-staff.desktop"
trust_desktop "$DESKTOP_DIR/DIPI Staff.desktop"
trust_desktop "$APP_DIR/dipi-staff.desktop"

if command -v xdg-desktop-icon >/dev/null 2>&1; then
  xdg-desktop-icon install --novendor "$APP_DIR/dipi-staff.desktop" 2>/dev/null || true
fi

# XFCE (cloud/dev VMs): show file icons.
if command -v xfconf-query >/dev/null 2>&1; then
  xfconf-query -c xfce4-desktop -p /desktop-icons/style -n -t int -s 2 2>/dev/null || \
    xfconf-query -c xfce4-desktop -p /desktop-icons/style -s 2 || true
  xfconf-query -c xfce4-desktop -p /desktop-icons/file-icons/enabled -n -t bool -s true 2>/dev/null || true
fi
if command -v xfdesktop >/dev/null 2>&1; then
  xfdesktop --reload 2>/dev/null || true
fi

# SteamOS / KDE Plasma: Folder View so ~/Desktop files appear on the wallpaper.
enable_plasma_folder_view() {
  local qdbus_cmd=""
  qdbus_cmd="$(command -v qdbus6 || command -v qdbus || true)"
  [[ -n "$qdbus_cmd" ]] || return 0
  "$qdbus_cmd" org.kde.plasmashell /PlasmaShell org.kde.PlasmaShell.evaluateScript '
    var allDesktops = desktops();
    for (var i = 0; i < allDesktops.length; i++) {
      var d = allDesktops[i];
      try { d.containmentType = "org.kde.plasma.folder"; } catch (e) {}
    }
  ' 2>/dev/null || true
}
enable_plasma_folder_view

if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database "$APP_DIR" 2>/dev/null || true
fi

echo "Steam Deck / Linux desktop launchers:"
echo "  $DESKTOP_DIR/dipi-staff.desktop"
echo "  $DESKTOP_DIR/DIPI Staff.desktop"
echo "  $APP_DIR/dipi-staff.desktop"
echo "Double-click DIPI Staff on this machine's Desktop (Desktop Mode on the Deck)."
echo "If the icon is missing: right-click the wallpaper → Configure Desktop and Wallpaper → Layout → Folder View."
echo "First launch may need: right-click the icon → Allow Launching."
