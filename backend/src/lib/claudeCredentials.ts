import { promises as fs, existsSync } from 'node:fs';
import path from 'node:path';

import { config } from '../config.js';

/**
 * Shape of the file Claude Code writes to `~/.claude/.credentials.json` on
 * Linux. We only pin the fields we actually read; the file carries more
 * (refresh token, scopes, etc.) that we ignore.
 */
export interface ClaudeCredentials {
  accessToken: string;
  /** Epoch ms the token expires (when present). */
  expiresAt?: number;
  /** e.g. "pro" / "max" / "team" / "free" — Claude Code persists this string. */
  subscriptionType?: string;
  /** Anthropic's internal tier label, e.g. "tier_2_pro". */
  rateLimitTier?: string;
}

interface CredentialsFile {
  claudeAiOauth?: {
    accessToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    subscriptionType?: string;
    rateLimitTier?: string;
  };
}

/**
 * Read `~/.claude/.credentials.json` and return the parsed inner block.
 * Returns `null` when the file is missing or malformed. Callers decide
 * whether `null` means "no probe" or "no plan inference" — both are valid.
 */
export async function readClaudeCredentials(): Promise<ClaudeCredentials | null> {
  const credPath = path.join(config.claudeHome, '.credentials.json');
  if (!existsSync(credPath)) return null;
  try {
    const raw = await fs.readFile(credPath, 'utf8');
    const json = JSON.parse(raw) as CredentialsFile;
    const inner = json.claudeAiOauth;
    if (!inner?.accessToken) return null;
    return {
      accessToken: inner.accessToken,
      expiresAt: inner.expiresAt,
      subscriptionType: inner.subscriptionType,
      rateLimitTier: inner.rateLimitTier,
    };
  } catch {
    return null;
  }
}
