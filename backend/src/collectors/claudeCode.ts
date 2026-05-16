import { promises as fs, existsSync } from 'node:fs';
import path from 'node:path';
import readline from 'node:readline';
import { createReadStream } from 'node:fs';

import { config } from '../config.js';
import { DAY, MINUTE } from '../lib/duration.js';

/** Anthropic's "unified-5h" bucket is always 5 hours. */
const CURRENT_WINDOW_MINUTES = 300;

/**
 * Output of a single collector pass — raw signals only, no formatting or
 * mood interpretation. Higher layers translate this into the wire payload.
 */
export interface ClaudeCodeSignals {
  /** Tokens (input + cache_creation + cache_read + output) inside rolling window. */
  tokensInWindow: number;
  /** Tokens within the last 7 days. */
  tokensWeekly: number;
  /** Number of assistant messages with usage metadata inside rolling window. */
  messagesInWindow: number;
  /** Same, but over the last 7 days. */
  messagesWeekly: number;
  /** Epoch millis of the most recent activity we observed, or 0 if none. */
  lastActivityAt: number;
  /** Sessions with activity inside the rolling window. */
  activeSessions: number;
  /** Where data was read from, for diagnostics. */
  sources: string[];
  /** Set when no usable signal was collected. */
  degraded: boolean;
}

interface UsageEntry {
  ts: number;
  tokens: number;
}

/**
 * Sum the usage block into a single "billable-equivalent" token count.
 *
 * Anthropic emits four token buckets that overlap: input_tokens (fresh prompt
 * tokens), cache_creation_input_tokens (writing to the prompt cache, billed at
 * 1.25x), cache_read_input_tokens (reading from cache, billed at 0.1x), and
 * output_tokens. The naive sum massively over-counts cache reads — a long
 * coding session can show 400% "usage" when the real billable surface is much
 * smaller. We approximate the billed quantity instead.
 */
function sumUsageTokens(usage: unknown): number {
  if (!usage || typeof usage !== 'object') return 0;
  const u = usage as Record<string, unknown>;
  const num = (k: string): number => {
    const v = u[k];
    return typeof v === 'number' && Number.isFinite(v) ? v : 0;
  };
  return (
    num('input_tokens') +
    Math.round(num('cache_creation_input_tokens') * 1.25) +
    Math.round(num('cache_read_input_tokens') * 0.1) +
    num('output_tokens')
  );
}

async function listJsonlSessions(claudeHome: string, since: number): Promise<string[]> {
  const projectsDir = path.join(claudeHome, 'projects');
  if (!existsSync(projectsDir)) return [];
  const out: string[] = [];
  const projectDirs = await fs.readdir(projectsDir, { withFileTypes: true });
  for (const dir of projectDirs) {
    if (!dir.isDirectory()) continue;
    const projectPath = path.join(projectsDir, dir.name);
    let entries: import('node:fs').Dirent[];
    try {
      entries = await fs.readdir(projectPath, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      if (!entry.isFile() || !entry.name.endsWith('.jsonl')) continue;
      const filePath = path.join(projectPath, entry.name);
      try {
        const stat = await fs.stat(filePath);
        if (stat.mtimeMs >= since) out.push(filePath);
      } catch {
        /* ignore */
      }
    }
  }
  return out;
}

async function streamUsageEntries(filePath: string, since: number): Promise<UsageEntry[]> {
  const rl = readline.createInterface({
    input: createReadStream(filePath, { encoding: 'utf8' }),
    crlfDelay: Infinity,
  });
  const entries: UsageEntry[] = [];
  for await (const line of rl) {
    if (!line || line.length < 16) continue;
    // Fast path: skip lines we know cannot carry usage to keep parsing cheap.
    if (line.indexOf('"usage"') < 0) continue;
    let parsed: unknown;
    try {
      parsed = JSON.parse(line);
    } catch {
      continue;
    }
    const obj = parsed as Record<string, unknown>;
    const ts = parseTimestamp(obj);
    if (!ts || ts < since) continue;
    const usage = extractUsage(obj);
    if (!usage) continue;
    const tokens = sumUsageTokens(usage);
    if (tokens > 0) entries.push({ ts, tokens });
  }
  return entries;
}

function parseTimestamp(line: Record<string, unknown>): number {
  const candidates = [line.timestamp, (line.message as { timestamp?: string } | undefined)?.timestamp];
  for (const c of candidates) {
    if (typeof c === 'string') {
      const t = Date.parse(c);
      if (Number.isFinite(t)) return t;
    }
  }
  return 0;
}

function extractUsage(line: Record<string, unknown>): unknown {
  const direct = line.usage;
  if (direct) return direct;
  const msg = line.message as { usage?: unknown } | undefined;
  if (msg?.usage) return msg.usage;
  return null;
}

/**
 * Cross-walks all on-disk Claude Code transcripts and aggregates token usage
 * into a rolling-window + 7-day view.
 */
export async function collectClaudeCodeSignals(now = Date.now()): Promise<ClaudeCodeSignals> {
  const home = config.claudeHome;
  const weekAgo = now - 7 * DAY;
  const windowStart = now - CURRENT_WINDOW_MINUTES * MINUTE;
  const sources: string[] = [];

  if (!existsSync(home)) {
    return emptySignals(['claude-home-missing']);
  }
  sources.push(home);

  const sessionFiles = await listJsonlSessions(home, weekAgo);
  if (sessionFiles.length === 0) {
    return emptySignals([...sources, 'no-recent-sessions']);
  }

  let tokensInWindow = 0;
  let tokensWeekly = 0;
  let messagesInWindow = 0;
  let messagesWeekly = 0;
  let lastActivityAt = 0;
  const sessionsTouched = new Set<string>();

  for (const file of sessionFiles) {
    const entries = await streamUsageEntries(file, weekAgo);
    if (entries.length === 0) continue;
    let touchedInWindow = false;
    for (const { ts, tokens } of entries) {
      tokensWeekly += tokens;
      messagesWeekly += 1;
      if (ts >= windowStart) {
        tokensInWindow += tokens;
        messagesInWindow += 1;
        touchedInWindow = true;
      }
      if (ts > lastActivityAt) lastActivityAt = ts;
    }
    if (touchedInWindow) sessionsTouched.add(file);
  }

  return {
    tokensInWindow,
    tokensWeekly,
    messagesInWindow,
    messagesWeekly,
    lastActivityAt,
    activeSessions: sessionsTouched.size,
    sources,
    degraded: false,
  };
}

function emptySignals(sources: string[]): ClaudeCodeSignals {
  return {
    tokensInWindow: 0,
    tokensWeekly: 0,
    messagesInWindow: 0,
    messagesWeekly: 0,
    lastActivityAt: 0,
    activeSessions: 0,
    sources,
    degraded: true,
  };
}
