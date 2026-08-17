#!/usr/bin/env bash
# Finish Galaxy Watch Ultra debloat: remove remaining non-Ultra watchfaces.
# Usage:
#   ./scripts/finish-watch-debloat.sh [WATCH_SERIAL]
# If connect shows offline, pair first:
#   adb pair 10.0.0.223:<pair_port> <6-digit-code>
#   adb connect 10.0.0.223:44433

set -euo pipefail

WATCH="${1:-10.0.0.223:44433}"
ADB="${ADB:-adb}"

echo "=== Finish Galaxy Watch debloat ==="
$ADB connect "$WATCH" >/dev/null 2>&1 || true
state="$($ADB -s "$WATCH" get-state 2>/dev/null || echo offline)"
if [[ "$state" != "device" ]]; then
  echo "ERROR: $WATCH state=$state"
  echo "On watch: Developer options → Wireless debugging → Pair new device"
  echo "Then: adb pair 10.0.0.223:<pair_port> <code> && adb connect $WATCH"
  exit 1
fi

echo "Device: $($ADB -s "$WATCH" shell getprop ro.product.model)"
echo "Before: $($ADB -s "$WATCH" shell pm list packages | grep -c watchface) watchface packages"
echo

REMOVE=(
  com.samsung.android.watch.watchface.dynamicfont
  com.samsung.android.watch.watchface.simpledigital
  com.samsung.android.watch.watchface.perpetual
  com.samsung.android.watch.watchface.spherenumber
  com.samsung.android.watch.watchface.photosticker
  com.samsung.android.watch.watchface.stretchindex
  com.samsung.android.watch.watchface.basicclock
  com.samsung.android.watch.watchface.tickingsound
  com.samsung.android.watch.watchface.activitynumber
  com.samsung.android.watch.watchface.digitalmodular
  com.samsung.android.watch.watchface.spatialnumber
  com.samsung.android.watch.watchface.sleepcoaching
  com.watchfacestudio.radariv
)

removed=0
for pkg in "${REMOVE[@]}"; do
  if $ADB -s "$WATCH" shell pm path "$pkg" 2>/dev/null | grep -q package; then
    result="$($ADB -s "$WATCH" shell pm uninstall -k --user 0 "$pkg" 2>&1)"
    echo "$pkg: $result"
    removed=$((removed + 1))
  else
    echo "$pkg: already removed"
  fi
done

echo
echo "Removed $removed packages"
echo "After: $($ADB -s "$WATCH" shell pm list packages | grep -c watchface) watchface packages"
echo "Remaining:"
$ADB -s "$WATCH" shell pm list packages | grep watchface | sort
