import type { Mood, Status } from '../../../shared/usage-contract.js';

interface MoodInput {
  currentUsagePercent: number;
  weeklyUsagePercent: number;
  /** Minutes since the most recent user/assistant message we could find. */
  minutesSinceActivity: number;
  /** Whether any session has produced events inside the rolling window. */
  hasRecentActivity: boolean;
  /** True when no provider data was usable. */
  degraded: boolean;
}

/**
 * Mood is the mascot's resting expression — playful, not informational. The
 * dashboard already renders the raw usage percent, reset times, and a
 * progress bar, so the mascot doesn't need to double as a meter. Default to
 * `idle` and only veer off that for unambiguous states:
 *
 *   - `offline`  → backend lost its data source entirely
 *   - `sleeping` → user has been away for half a day
 *
 * The user can still cycle through every mood manually by tapping the mascot
 * on the watch.
 */
export function deriveMood(input: MoodInput): { mood: Mood; status: Status } {
  if (input.degraded) return { mood: 'offline', status: 'error' };
  if (!input.hasRecentActivity && input.minutesSinceActivity > 12 * 60) {
    return { mood: 'sleeping', status: 'idle' };
  }
  return { mood: 'idle', status: input.hasRecentActivity ? 'active' : 'idle' };
}
