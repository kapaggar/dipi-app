#!/usr/bin/env bash
# Put a double-clickable DIPI Staff icon on the Desktop (XFCE / SteamOS / KDE).
set -euo pipefail

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
ICON_SRC="$HERE/icons/dipi-staff.png"
LAUNCHER="$HERE/launch-dipi-staff.sh"
DESKTOP_DIR="${XDG_DESKTOP_DIR:-$HOME/Desktop}"
APP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/512x512/apps"
BIN_DIR="$HOME/.local/bin"

if [[ ! -f "$ICON_SRC" ]]; then
  echo "Missing icon: $ICON_SRC" >&2
  exit 1
fi
chmod +x "$LAUNCHER" "$HERE/install-steam-deck.sh" 2>/dev/null || true

mkdir -p "$DESKTOP_DIR" "$APP_DIR" "$ICON_DIR" "$BIN_DIR" "$HOME/.local/share/icons"
install -m 0644 "$ICON_SRC" "$ICON_DIR/dipi-staff.png"
install -m 0644 "$ICON_SRC" "$HOME/.local/share/icons/dipi-staff.png"
ln -sfn "$LAUNCHER" "$BIN_DIR/dipi-staff"

# Fast path: packaged Compose binary (createDistributable / ~/.local/opt).
PACKAGED_BIN="$ROOT/desktop/build/compose/binaries/main/app/dipi-staff/bin/dipi-staff"
if [[ -x "$PACKAGED_BIN" ]]; then
  ln -sfn "$PACKAGED_BIN" "$BIN_DIR/dipi-staff-bin"
elif [[ -x "$HOME/.local/opt/dipi-staff/bin/dipi-staff" ]]; then
  ln -sfn "$HOME/.local/opt/dipi-staff/bin/dipi-staff" "$BIN_DIR/dipi-staff-bin"
fi

# Keep XDG Desktop at ~/Desktop so the file actually appears on the wallpaper.
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

write_desktop() {
  local dest="$1"
  cat > "$dest" <<EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=DIPI Staff
Comment=Registrar desk for dipi.vridhamma.org
Exec=$BIN_DIR/dipi-staff --windowed
Icon=$HOME/.local/share/icons/dipi-staff.png
Path=$ROOT
Terminal=false
StartupNotify=true
Categories=Office;
StartupWMClass=org-dhamma-dipi-staff-desktop-MainKt
EOF
  chmod 0755 "$dest"
}

write_desktop "$APP_DIR/dipi-staff.desktop"
write_desktop "$DESKTOP_DIR/DIPI Staff.desktop"

# XFCE / GNOME: mark the Desktop shortcut as allowed to launch.
if command -v gio >/dev/null 2>&1; then
  gio set "$DESKTOP_DIR/DIPI Staff.desktop" metadata::trusted true 2>/dev/null || true
  gio set "$DESKTOP_DIR/DIPI Staff.desktop" "metadata::xfce-exe-checksum" "$(sha256sum "$DESKTOP_DIR/DIPI Staff.desktop" | awk '{print $1}')" 2>/dev/null || true
fi

# Show file/launcher icons on the XFCE wallpaper (style 0 = hidden).
if command -v xfconf-query >/dev/null 2>&1; then
  xfconf-query -c xfce4-desktop -p /desktop-icons/style -n -t int -s 2 2>/dev/null || \
    xfconf-query -c xfce4-desktop -p /desktop-icons/style -s 2 || true
  xfconf-query -c xfce4-desktop -p /desktop-icons/file-icons/enabled -n -t bool -s true 2>/dev/null || true
  xfconf-query -c xfce4-desktop -p /desktop-icons/file-icons/show-tooltips -n -t bool -s true 2>/dev/null || true
fi
if command -v xfdesktop >/dev/null 2>&1; then
  xfdesktop --reload 2>/dev/null || true
fi
if command -v update-desktop-database >/dev/null 2>&1; then
  update-desktop-database "$APP_DIR" 2>/dev/null || true
fi

echo "Desktop icon: $DESKTOP_DIR/DIPI Staff.desktop"
echo "Double-click DIPI Staff on the desktop to launch."
