/**
 * `npm run probe` — one-shot dump of what `/usage` would currently return,
 * without standing up the Fastify server. Handy when iterating on collectors
 * or tweaking budgets.
 */
import { collectUsage } from '../usage.js';
import { config } from '../config.js';

async function main() {
  const payload = await collectUsage(Date.now());
  process.stdout.write(JSON.stringify({ config, payload }, null, 2) + '\n');
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
