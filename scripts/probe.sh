#!/usr/bin/env bash
#
# One-shot dump of the active provider's /usage payload without booting the
# Fastify server. Great when iterating on collectors or budgets.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/backend"
exec npm run probe
