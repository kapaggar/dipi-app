#!/usr/bin/env bash
# Install a built :desktop distribution into ~/.local for Steam Deck Desktop Mode.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DEST="${DIPI_INSTALL_DIR:-$HOME/.local/opt/dipi-staff}"
BINARIES="$ROOT/desktop/build/compose/binaries/main"
if [[ ! -d "$BINARIES" ]]; then
  echo "No compose binaries yet. Run: ./gradlew :desktop:packageDistributionForCurrentOS" >&2
  exit 1
fi
mkdir -p "$DEST" "$HOME/.local/bin" "$HOME/.local/share/applications"
# Prefer an AppImage; otherwise copy the unpacked app tree.
APPIMAGE="$(find "$BINARIES" -name '*.AppImage' -print -quit || true)"
if [[ -n "$APPIMAGE" ]]; then
  install -m 0755 "$APPIMAGE" "$DEST/DIPI-Staff.AppImage"
  ln -sfn "$DEST/DIPI-Staff.AppImage" "$HOME/.local/bin/dipi-staff"
else
  TREE="$(find "$BINARIES" -type d -name 'dipi-staff' -print -quit || true)"
  if [[ -z "$TREE" ]]; then
    echo "Could not find dipi-staff tree under $BINARIES" >&2
    exit 1
  fi
  rsync -a --delete "$TREE/" "$DEST/"
  ln -sfn "$DEST/bin/dipi-staff" "$HOME/.local/bin/dipi-staff"
fi
sed "s|^Exec=.*|Exec=$HOME/.local/bin/dipi-staff --deck|" \
  "$ROOT/desktop/packaging/dipi-staff.desktop" \
  > "$HOME/.local/share/applications/dipi-staff.desktop"
echo "Installed. Launch: dipi-staff --deck"
echo "Add as a non-Steam game in Game Mode if you want it on the OLED home row."
