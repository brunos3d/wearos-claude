#!/usr/bin/env bash
#
# Production server startup. Loads nvm, builds backend if dist is stale,
# then exec-replaces itself with the node process.
#
# Usage:  scripts/start.sh
#         (also called by the systemd service — see scripts/install-service.sh)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="$ROOT/backend/dist/backend/src/server.js"

# Load nvm so node/npm are on PATH even in non-interactive shells (e.g. systemd)
export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
# shellcheck disable=SC1091
[ -s "$NVM_DIR/nvm.sh" ] && source "$NVM_DIR/nvm.sh" --no-use

if ! command -v node &>/dev/null; then
  echo "[start] node not found — is nvm installed?" >&2
  exit 1
fi

# Build if dist is absent or any source file is newer than the compiled entry
needs_build() {
  [ ! -f "$DIST" ] && return 0
  find "$ROOT/backend/src" "$ROOT/shared" -name '*.ts' -newer "$DIST" 2>/dev/null | grep -q .
}

if needs_build; then
  echo "[start] building backend..." >&2
  cd "$ROOT/backend"
  npm install --no-audit --no-fund --silent
  npx tsc -p tsconfig.json
fi

cd "$ROOT/backend"
exec node "$DIST"
