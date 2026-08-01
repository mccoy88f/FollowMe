import Fastify, { FastifyInstance } from 'fastify';
import cors from '@fastify/cors';
import { env } from './config/env';
import authRoutes from './routes/auth.routes';
import deviceRoutes, { pairDeviceRoute } from './routes/devices.routes';
import recordingsRoutes, { deviceRecordingsRoutes } from './routes/recordings.routes';

const MAX_CHUNK_BODY_BYTES = 25 * 1024 * 1024; // 25MB per uploaded chunk

export async function buildApp(): Promise<FastifyInstance> {
  const app = Fastify({
    logger: true,
    bodyLimit: MAX_CHUNK_BODY_BYTES,
  });

  await app.register(cors, { origin: env.corsOrigins });

  app.addContentTypeParser(
    /^audio\/|^video\/|^application\/octet-stream$/,
    { parseAs: 'buffer' },
    (_req, body, done) => done(null, body as Buffer)
  );

  app.get('/health', async () => ({ ok: true }));

  await app.register(authRoutes);
  await app.register(deviceRoutes);
  await app.register(pairDeviceRoute);
  await app.register(recordingsRoutes);
  await app.register(deviceRecordingsRoutes);

  return app;
}
