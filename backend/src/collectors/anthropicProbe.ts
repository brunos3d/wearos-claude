import { readClaudeCredentials } from '../lib/claudeCredentials.js';

/**
 * Anthropic returns authoritative rate-limit headers on every `/v1/messages`
 * response when called with the Claude Code OAuth scope. A 1-token probe is
 * the cheapest legal way to read them.
 *
 *   anthropic-ratelimit-unified-5h-utilization    0..1 fraction of 5-hour bucket consumed
 *   anthropic-ratelimit-unified-5h-reset          epoch seconds the 5-hour bucket resets
 *   anthropic-ratelimit-unified-5h-status         allowed | allowed_warning | rejected
 *   anthropic-ratelimit-unified-7d-utilization    0..1 fraction of weekly bucket consumed
 *   anthropic-ratelimit-unified-7d-reset          epoch seconds the weekly bucket resets
 *   anthropic-ratelimit-unified-7d-status         allowed | allowed_warning | rejected
 *
 * Discovery credit: Clawdmeter (github:HermannBjorgvin/Clawdmeter) does the
 * same trick for an ESP32 desk display.
 */

export interface AnthropicProbeResult {
  /** 0..1 from header. */
  fiveHourUtilization: number;
  /** Epoch ms the 5-hour bucket resets. 0 if header missing. */
  fiveHourResetAt: number;
  fiveHourStatus: string;
  sevenDayUtilization: number;
  sevenDayResetAt: number;
  sevenDayStatus: string;
  /** "five_hour" | "seven_day" — whichever bucket Anthropic considers binding. */
  representativeClaim: string;
  fetchedAt: number;
  source: 'oauth-probe';
}

export class AnthropicProbeError extends Error {
  constructor(message: string, readonly kind: 'no-creds' | 'expired' | 'http' | 'parse' | 'unknown', readonly httpStatus?: number) {
    super(message);
  }
}

/**
 * Sends the minimum legal /v1/messages request and parses the
 * `anthropic-ratelimit-unified-*` headers from the response.
 *
 * IMPORTANT: each call counts as one message against the user's 5-hour
 * quota. Callers MUST cache. The usage orchestrator caches at
 * `OAUTH_PROBE_INTERVAL_MINUTES`.
 */
export async function probeAnthropicRateLimits(): Promise<AnthropicProbeResult> {
  const creds = await readClaudeCredentials();
  if (!creds) throw new AnthropicProbeError('no oauth credentials on disk', 'no-creds');
  if (creds.expiresAt && Number.isFinite(creds.expiresAt) && creds.expiresAt < Date.now()) {
    throw new AnthropicProbeError('oauth token expired', 'expired');
  }

  const body = JSON.stringify({
    model: 'claude-haiku-4-5-20251001',
    max_tokens: 1,
    messages: [{ role: 'user', content: 'hi' }],
  });

  let resp: Response;
  try {
    resp = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${creds.accessToken}`,
        'anthropic-version': '2023-06-01',
        'anthropic-beta': 'oauth-2025-04-20',
        'Content-Type': 'application/json',
        'User-Agent': 'claude-code/2.1.5',
      },
      body,
    });
  } catch (err) {
    throw new AnthropicProbeError(`network: ${(err as Error).message}`, 'http');
  }

  if (resp.status === 401 || resp.status === 403) {
    throw new AnthropicProbeError(`auth rejected (${resp.status})`, 'expired', resp.status);
  }
  // Even 429 still carries the ratelimit headers, so we read them before bailing.
  if (resp.status >= 500) {
    throw new AnthropicProbeError(`upstream ${resp.status}`, 'http', resp.status);
  }

  const h = resp.headers;
  const fiveHourUtilization = parseFraction(h.get('anthropic-ratelimit-unified-5h-utilization'));
  const fiveHourResetAt = parseEpochSec(h.get('anthropic-ratelimit-unified-5h-reset'));
  const fiveHourStatus = h.get('anthropic-ratelimit-unified-5h-status') ?? 'unknown';
  const sevenDayUtilization = parseFraction(h.get('anthropic-ratelimit-unified-7d-utilization'));
  const sevenDayResetAt = parseEpochSec(h.get('anthropic-ratelimit-unified-7d-reset'));
  const sevenDayStatus = h.get('anthropic-ratelimit-unified-7d-status') ?? 'unknown';
  const representativeClaim = h.get('anthropic-ratelimit-unified-representative-claim') ?? '';

  if (!Number.isFinite(fiveHourUtilization) && !Number.isFinite(sevenDayUtilization)) {
    throw new AnthropicProbeError('ratelimit headers missing', 'parse', resp.status);
  }

  // Drain body to free the connection cleanly (we don't need the contents).
  await resp.text().catch(() => undefined);

  return {
    fiveHourUtilization,
    fiveHourResetAt,
    fiveHourStatus,
    sevenDayUtilization,
    sevenDayResetAt,
    sevenDayStatus,
    representativeClaim,
    fetchedAt: Date.now(),
    source: 'oauth-probe',
  };
}

function parseFraction(value: string | null): number {
  if (!value) return NaN;
  const n = Number(value);
  return Number.isFinite(n) ? n : NaN;
}

function parseEpochSec(value: string | null): number {
  if (!value) return 0;
  const n = Number(value);
  return Number.isFinite(n) ? n * 1000 : 0;
}
