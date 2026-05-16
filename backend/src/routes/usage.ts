import type { FastifyInstance } from 'fastify';
import type { UsagePayload } from '../../../shared/usage-contract.js';
import { config } from '../config.js';
import { collectUsage } from '../usage.js';

const CACHE_TTL_MS = 15_000;

interface CacheSlot {
  payload: UsagePayload;
  generatedAt: number;
}

let cache: CacheSlot | null = null;

async function getUsage(force: boolean): Promise<UsagePayload> {
  const now = Date.now();
  if (!force && cache && now - cache.generatedAt < CACHE_TTL_MS) {
    return cache.payload;
  }
  const payload = await collectUsage(now);
  cache = { payload, generatedAt: now };
  return payload;
}

/**
 * Endpoints the watch needs to reach without proving identity. They exist
 * specifically so the device can diagnose "is the server even there?"
 * separately from "did I send the right token?".
 */
const OPEN_PATHS = new Set(['/health', '/ping', '/']);

export async function usageRoutes(app: FastifyInstance) {
  app.addHook('onRequest', async (req, reply) => {
    if (!config.authToken) return;
    if (OPEN_PATHS.has(req.url.split('?')[0] ?? req.url)) return;
    const token = req.headers['x-wearos-claude-token'];
    if (token !== config.authToken) {
      reply.code(401).send({ error: 'unauthorized', authRequired: true });
    }
  });

  app.get('/usage', async (req) => {
    const force = (req.query as Record<string, string> | undefined)?.fresh === '1';
    return getUsage(force);
  });

  app.get('/usage/fresh', async () => getUsage(true));

  app.get('/health', async () => ({
    ok: true,
    uptime: Math.round(process.uptime()),
    version: '0.1.0',
    authRequired: Boolean(config.authToken),
  }));

  /**
   * Lightweight liveness probe the Wear OS app uses to confirm LAN
   * reachability before attempting an authenticated /usage call.
   */
  app.get('/ping', async () => ({
    pong: true,
    ts: Date.now(),
    authRequired: Boolean(config.authToken),
  }));

  app.get('/', async () => ({
    name: 'wearos-claude',
    endpoints: ['/usage', '/usage/fresh', '/health', '/ping'],
    authRequired: Boolean(config.authToken),
  }));
}
