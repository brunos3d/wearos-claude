# Contributing

Thanks for considering a contribution. `wearos-claude` is a small personal
project; the bar for changes is low, but I want to keep it small and focused.

## Before you start

- Open an issue first for anything bigger than a typo or a one-file tweak.
  We'll agree on direction so your PR doesn't get reworked.
- This project speaks **Claude Code only**. Multi-provider abstractions
  (OpenAI, generic LLMs, etc.) are out of scope on purpose — fork it if you
  need something else.
- The Wear OS surface is Wear OS 3 / API 30+ and is developed against a
  Galaxy Watch 6 Classic. Untested devices are welcome PR contexts but I
  won't gate releases on them.

## Setting up

See [`docs/SETUP.md`](docs/SETUP.md). The TL;DR:

```bash
cd backend && cp .env.example .env && npm install && npm run dev
# in another shell, from repo root:
scripts/build-apk.sh && scripts/install-watch.sh
```

## Local checks before sending a PR

```bash
# Backend
cd backend
npm run typecheck
npm run probe       # confirm /usage still serializes cleanly

# Wear OS
cd wearos
./gradlew :app:assembleDebug
```

## Style

- TypeScript: strict mode, prefer plain functions over classes, no DI
  frameworks. The backend deliberately does its own `.env` parsing to avoid
  the dependency.
- Kotlin: official style. Compose for Wear OS for the UI; ProtoLayout for
  the Tile.
- Comments: explain *why*, not *what*. Code that needs a comment to be
  understood usually needs a better name first.

## Commits and PRs

- Keep commits focused. Squash-merge is fine for tiny PRs.
- PR descriptions should say what changed and how to verify it (screenshot,
  `curl` command, manual repro steps on the watch).
- Don't include personal `.env` files, build artifacts, or screenshots
  containing your API keys in commits.

## Reporting bugs

Open an issue with:

- Watch model + Wear OS version
- Host OS + Node version
- The output of `npm run probe`
- Relevant `adb logcat` lines (`*:S WearOsClaude:V` is a good filter)

## Security

Don't open public issues for security problems. See
[`SECURITY.md`](SECURITY.md).
