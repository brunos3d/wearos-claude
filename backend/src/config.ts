import os from 'node:os';
import path from 'node:path';
import { readFileSync, existsSync } from 'node:fs';

/**
 * Hand-rolled `.env` loader so we don't pull in a dependency just to read a
 * handful of key=value lines. Looks for `backend/.env` and applies any keys
 * that are not already present in `process.env`.
 */
function loadDotenv() {
  const envPath = path.resolve(process.cwd(), '.env');
  if (!existsSync(envPath)) return;
  for (const raw of readFileSync(envPath, 'utf8').split('\n')) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const eq = line.indexOf('=');
    if (eq < 0) continue;
    const key = line.slice(0, eq).trim();
    let value = line.slice(eq + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    if (process.env[key] === undefined) process.env[key] = value;
  }
}
loadDotenv();

const num = (key: string, fallback: number): number => {
  const raw = process.env[key];
  if (!raw) return fallback;
  const v = Number(raw);
  return Number.isFinite(v) ? v : fallback;
};

const str = (key: string, fallback: string): string => process.env[key]?.trim() || fallback;
const bool = (key: string): boolean => {
  const v = (process.env[key] ?? '').trim().toLowerCase();
  return v === '1' || v === 'true' || v === 'yes';
};

/**
 * The backend has exactly four knobs the user can turn. Everything else —
 * window length, plan limits, weekly bucket size — is derived from
 * Anthropic's ratelimit headers and the local Claude Code credentials.
 */
export const config = {
  /** Address the Fastify server binds to. 0.0.0.0 = reachable on the LAN. */
  host: str('HOST', '0.0.0.0'),
  /** TCP port for the HTTP API. */
  port: num('PORT', 47823),

  /** Where Claude Code keeps its OAuth credentials and JSONL transcripts. */
  claudeHome: str('CLAUDE_HOME', path.join(os.homedir(), '.claude')),

  /**
   * How often the backend is allowed to hit `POST /v1/messages` for the
   * authoritative 5h/7d utilization headers. Each probe burns one message
   * from the user's quota, so the default is intentionally conservative.
   */
  oauthProbeIntervalMinutes: num('OAUTH_PROBE_INTERVAL_MINUTES', 10),
  /** Disable the live probe entirely (falls back to JSONL token estimate). */
  oauthProbeDisabled: bool('OAUTH_PROBE_DISABLED'),

  /** Optional shared secret. When set, /usage requires `x-wearos-claude-token`. */
  authToken: str('AUTH_TOKEN', ''),
} as const;

export type Config = typeof config;
