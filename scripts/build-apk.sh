#!/usr/bin/env bash
#
# Builds a debug APK from the wearos/ Gradle project.
#
# The script auto-detects sensible defaults so a fresh install on a real
# Galaxy Watch can talk to the backend out of the box:
#
#   * BACKEND_URL  — env override, else http://<host-lan-ip>:<port-from-.env>
#   * AUTH_TOKEN   — env override, else parsed from backend/.env
#
# The resulting APK is left at:
#   wearos/app/build/outputs/apk/debug/app-debug.apk
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT/backend/.env"

read_env() {
    local key="$1"
    [ -f "$ENV_FILE" ] || return 0
    local raw
    raw=$(grep -E "^${key}=" "$ENV_FILE" | tail -n1 | cut -d'=' -f2- || true)
    # strip optional surrounding quotes and trailing comments
    raw="${raw#\"}"; raw="${raw%\"}"
    raw="${raw#\'}"; raw="${raw%\'}"
    printf '%s' "$raw"
}

PORT="${PORT:-$(read_env PORT)}"
PORT="${PORT:-47823}"

if [ -z "${BACKEND_URL:-}" ]; then
    LAN_IP="$("$ROOT/scripts/lan-ip.sh" 2>/dev/null || true)"
    if [ -z "$LAN_IP" ]; then
        echo "[build] WARN: couldn't detect a LAN IP — falling back to localhost; pass BACKEND_URL=... to override." >&2
        LAN_IP="127.0.0.1"
    fi
    BACKEND_URL="http://$LAN_IP:$PORT"
fi

if [ -z "${AUTH_TOKEN:-}" ]; then
    AUTH_TOKEN="$(read_env AUTH_TOKEN || true)"
fi

cd "$ROOT/wearos"

ARGS=(":app:assembleDebug" "-Pwearosclaude.backendUrl=$BACKEND_URL")
if [ -n "${AUTH_TOKEN:-}" ]; then
    # Token can contain shell-meaningful chars in theory but we pass via -P
    # which gradle parses internally; safer than env file substitution.
    ARGS+=("-Pwearosclaude.authToken=$AUTH_TOKEN")
    TOKEN_HINT="(token: ${AUTH_TOKEN:0:4}…${AUTH_TOKEN: -2})"
else
    TOKEN_HINT="(no token)"
fi

echo "[build] baking BACKEND_URL=$BACKEND_URL $TOKEN_HINT"
./gradlew "${ARGS[@]}"

echo
echo "APK ready: $ROOT/wearos/app/build/outputs/apk/debug/app-debug.apk"
