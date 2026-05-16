# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`wearos-claude` is a local-first Claude Code telemetry dashboard for a Wear OS watch. **Claude Code only** — there is no generic provider abstraction, no OpenAI path, no API-key mode. Reject PRs/refactors that try to add one.

Three pieces, one wire contract, no cloud:
- `backend/` — Fastify + TypeScript. Single endpoint `GET /usage`, no DB.
- `wearos/` — Kotlin + Compose for Wear OS. One Activity, one Tile, one complication.
- `shared/usage-contract.ts` — wire schema. `wearos/.../data/UsagePayload.kt` mirrors it field-for-field.

NPM workspace; the backend is a workspace member, the Wear OS app is not (not Node). `package.json` at the root proxies `dev`/`build`/`start`/`typecheck` into the backend workspace.

## Commands

From the repo root:

```bash
npm install              # installs backend deps (workspace)
npm run dev              # starts backend (tsx watch) on 0.0.0.0:47823
npm run typecheck        # tsc --noEmit in backend
npm run build            # tsc → backend/dist
scripts/probe.sh         # curl GET /usage against localhost and pretty-print
scripts/build-apk.sh     # build debug APK with LAN IP + AUTH_TOKEN auto-detected
scripts/install-watch.sh # adb install -r and launch the activity
scripts/lan-ip.sh        # print host LAN IP (used by build-apk.sh)
```

From `backend/`:

```bash
npm run probe            # one-shot dump of what /usage would return (no Fastify)
npm run dev              # tsx watch src/server.ts
```

From `wearos/`:

```bash
./gradlew :app:assembleDebug \
  -Pwearosclaude.backendUrl=http://<lan-ip>:47823 \
  -Pwearosclaude.authToken=<token>
```

Use `scripts/build-apk.sh` instead of invoking gradle directly — it parses `backend/.env` for `AUTH_TOKEN`/`PORT` and runs `lan-ip.sh`.

## Architecture

### Data flow

`Anthropic /v1/messages` (OAuth probe) → `backend` (Fastify) → `/usage` JSON → Wear OS Activity / Tile / complication, all sharing one in-memory `UsageRepository` via `AppGraph`.

The backend has **two complementary data sources**, stitched by `backend/src/usage.ts → collectUsage()`:

1. **OAuth probe** (`backend/src/collectors/anthropicProbe.ts`) — reads `claudeAiOauth.accessToken` from `~/.claude/.credentials.json`, sends a 1-token `POST /v1/messages` with `anthropic-beta: oauth-2025-04-20`, parses `anthropic-ratelimit-unified-{5h,7d}-{utilization,reset,status}` and `…-representative-claim`. Authoritative. **Each probe burns one message** from the user's 5-hour quota; cached for `OAUTH_PROBE_INTERVAL_MINUTES` (default 10).
2. **JSONL fallback** (`backend/src/collectors/claudeCode.ts`) — walks `~/.claude/projects/*/*.jsonl`, weights cache reads at 0.1× and cache creation at 1.25× to approximate *billable* tokens, then divides by a plan-aware budget. Always run (drives mood/activity signal); becomes the primary path when the probe is disabled or fails.

Plan inference (`backend/src/lib/planProfile.ts`) is **only consulted by the JSONL fallback** — probe headers are pre-normalized 0..1 against the user's plan, so the probe path never needs to know plan limits.

### Route layer (`backend/src/routes/usage.ts`)

- `GET /usage` — cached payload, 15 s TTL (shared by Tile + Activity + complication so they all collapse into one fetch).
- `GET /usage/fresh` — bypass cache.
- `GET /health`, `GET /ping`, `GET /` — anonymous liveness. These are in `OPEN_PATHS`; **when adding a new public endpoint, add it to `OPEN_PATHS` to bypass the optional `x-wearos-claude-token` shared-secret check.**

### Wear OS app (`wearos/app/src/main/kotlin/com/brunos3d/wearosclaude/`)

