# Debug guide

## Backend

### `/usage` is `{ ... "degraded": true, "tokensInWindow": 0 }`

The collector couldn't find any recent JSONL transcripts under
`~/.claude/projects`. Likely causes:

- `~/.claude` doesn't exist (run Claude Code once, even on a throwaway
  prompt, to populate it).
- `CLAUDE_HOME` in `.env` points somewhere wrong.
- Sessions are older than 7 days — the collector intentionally caps at a
  week to keep parsing cheap.

Run `npm run probe` for a verbose dump of the config + raw signals.

### Numbers feel wildly off

There are two cases:

- **Probe path (default).** The percentages come straight from Anthropic's
  `anthropic-ratelimit-unified-*-utilization` headers. If they look wrong,
  they're the same numbers Claude Code itself shows. File a bug against
  Anthropic, not us.
- **JSONL fallback** (`OAUTH_PROBE_DISABLED=1`, or the probe is failing).
  The backend approximates *billable* tokens by weighting cache reads at
  10% and cache writes at 1.25×, then divides by the budget for your
  inferred plan tier. Plan is inferred from `subscriptionType` /
  `rateLimitTier` in `~/.claude/.credentials.json`. If `npm run probe`
  shows `extras.plan: fallback:unknown`, your credentials file didn't have
  a recognizable tier — the fallback is using Pro-like defaults.

If you want raw token volume, edit
`backend/src/collectors/claudeCode.ts → sumUsageTokens`.

### Watch can't reach the server

```bash
# From the host:
curl -s http://0.0.0.0:47823/health      # works → server is fine
curl -s http://<lan-ip>:47823/health     # fails → firewall most likely
```

On Manjaro:

```bash
# UFW example. Replace with firewalld / iptables on other distros.
sudo ufw allow 47823/tcp
```

Note `usesCleartextTraffic="true"` is set in the manifest so plain HTTP on
the LAN works without needing a TLS cert. If you want TLS, wire up a
reverse proxy in front of the backend and update `BACKEND_URL` accordingly.

## Wear OS app

### "offline" mascot, red glyph

Either the URL is wrong, the host firewall is blocking 47823, or the
backend isn't running. Quick checks:

```bash
adb shell ping -c2 <lan-ip-of-host>     # watch can reach host
adb shell curl -s http://<lan-ip>:47823/health   # only works on rooted/dev watches
```

If `ping` works but the app still shows offline, double-check the URL the
APK was built with (look at `BuildConfig.DEFAULT_BACKEND_URL` in logs):

```bash
adb shell logcat -d | grep wearosclaude
```

### Tile says "loading" forever

Long-press the tile → remove it → re-add it. Wear OS sometimes hangs onto
the very first failed `onTileRequest` payload. Alternatively from the host:

```bash
adb shell cmd appops set com.brunos3d.wearosclaude.debug \
    com.google.android.permission.RUN_WEAR_TILE allow
adb shell am broadcast \
    -a com.google.android.wearable.action.REQUEST_TILE_UPDATE \
    -n com.brunos3d.wearosclaude.debug/.tile.UsageTileService
```

### Complication doesn't show

Verify the complication slot on your watch face supports `SHORT_TEXT` or
`RANGED_VALUE`. Long-press the face → customize → pick the slot → if
`claude.watch` isn't listed, force-stop the app and reopen it once to
re-register the data source.

```bash
adb shell am force-stop com.brunos3d.wearosclaude.debug
adb shell am start -n com.brunos3d.wearosclaude.debug/.MainActivity
```

### Mascot is misaligned

Every frame in `MascotFrames` must be exactly `ROWS=4` lines, each padded
to `COLS=11` columns. The constructor asserts this; if a contributor adds a
new frame that violates the grid, the app crashes immediately on launch.
Restore the canonical body lines from `MascotFrames.kt` if in doubt.

## Adb cheat-sheet

```bash
adb devices                                # confirm pairing
adb logcat -c                              # clear buffer
adb logcat *:S WearOsClaude:V              # filter to our tag (set TAG in source)
adb shell dumpsys activity service \
   com.brunos3d.wearosclaude.debug/.tile.UsageTileService
adb shell am start \
   -a android.intent.action.MAIN \
   -n com.brunos3d.wearosclaude.debug/.MainActivity
```
