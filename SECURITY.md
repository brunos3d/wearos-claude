# Security policy

`wearos-claude` is a personal prototype, not a hardened product. With that
caveat, please report security issues responsibly.

## How to report

Email **bruno3dcontato@gmail.com** with:

- A description of the issue
- Steps to reproduce, if you have them
- Any logs or screenshots that help

I'll respond within a few days. Do **not** open a public GitHub issue for
security problems.

## In scope

- Auth bypass on the backend (`AUTH_TOKEN` not enforced when it should be)
- Credential disclosure (reading `~/.claude/.credentials.json` and leaking
  it over the wire)
- Wear OS app sending data anywhere other than the configured backend
- Path traversal / arbitrary file read in the collectors

## Out of scope

- Anyone on your LAN being able to reach the backend on port `47823` when
  you bind to `0.0.0.0`. That is the point. Use `AUTH_TOKEN` if you don't
  want that.
- The OAuth probe burning quota when enabled. That is documented.
- Bugs in Claude Code, Anthropic's API, or Wear OS itself.

## Rotating compromised secrets

If you accidentally commit your Anthropic OAuth token (lives in
`~/.claude/.credentials.json`), run `claude auth logout && claude auth login`
to get a fresh one.

If you commit the backend `AUTH_TOKEN`, generate a new one with
`openssl rand -hex 32`, drop it in `backend/.env`, and rebuild the APK
(`scripts/build-apk.sh`).
