/**
 * Wire contract between the local backend and the Wear OS client.
 *
 * Keep this file authoritative — the Kotlin data classes in
 * `wearos/app/src/main/kotlin/.../data/UsagePayload.kt` mirror it.
 *
 * The project speaks Claude Code only. There is intentionally no provider
 * discriminator: every payload is Claude Code telemetry.
 */

export type Mood =
  | 'idle'
  | 'musing'
  | 'thinking'
  | 'coding'
  | 'compiling'
  | 'debugging'
  | 'overloaded'
  | 'sleeping'
  | 'offline';

export type Status = 'active' | 'idle' | 'stale' | 'offline' | 'error';

/** Where the headline percentages were ultimately read from. */
export type UsageSource = 'oauth-probe' | 'jsonl-fallback';

export interface UsagePayload {
  /** 0-100, fraction of the current rolling-window budget that has been used. */
  currentUsagePercent: number;
  /** 0-100, fraction of the rolling 7-day budget that has been used. */
  weeklyUsagePercent: number;
  /** Human friendly countdown until the current window resets, e.g. "1h22m". */
  currentResetIn: string;
  /** Same, for the weekly window, e.g. "6d8h". */
  weeklyResetIn: string;
  /** Epoch ms when the current rolling window resets (0 if unknown). */
  currentResetAt: number;
  /** Epoch ms when the weekly window resets (0 if unknown). */
  weeklyResetAt: number;
  /** Coarse liveness indicator. */
  status: Status;
  /** Mascot mood derived from usage + activity. */
  mood: Mood;
  /** Epoch millis when this payload was assembled. */
  lastUpdated: number;
  /** Optional extras, safe to ignore on older clients. */
  extras?: {
    source: UsageSource;
    currentWindowMinutes: number;
    sessionsActive: number;
    tokensInWindow: number;
    tokensWeekly: number;
    messagesInWindow: number;
    messagesWeekly: number;
    plan?: string;
  };
}

export const USAGE_CONTRACT_VERSION = 1;
