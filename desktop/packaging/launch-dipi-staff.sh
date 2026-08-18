#!/usr/bin/env bash
# Double-click launcher for DIPI Staff (repo tree or ~/.local install).
set -euo pipefail

# Packaged Compose binaries ship their own JRE. JDK is only for the Gradle fallback.
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  if [[ -x /usr/lib/jvm/java-21-openjdk-amd64/bin/java ]]; then
    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  elif command -v java >/dev/null 2>&1; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  fi
fi
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:${PATH:-/usr/bin}"
fi

HERE="$(cd "$(dirname "$(readlink -f "$0")")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
LOG_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/dipi-staff"
mkdir -p "$LOG_DIR"
LOG="$LOG_DIR/launch.log"

# Prefer a packaged binary (createDistributable / install-steam-deck).
CANDIDATES=(
  "${DIPI_BIN:-}"
  "$HOME/.local/bin/dipi-staff-bin"
  "$HOME/.local/opt/dipi-staff/bin/dipi-staff"
  "$ROOT/desktop/build/compose/binaries/main/app/dipi-staff/bin/dipi-staff"
)

run_bin() {
  local bin="$1"
  shift
  exec "$bin" "$@"
}

for bin in "${CANDIDATES[@]}"; do
  if [[ -n "$bin" && -x "$bin" ]]; then
    # Avoid recursing into this wrapper if ~/.local/bin/dipi-staff is us.
    if [[ "$(readlink -f "$bin")" == "$(readlink -f "$0")" ]]; then
      continue
    fi
    run_bin "$bin" "$@" >>"$LOG" 2>&1
  fi
done

if [[ ! -x "$ROOT/gradlew" ]]; then
  echo "DIPI Staff: no packaged binary and no Gradle wrapper at $ROOT" >>"$LOG"
  if command -v zenity >/dev/null 2>&1; then
    zenity --error --text="DIPI Staff is not installed yet. Open a terminal in the repo and run:\n\n./gradlew -Pdipi.desktopOnly=true :desktop:createDistributable\n\nThen double-click this icon again."
  fi
  exit 1
fi

cd "$ROOT"
# First launch from the Desktop uses a decorated window so the close button is visible.
ARGS=("$@")
if [[ ${#ARGS[@]} -eq 0 ]]; then
  ARGS=(--windowed)
fi
exec ./gradlew -Pdipi.desktopOnly=true --quiet :desktop:run --args="${ARGS[*]}" >>"$LOG" 2>&1
