# Setup

## Prerequisites

| Tool                          | Version            | Notes                                               |
| ----------------------------- | ------------------ | --------------------------------------------------- |
| Node.js                       | ≥ 20               | nvm-installed Node is fine                          |
| Android Studio                | Iguana or newer    | only required if you want to edit in an IDE         |
| Android SDK                   | API 35 (Wear OS 4) | platform-tools + build-tools 35.0.0+                |
| JDK                           | 17                 | bundled with Android Studio, or system-installed    |
| Galaxy Watch 6 Classic        | Wear OS 4+         | any Wear OS 3+ device should work                   |
| Claude Code                   | recent             | the OAuth probe reads `~/.claude/.credentials.json` |
| Same Wi-Fi for host and watch | —                  | the watch hits the backend directly                 |

## 1. Backend

```bash
npm install
npm run dev
```

You should see something like:

```
wearos-claude backend listening on http://0.0.0.0:47823
  oauthProbe: every 10m
  claudeHome: /home/you/.claude
  authRequired: false
```

That's it. **No `.env` editing is required for first run** — the only
values worth tweaking are documented in `.env.example`:

- `AUTH_TOKEN` — set a shared secret if you don't want everyone on your LAN
  to be able to hit `/usage`.
- `OAUTH_PROBE_INTERVAL_MINUTES` — bump higher to spend fewer messages.
- `OAUTH_PROBE_DISABLED=1` — turn the probe off entirely. The backend will
  estimate usage from local JSONL transcripts against a plan-aware budget
  (inferred from your Claude Code credentials).

### Sanity checks

```bash
curl -s http://localhost:47823/health
curl -s http://localhost:47823/usage | jq
npm run probe                 # one-shot dump of what /usage would return now
```

### Where the numbers come from

- **Live probe path (default):** the backend sends a 1-token request to
  `POST https://api.anthropic.com/v1/messages` using your Claude Code OAuth
  token and reads the `anthropic-ratelimit-unified-*` response headers.
  Each probe costs ONE message against your 5-hour quota. Default cadence
  is every 10 minutes (≈30 probes per 5-hour window).
- **JSONL fallback:** when the probe is disabled or fails, the backend
  aggregates `usage` blocks from `~/.claude/projects/*.jsonl` and computes
  a rough percentage against a plan-aware budget inferred from
  `subscriptionType` / `rateLimitTier` in `~/.claude/.credentials.json`.
  No quota cost, less accurate.

You should never need to manually configure token budgets.

## 2. Wear OS app

### Configure the backend URL

The watch needs to reach the Linux host over Wi-Fi. Find your LAN IP:

```bash
scripts/lan-ip.sh
# eg.  192.168.1.7
```

Bake it into the APK at build time:

```bash
BACKEND_URL=http://192.168.1.7:47823 scripts/build-apk.sh
```

Without `BACKEND_URL`, the script auto-detects your LAN IP (via
`scripts/lan-ip.sh`) and grabs the `AUTH_TOKEN` from `backend/.env`.

### Pair adb over Wi-Fi (Galaxy Watch 6 Classic)

1. On the watch: `Settings → About watch → Software → tap build number 7×` to
   unlock Developer options.
2. `Settings → Developer options → ADB debugging → on`.
3. Same screen: `Wireless debugging → on`.
4. **First time only — pair the device.** On the watch, tap
   `Pair new device`. The watch shows a **6-digit pairing code** and an
   `IP:PORT` (the port for pairing is different from the port for
   connecting — the watch shows both). From your Linux host, run:

   ```bash
   adb pair 192.168.1.2:<pair-port>    # use the IP and PAIR-port from the watch
   # → enter the 6-digit code when prompted
   ```

   You should see `Successfully paired to …`. You only do this once per
   host — the watch remembers it.

5. Now connect. The connect port is the one shown on the main "Wireless
   debugging" screen, **not** the pairing port:

   ```bash
   adb connect 192.168.1.2:<connect-port>
   adb devices                            # confirm it shows as "device"
   ```

### Install

```bash
scripts/install-watch.sh
```

This will rebuild if needed, `adb install -r` onto the watch, and launch
the activity.

### Add the tile

On the watch, long-press the watch face → swipe to the right-most slot →
tap `+` → pick **claude.watch**. Tap the tile to open the full dashboard.
Long-press it to remove or reorder.

### Add the complication

`Settings → Watchfaces → customize → pick a slot → claude.watch`. The
complication shows `XX%` plus the current mood label.

## 3. Day-to-day

- Run `scripts/dev.sh` on the host whenever you want telemetry.
- The watch app polls every 60 s while open; the Tile refreshes every 60 s
  while on the carousel.
- To inspect the raw payload from your phone or browser:
  `http://<lan-ip>:47823/usage`.

Run into trouble? See [DEBUG.md](DEBUG.md).
