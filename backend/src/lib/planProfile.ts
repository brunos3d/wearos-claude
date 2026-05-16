import type { ClaudeCredentials } from './claudeCredentials.js';

/**
 * Plan-shaped defaults used by the JSONL fallback path.
 *
 * The OAuth probe path doesn't need these — Anthropic's
 * `anthropic-ratelimit-unified-*-utilization` headers come back already
 * normalized 0..1 against the user's actual plan, so the server does the
 * math for us.
 *
 * These numbers only matter when the live probe is disabled or failing,
 * and even then they're approximate by design. We expose them as a single
 * inference function so the rest of the code never has to think about
 * which plan the user is on.
 */
export interface PlanProfile {
  /** Human-readable label, e.g. "pro", "max", "team", "unknown". */
  label: string;
  /** Length of the rolling "current usage" window. Anthropic uses 5 hours. */
  currentWindowMinutes: number;
  /** Soft billable-token budget used as the JSONL fallback denominator. */
  currentTokenBudget: number;
  /** Same, for the 7-day weekly bucket. */
  weeklyTokenBudget: number;
}

// Anthropic's unified-5h bucket is always 5 hours; mirroring that here keeps
// the JSONL fallback aligned with what the probe would have reported.
const WINDOW = 300;

/**
 * Best-guess profiles for the public Claude Code subscription tiers. Numbers
 * are rough — the real ones live server-side and are exposed through the
 * ratelimit headers. We only use these when those headers are unavailable.
 */
const FREE: PlanProfile = { label: 'free', currentWindowMinutes: WINDOW, currentTokenBudget: 100_000, weeklyTokenBudget: 1_500_000 };
const PRO: PlanProfile = { label: 'pro', currentWindowMinutes: WINDOW, currentTokenBudget: 900_000, weeklyTokenBudget: 12_000_000 };
const MAX: PlanProfile = { label: 'max', currentWindowMinutes: WINDOW, currentTokenBudget: 3_000_000, weeklyTokenBudget: 40_000_000 };
const TEAM: PlanProfile = { label: 'team', currentWindowMinutes: WINDOW, currentTokenBudget: 1_500_000, weeklyTokenBudget: 20_000_000 };
const UNKNOWN: PlanProfile = { ...PRO, label: 'unknown' };

/**
 * Infer a plan profile from whatever Claude Code persisted alongside the
 * OAuth token. Falls back to a "Pro-like" default — that's the most common
 * Claude Code subscription in the wild.
 */
export function inferPlanProfile(creds: ClaudeCredentials | null): PlanProfile {
  if (!creds) return UNKNOWN;
  const haystack = `${creds.subscriptionType ?? ''} ${creds.rateLimitTier ?? ''}`.toLowerCase();
  if (haystack.includes('max')) return MAX;
  if (haystack.includes('team') || haystack.includes('enterprise')) return TEAM;
  if (haystack.includes('pro')) return PRO;
  if (haystack.includes('free')) return FREE;
  return UNKNOWN;
}