- `AppGraph` — hand-rolled singleton container. Activity, Tile, and complication all read from the same `UsageRepository` instance.
- `data/UsageRepository.kt` — dedupes in-flight HTTP fetches with a `Mutex`; falls back to last-good payload on network error.
- `data/UsageApi.kt` — Ktor + OkHttp, 6 s timeout, 2 retries.
- `tile/UsageTileService.kt` — ProtoLayout. ASCII mascot rendered as one `Text` per line inside a `Column` so the monospace grid stays aligned without bitmaps.
- `complication/UsageComplicationService.kt` — `SHORT_TEXT` + `RANGED_VALUE`. Update period 120 s, declared in `AndroidManifest.xml`.
- `mascot/MascotFrames.kt` — **source of truth** for the ASCII mascot. Same string art renders in Compose `Text` and ProtoLayout `Text` — no bitmap atlas to sync.
- `ui/SettingsScreen.kt` — on-watch settings (backend URL, auth token) via `RemoteInputIntentHelper`.

`BuildConfig.DEFAULT_BACKEND_URL` and `DEFAULT_AUTH_TOKEN` are injected via `-Pwearosclaude.backendUrl` / `-Pwearosclaude.authToken` in `wearos/app/build.gradle.kts`. Watch user can override at runtime in settings.

### Mascot grid invariant

Every frame in `MascotFrames` **must be exactly 4 rows, 11 columns**. The constructor asserts this; violating it crashes the app on launch. See `docs/ASSETS.md`. When adding a new mood:

1. Add value to `Mood` in `shared/usage-contract.ts`.
2. Add value to `Mood` enum in `wearos/.../data/Mood.kt`.
3. Map it in `backend/src/lib/mood.ts → deriveMood`.
4. Add frames + tagline in `MascotFrames`.

### Mood policy

`deriveMood` defaults to `idle`. It only veers off that for **unambiguous** states: `offline` (probe + JSONL both failed) and `sleeping` (>12h since activity). The mascot is for personality, not as a meter — the dashboard already shows the raw percentage. Don't add mood transitions that turn the mascot into a redundant gauge.

### Wire contract sync

`shared/usage-contract.ts` is authoritative. `wearos/app/.../data/UsagePayload.kt` and `data/Mood.kt` mirror it. **Any change to one requires a matching change to the other**, or the Kotlin client will fail to deserialize and the watch will fall into the "stale/offline" state.

## Config surface (backend)

Only four knobs — everything else is derived from Anthropic headers or `.credentials.json`:

- `HOST` (default `0.0.0.0`), `PORT` (default `47823`)
- `CLAUDE_HOME` (default `~/.claude`)
- `OAUTH_PROBE_INTERVAL_MINUTES` (default `10`), `OAUTH_PROBE_DISABLED` (`1` to disable)
- `AUTH_TOKEN` (optional shared secret; clients send `x-wearos-claude-token`)

The backend has a hand-rolled `.env` loader in `config.ts` so it doesn't pull in `dotenv`. Don't add the dependency back.

Earlier versions exposed `*_TOKEN_BUDGET` / `*_WINDOW_MINUTES` knobs — those are intentionally **gone**. Don't reintroduce them; the probe headers or plan inference cover both cases.

## Notes for changes

- Type-check before sending a PR: `npm run typecheck` in `backend/`, `./gradlew :app:assembleDebug` in `wearos/`.
- `npm run probe` is the fastest way to confirm `/usage` still serializes cleanly without booting Fastify.
- TypeScript: strict mode, plain functions over classes, no DI frameworks.
- Compose for the Activity UI; ProtoLayout for the Tile. They share the same `MascotFrames` strings — keep it that way.
- The Wear OS app is a **standalone** Wear OS device target (`com.google.android.wearable.standalone = true`). No phone companion, no Data Layer API — the watch hits the backend directly over LAN HTTP. `usesCleartextTraffic="true"` is set deliberately.
