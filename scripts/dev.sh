#!/usr/bin/env bash
#
# Boots the local backend in dev/watch mode. The watch can then point at
# http://<this-machine>:47823 over Wi-Fi.
#
# Usage:  scripts/dev.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/backend"
if [ ! -d node_modules ]; then
    echo "[dev] installing backend deps..." >&2
    npm install --no-audit --no-fund
fi
exec npm run dev
