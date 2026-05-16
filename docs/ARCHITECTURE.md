# Architecture

```
 ┌──────────────────┐         ┌─────────────────────┐         ┌────────────────────┐
 │ Anthropic        │ headers │  Local Fastify      │  HTTP   │  Wear OS app       │
 │ /v1/messages     │ ──────▶ │  backend (this repo)│ ──────▶ │  Tile + Activity   │
 │ (1-token probe)  │         │  + JSONL fallback   │ /usage  │  (Compose + Tiles) │
 └──────────────────┘         └─────────────────────┘         └────────────────────┘
```

Three components, one wire contract, no cloud. The project speaks Claude
Code only — no provider abstraction, no OpenAI path, no API-key mode.

## Components

### 1. `backend/` — Fastify + TypeScript

Single process, single endpoint, no database. Two complementary data
sources stitched into one payload by `src/usage.ts`:

**OAuth probe** (`src/collectors/anthropicProbe.ts`)
- Reads `claudeAiOauth.accessToken` from `~/.claude/.credentials.json`.
- Sends a 1-token `POST /v1/messages` request to `api.anthropic.com` using
  the Claude Code OAuth scope (`anthropic-beta: oauth-2025-04-20`).
- Parses `anthropic-ratelimit-unified-5h-utilization`, `…-5h-reset`,
  `…-5h-status`, `…-7d-utilization`, `…-7d-reset`, `…-7d-status`, and
  `…-representative-claim` from the response.
- Cached at `OAUTH_PROBE_INTERVAL_MINUTES` so the cost stays bounded.

**JSONL fallback** (`src/collectors/claudeCode.ts`)
- Walks `~/.claude/projects/*/*.jsonl` and aggregates `usage` blocks within
  the last 7 days.
- Cache reads are weighted at 0.1×, cache creation at 1.25× to approximate
  *billable* tokens (the naive sum massively over-counts cache reads).
- Used both for "is the user actively coding right now?" (drives mood) and
  as a graceful fallback when the probe is unavailable.

**Plan inference** (`src/lib/planProfile.ts`)
- Only consulted by the JSONL fallback path — the probe headers come
  pre-normalized against the user's plan.
- Reads `subscriptionType` / `rateLimitTier` from
  `~/.claude/.credentials.json` and picks a built-in profile for Pro / Max
  / Team / Free. Unknown plans fall back to "Pro-like" defaults.
- Numbers are deliberately approximate. The probe is the source of truth.

**Mood engine** (`src/lib/mood.ts`)
- Default mood is `idle`. Only veers off that for unambiguous states:
  `offline` (probe + JSONL both failed) and `sleeping` (>12h since activity).
- The mascot exists for personality, not as a meter — the dashboard
  already renders the raw percentage.

**Route layer** (`src/routes/usage.ts`)
- `GET /usage` — returns the cached payload (15 s TTL) or computes fresh.
- `GET /usage/fresh` — bypass cache.
- `GET /health`, `GET /ping`, `GET /` — anonymous liveness probes.
- Optional `x-wearos-claude-token` shared-secret check on non-open paths.

### 2. `wearos/` — Kotlin + Compose for Wear OS

Single Activity, single Tile, single complication.

- **`AppGraph`** — hand-rolled singleton container shared by every Wear OS
  surface, so they all reuse one in-memory `UsageRepository`.
- **`UsageRepository`** — dedupes in-flight HTTP fetches with a `Mutex` and
  falls back to the most recent payload on network error.
- **`MascotFrames`** — the source of truth for the ASCII mascot. Tile and
  Compose both render the same frames.
- **`MascotView`** — drives the per-frame animation loop (≈380 ms tick) with
  a phosphor-style alpha pulse.
- **`UsageTileService`** — renders the same dashboard via ProtoLayout. ASCII
  lines become individual `Text` children inside a `Column` to keep the
  monospace grid aligned without shipping a bitmap atlas.
- **`UsageComplicationService`** — exposes the current usage % to any watch
  face that supports `SHORT_TEXT` or `RANGED_VALUE` complications.
- **`SettingsScreen`** — on-watch editor for backend URL + auth token,
  driven by `RemoteInputIntentHelper`.

### 3. `shared/usage-contract.ts`

The wire schema. `data/UsagePayload.kt` mirrors it field-for-field.

## Data flow

1. The Activity's `LaunchedEffect` polls every 60 seconds (or on tap).
2. Each poll calls `UsageRepository.load()`.
3. The repository returns the cache if fresh, otherwise calls `UsageApi`,
   a Ktor + OkHttp client with a 6 s timeout and 2 retries.
4. The backend re-runs `collectUsage()` at most once per 15 seconds and
   serves the latest payload as JSON.
5. The Activity also pings `TileService.getUpdater().requestUpdate` so the
   home Tile stays in sync.
6. The complication is scheduled by Wear OS every 120 seconds (declared in
   `AndroidManifest.xml`).

## Why no phone companion

The Galaxy Watch 6 Classic is a fully standalone Wear OS device with Wi-Fi.
The manifest declares
`meta-data com.google.android.wearable.standalone = true`. Provided the
watch is on the same Wi-Fi as the Linux host, it can hit
`http://<host>:47823/usage` directly. No Data Layer API, no companion app.

## Battery footprint

- 60 s polling, single round-trip per poll (~1 KB payload).
- Tile uses a `freshnessIntervalMillis` of 60 000 ms.
- Complication update period is 120 s.
- Animation only runs while the Activity is foregrounded — the Tile is a
  single-frame snapshot and stops drawing when off-screen.
- Pure-black background means most pixels stay off on AMOLED.

## Quota footprint of the OAuth probe

- Each `/v1/messages` probe burns exactly **one message** from your 5-hour
  Claude quota.
- Default cadence (`OAUTH_PROBE_INTERVAL_MINUTES=10`) → ~30 probes per 5-h
  window.
- Disable entirely with `OAUTH_PROBE_DISABLED=1`. The backend will then
  estimate usage from local JSONL transcripts only — rougher numbers, but
  zero quota cost.

## Extending

- **New mood** → add a wire value to `Mood` in `shared/usage-contract.ts`,
  add frames to `MascotFrames`, add a mapping in `deriveMood`.
- **New screen** → add a Compose composable and an entry to the bottom-nav
  in `MainActivity`.
- **New endpoint** → add a route to `routes/usage.ts`. Remember to add it
  to `OPEN_PATHS` if it should bypass auth.
