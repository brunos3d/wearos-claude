/**
 * Tiny utilities for shaping durations into the retro `1h22m` / `6d8h` form
 * the watch tile renders.
 */

export const SECOND = 1_000;
export const MINUTE = 60 * SECOND;
export const HOUR = 60 * MINUTE;
export const DAY = 24 * HOUR;
export const WEEK = 7 * DAY;

export function formatShort(ms: number): string {
  if (!Number.isFinite(ms) || ms <= 0) return '0m';
  if (ms < HOUR) {
    const m = Math.max(1, Math.round(ms / MINUTE));
    return `${m}m`;
  }
  if (ms < DAY) {
    const h = Math.floor(ms / HOUR);
    const m = Math.floor((ms % HOUR) / MINUTE);
    return m === 0 ? `${h}h` : `${h}h${m}m`;
  }
  const d = Math.floor(ms / DAY);
  const h = Math.floor((ms % DAY) / HOUR);
  return h === 0 ? `${d}d` : `${d}d${h}h`;
}

export function clampPercent(value: number): number {
  if (!Number.isFinite(value) || value <= 0) return 0;
  if (value >= 100) return 100;
  return Math.round(value);
}
