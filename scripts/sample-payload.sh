#!/usr/bin/env bash
#
# Pretty-prints the live /usage payload from the backend. Handy when the watch
# misbehaves and you want to confirm what the host is actually sending.
set -euo pipefail
HOST="${1:-localhost}"
PORT="${PORT:-47823}"
URL="http://$HOST:$PORT/usage"
echo "GET $URL"
if command -v jq >/dev/null 2>&1; then
    curl -s "$URL" | jq
else
    curl -s "$URL"
    echo
fi
