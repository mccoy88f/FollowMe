import { buildApp } from './app';
import { setupWebSocket } from './ws';
import { env } from './config/env';

async function main(): Promise<void> {
  const app = await buildApp();
  setupWebSocket(app.server);

  await app.listen({ port: env.port, host: '0.0.0.0' });
}

main().catch((err) => {
  // eslint-disable-next-line no-console
  console.error('Fatal startup error:', err);
  process.exit(1);
});
