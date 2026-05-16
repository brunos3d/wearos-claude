import type { UsagePayload } from '../../shared/usage-contract.js';
import { config } from './config.js';
import { collectClaudeCodeSignals } from './collectors/claudeCode.js';
import {
  AnthropicProbeError,
  probeAnthropicRateLimits,
  type AnthropicProbeResult,
} from './collectors/anthropicProbe.js';
import { readClaudeCredentials } from './lib/claudeCredentials.js';
import { inferPlanProfile, type PlanProfile } from './lib/planProfile.js';
import { clampPercent, formatShort, MINUTE } from './lib/duration.js';
import { deriveMood } from './lib/mood.js';

/**
 * Builds the `/usage` payload from two sources, in order:
 *
 *   1. Anthropic OAuth probe — authoritative 5h/7d utilization headers,
 *      cached so each user only burns a handful of probe messages per hour.
 *      The headers come pre-normalized 0..1 against the user's plan, so we
 *      never need to know what the plan limits actually are.
 *   2. Local Claude Code JSONL aggregation — used both for "is the user
 *      coding right now?" (mood signal) and as a graceful fallback when the
 *      probe is unavailable. In fallback mode the percentage is computed
 *      against a plan-aware budget inferred from `.credentials.json`.
 *
 * No abstract "provider" layer: this project speaks Claude Code only.
 */

interface ProbeCache {
  result: AnthropicProbeResult;
  expiresAt: number;
}

let probeCache: ProbeCache | null = null;
let lastProbeError: string | null = null;

async function readProbe(now: number): Promise<AnthropicProbeResult | null> {
  if (config.oauthProbeDisabled) return null;
  if (probeCache && probeCache.expiresAt > now) return probeCache.result;
  try {
    const result = await probeAnthropicRateLimits();
    probeCache = {
      result,
      expiresAt: now + config.oauthProbeIntervalMinutes * MINUTE,
    };
    lastProbeError = null;
    return result;
  } catch (err) {
    lastProbeError = err instanceof AnthropicProbeError ? `${err.kind}: ${err.message}` : String(err);
    return null;
  }
}

export async function collectUsage(now = Date.now()): Promise<UsagePayload> {
  const [probe, jsonl] = await Promise.all([
    readProbe(now),
    collectClaudeCodeSignals(now),
  ]);

  const minutesSinceActivity = jsonl.lastActivityAt
    ? Math.max(0, (now - jsonl.lastActivityAt) / MINUTE)
    : Number.POSITIVE_INFINITY;

  if (probe) {
    return buildFromProbe(probe, jsonl, minutesSinceActivity, now);
  }
  // Plan inference is only consulted by the fallback path — the probe path
  // already has accurate percentages.
  const plan = inferPlanProfile(await readClaudeCredentials());
  return buildFromJsonl(jsonl, plan, minutesSinceActivity, now);
}

function buildFromProbe(
  probe: AnthropicProbeResult,
  jsonl: Awaited<ReturnType<typeof collectClaudeCodeSignals>>,
  minutesSinceActivity: number,
  now: number,
): UsagePayload {
  const currentUsagePercent = clampPercent(probe.fiveHourUtilization * 100);
  const weeklyUsagePercent = clampPercent(probe.sevenDayUtilization * 100);
  const currentResetIn = probe.fiveHourResetAt
    ? formatShort(Math.max(0, probe.fiveHourResetAt - now))
    : formatShort(5 * 60 * MINUTE);
  const weeklyResetIn = probe.sevenDayResetAt
    ? formatShort(Math.max(0, probe.sevenDayResetAt - now))
    : formatShort(7 * 24 * 60 * MINUTE);
  const currentResetAt = probe.fiveHourResetAt || (now + 5 * 60 * MINUTE);
  const weeklyResetAt = probe.sevenDayResetAt || (now + 7 * 24 * 60 * MINUTE);

  const { mood, status } = deriveMood({
    currentUsagePercent,
    weeklyUsagePercent,
    minutesSinceActivity,
    hasRecentActivity: jsonl.messagesInWindow > 0,
    degraded: probe.fiveHourStatus === 'rejected',
  });

  return {
    currentUsagePercent,
    weeklyUsagePercent,
    currentResetIn,
    weeklyResetIn,
    currentResetAt,
    weeklyResetAt,
    status,
    mood,
    lastUpdated: now,
    extras: {
      source: 'oauth-probe',
      currentWindowMinutes: 300,
      sessionsActive: jsonl.activeSessions,
      tokensInWindow: jsonl.tokensInWindow,
      tokensWeekly: jsonl.tokensWeekly,
      messagesInWindow: jsonl.messagesInWindow,
      messagesWeekly: jsonl.messagesWeekly,
      plan: `${probe.representativeClaim || 'live'}/${probe.fiveHourStatus}`,
    },
  };
}

function buildFromJsonl(
  jsonl: Awaited<ReturnType<typeof collectClaudeCodeSignals>>,
  plan: PlanProfile,
  minutesSinceActivity: number,
  now: number,
): UsagePayload {
  const currentUsagePercent = clampPercent((jsonl.tokensInWindow / plan.currentTokenBudget) * 100);
  const weeklyUsagePercent = clampPercent((jsonl.tokensWeekly / plan.weeklyTokenBudget) * 100);
  const windowMs = plan.currentWindowMinutes * MINUTE;
  const currentResetAtRaw = jsonl.lastActivityAt
    ? jsonl.lastActivityAt + windowMs
    : now + windowMs;
  const weeklyResetAtRaw = now + weeklyResetCountdown(now);
  const currentResetIn = formatShort(Math.max(0, currentResetAtRaw - now));
  const weeklyResetIn = formatShort(Math.max(0, weeklyResetAtRaw - now));

  const { mood, status } = deriveMood({
    currentUsagePercent,
    weeklyUsagePercent,
    minutesSinceActivity,
    hasRecentActivity: jsonl.messagesInWindow > 0,
    degraded: jsonl.degraded,
  });

  return {
    currentUsagePercent,
    weeklyUsagePercent,
    currentResetIn,
    weeklyResetIn,
    currentResetAt: currentResetAtRaw,
    weeklyResetAt: weeklyResetAtRaw,
    status,
    mood,
    lastUpdated: now,
    extras: {
      source: 'jsonl-fallback',
      currentWindowMinutes: plan.currentWindowMinutes,
      sessionsActive: jsonl.activeSessions,
      tokensInWindow: jsonl.tokensInWindow,
      tokensWeekly: jsonl.tokensWeekly,
      messagesInWindow: jsonl.messagesInWindow,
      messagesWeekly: jsonl.messagesWeekly,
      plan: lastProbeError
        ? `fallback:${plan.label}:${lastProbeError}`
        : `fallback:${plan.label}`,
    },
  };
}

function weeklyResetCountdown(now: number): number {
  const d = new Date(now);
  const day = d.getUTCDay();
  const daysUntilMonday = (8 - day) % 7 || 7;
  const reset = Date.UTC(
    d.getUTCFullYear(),
    d.getUTCMonth(),
    d.getUTCDate() + daysUntilMonday,
    0,
    0,
    0,
  );
  return reset - now;
}
