#!/usr/bin/env bash
#
# Installs the wearos-claude systemd user service so the backend starts
# automatically at login (and at boot if linger is enabled).
#
# Usage:  scripts/install-service.sh [--uninstall]
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE_NAME="wearos-claude"
UNIT_SRC="$ROOT/scripts/${SERVICE_NAME}.service"
UNIT_DIR="$HOME/.config/systemd/user"
UNIT_DST="$UNIT_DIR/${SERVICE_NAME}.service"

if [[ "${1:-}" == "--uninstall" ]]; then
  systemctl --user stop    "$SERVICE_NAME" 2>/dev/null || true
  systemctl --user disable "$SERVICE_NAME" 2>/dev/null || true
  rm -f "$UNIT_DST"
  systemctl --user daemon-reload
  echo "[install-service] service removed"
  exit 0
fi

chmod +x "$ROOT/scripts/start.sh"

mkdir -p "$UNIT_DIR"
cp "$UNIT_SRC" "$UNIT_DST"
systemctl --user daemon-reload
systemctl --user enable --now "$SERVICE_NAME"

# Allow the service to run at boot even when the user is not logged in
if ! loginctl show-user "$USER" 2>/dev/null | grep -q "Linger=yes"; then
  echo "[install-service] enabling linger so the service survives logout..."
  loginctl enable-linger "$USER"
fi

echo "[install-service] done"
echo ""
systemctl --user status "$SERVICE_NAME" --no-pager
