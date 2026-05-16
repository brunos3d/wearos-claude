#!/usr/bin/env bash
set -euo pipefail
adb uninstall com.brunos3d.wearosclaude.debug || true
adb uninstall com.brunos3d.wearosclaude || true
