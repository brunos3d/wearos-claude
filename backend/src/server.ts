import Fastify from 'fastify';
import cors from '@fastify/cors';

import { config } from './config.js';
import { usageRoutes } from './routes/usage.js';

async function main() {
  const app = Fastify({
    logger: {
      level: process.env.LOG_LEVEL ?? 'info',
      transport: process.stdout.isTTY
        ? { target: 'pino-pretty', options: { colorize: true, translateTime: 'HH:MM:ss' } }
        : undefined,
    },
    disableRequestLogging: false,
  });

  await app.register(cors, { origin: true });
  await app.register(usageRoutes);

  try {
    const address = await app.listen({ host: config.host, port: config.port });
    app.log.info(
      {
        oauthProbe: config.oauthProbeDisabled ? 'disabled' : `every ${config.oauthProbeIntervalMinutes}m`,
        claudeHome: config.claudeHome,
        authRequired: Boolean(config.authToken),
      },
      `wearos-claude backend listening on ${address}`,
    );
  } catch (err) {
    app.log.error(err, 'failed to start');
    process.exit(1);
  }

  const shutdown = async (signal: string) => {
    app.log.info({ signal }, 'shutting down');
    await app.close();
    process.exit(0);
  };
  process.on('SIGINT', () => void shutdown('SIGINT'));
  process.on('SIGTERM', () => void shutdown('SIGTERM'));
}

void main();
