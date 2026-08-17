#!/usr/bin/env bash
# Audit Galaxy Watch Ultra debloat + tuning before disabling ADB.
# Usage: ./scripts/audit-galaxy-watch-debloat.sh [WATCH_SERIAL]
# Default: 10.0.0.223:44433

set -euo pipefail

WATCH="${1:-10.0.0.223:44433}"
ADB="${ADB:-adb}"

echo "=== Galaxy Watch debloat audit ==="
echo "Target: $WATCH"
echo

$ADB connect "$WATCH" >/dev/null 2>&1 || true
state="$($ADB -s "$WATCH" get-state 2>/dev/null || echo offline)"
if [[ "$state" != "device" ]]; then
  echo "ERROR: watch not reachable (state=$state). Enable Wireless debugging and retry."
  exit 1
fi

echo "--- Device ---"
$ADB -s "$WATCH" shell getprop ro.product.model
$ADB -s "$WATCH" shell getprop ro.build.display.id
$ADB -s "$WATCH" shell dumpsys battery | grep -E "level|status|powered|Wireless"
echo

check_removed() {
  local pkg="$1"
  if $ADB -s "$WATCH" shell pm path "$pkg" 2>/dev/null | grep -q package; then
    echo "FAIL  still installed: $pkg"
    return 1
  else
    echo "OK    removed: $pkg"
    return 0
  fi
}

check_disabled() {
  local pkg="$1"
  local enabled
  enabled="$($ADB -s "$WATCH" shell dumpsys package "$pkg" 2>/dev/null | grep -m1 'enabled=' || true)"
  if echo "$enabled" | grep -qE 'enabled=3|enabled=2|disabled'; then
    echo "OK    disabled: $pkg"
    return 0
  elif $ADB -s "$WATCH" shell pm path "$pkg" 2>/dev/null | grep -q package; then
    echo "FAIL  still enabled: $pkg ($enabled)"
    return 1
  else
    echo "OK    removed: $pkg"
    return 0
  fi
}

check_present() {
  local pkg="$1"
  if $ADB -s "$WATCH" shell pm path "$pkg" 2>/dev/null | grep -q package; then
    echo "OK    present: $pkg"
    return 0
  else
    echo "FAIL  missing: $pkg"
    return 1
  fi
}

echo "--- Tier 2 debloat (should be removed/disabled) ---"
FAIL=0
REMOVED=(
  com.google.android.wearable.assistant
  com.samsung.android.watch.weather
  com.google.android.apps.messaging
  com.samsung.android.watch.budscontroller
  com.samsung.android.intellivoiceservice
  com.samsung.android.dqagent
  com.sec.android.diagmonagent
  com.samsung.android.watch.runestone.app
  com.google.android.apps.wearable.retailattractloop
  com.samsung.android.wearable.media.sessions
  com.google.android.wearable.protolayout.renderer
  com.google.android.wearable.overlay.home.merlot
  com.samsung.android.watch.compass
  com.samsung.android.watch.worldclock
  com.sec.android.easyMover
  com.samsung.android.wear.smartswitchassistant
  com.samsung.android.messaging
  com.samsung.android.app.reminder
  com.samsung.android.wearable.music
  com.samsung.android.calendar
  com.samsung.android.watch.selfdiagnostics
  com.samsung.android.aircommandmanager
  com.sec.android.app.samsungapps
  com.google.android.partnersetup
  com.android.managedprovisioning
  com.sec.android.app.wlantest
  com.android.networkstack.tethering
  com.sec.facatfunction
  com.android.adservices.api
  com.sec.android.app.hwmoduletest
  com.sec.android.app.servicemodeapp
  com.samsung.accessibility
  com.android.providers.userdictionary
  com.google.android.marvin.talkback
  com.android.cts.ctsshim
  com.android.cts.priv.ctsshim
)
DISABLED=(
  com.android.vending
  com.sec.android.soagent
  com.wssyncmldm
  com.samsung.android.bixby.agent
  com.samsung.android.bixby.wakeup
)
for pkg in "${REMOVED[@]}"; do
  check_removed "$pkg" || FAIL=1
done
for pkg in "${DISABLED[@]}"; do
  check_disabled "$pkg" || FAIL=1
done
echo

echo "--- Must remain (core) ---"
CORE=(
  com.google.android.gms
  com.google.android.wearable.healthservices
  com.samsung.android.wear.shealth
  com.samsung.android.wcs.extension
  com.android.bluetooth
)
for pkg in "${CORE[@]}"; do
  check_present "$pkg" || FAIL=1
done
echo

echo "--- Watchfaces ---"
echo "Ultra faces (keep):"
for pkg in \
  com.samsung.android.watch.watchface.ultraanalog \
  com.samsung.android.watch.watchface.ultrainfoboard \
  com.samsung.android.watch.watchface.simpleultra; do
  check_present "$pkg" || FAIL=1
done
echo "System helpers (keep):"
for pkg in \
  com.samsung.wear.watchface.runtime \
  com.samsung.android.watch.watchface.complicationhelper \
  com.samsung.android.watch.watchface.companionhelper \
  com.samsung.android.watch.watchface.face; do
  check_present "$pkg" || FAIL=1
done
echo "Remaining watchface packages:"
$ADB -s "$WATCH" shell pm list packages | grep watchface | sort
count="$($ADB -s "$WATCH" shell pm list packages | grep -c watchface || true)"
echo "Total watchface packages: $count (expect 7 after final cleanup, 20 before)"
echo

echo "--- Settings ---"
for kv in \
  "secure:wake_gesture_enabled:0" \
  "secure:double_tap_to_wake:0" \
  "global:wifi_scan_always_enabled:0" \
  "global:max_running_services:10" \
  "global:low_power:1" \
  "global:window_animation_scale:0.5" \
  "global:mobile_data:0"; do
  ns="${kv%%:*}"
  rest="${kv#*:}"
  key="${rest%%:*}"
  want="${rest##*:}"
  got="$($ADB -s "$WATCH" shell settings get "$ns" "$key" 2>/dev/null | tr -d '\r')"
  if [[ "$got" == "$want" ]]; then
    echo "OK    $ns.$key=$got"
  else
    echo "WARN  $ns.$key=$got (expected $want)"
    FAIL=1
  fi
done
echo "ADB (you will disable next):"
echo "  adb_enabled=$($ADB -s "$WATCH" shell settings get global adb_enabled | tr -d '\r')"
echo "  adb_wifi=$($ADB -s "$WATCH" shell settings get global adb_wifi_enabled 2>/dev/null | tr -d '\r' || echo n/a)"
echo

echo "--- Process count ---"
$ADB -s "$WATCH" shell ps -A | wc -l
echo

if [[ "$FAIL" -eq 0 ]]; then
  echo "RESULT: PASS — safe to disable Wireless debugging + ADB debugging on the watch."
else
  echo "RESULT: ISSUES FOUND — review FAIL/WARN lines above before disabling ADB."
fi
exit "$FAIL"
