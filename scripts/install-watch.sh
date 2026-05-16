#!/usr/bin/env bash
#
# Installs the debug APK onto a paired Wear OS device over ADB.
#
# Prereqs:
#   1. Enable ADB Debugging on the watch (Settings → Developer options).
#   2. Enable Wireless debugging on the watch.
#   3. FIRST TIME ONLY — pair the host. On the watch tap "Pair new device";
#      it shows a 6-digit code + IP:PAIR-port. From the Linux host run:
#          adb pair 192.168.1.2:<pair-port>     # enter the 6-digit code
#   4. The main "Wireless debugging" screen shows IP:CONNECT-port (different
#      from the pair port). From the Linux host run:
#          adb connect 192.168.1.2:<connect-port>
#
# Then:  scripts/install-watch.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="$ROOT/wearos/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK" ]; then
    echo "[install] No APK found; building it first..." >&2
    "$ROOT/scripts/build-apk.sh"
fi
echo "[install] devices visible to adb:"
adb devices
echo "[install] installing $APK"
adb install -r "$APK"
echo "[install] launching activity..."
adb shell am start -n com.brunos3d.wearosclaude.debug/com.brunos3d.wearosclaude.MainActivity
