import type { FastifyRequest, FastifyReply } from 'fastify';
import { verifyAccessToken, verifyDeviceToken } from '../services/token.service';

declare module 'fastify' {
  interface FastifyRequest {
    userId?: string;
    deviceAuth?: { deviceId: string; userId: string; ver: number };
  }
}

export async function authenticateUser(req: FastifyRequest, reply: FastifyReply): Promise<void> {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    reply.code(401).send({ error: 'Missing bearer token' });
    return;
  }
  try {
    const payload = verifyAccessToken(header.slice('Bearer '.length));
    req.userId = payload.sub;
  } catch {
    reply.code(401).send({ error: 'Invalid or expired token' });
  }
}

export async function authenticateDevice(req: FastifyRequest, reply: FastifyReply): Promise<void> {
  const header = req.headers.authorization;
  if (!header?.startsWith('Bearer ')) {
    reply.code(401).send({ error: 'Missing bearer token' });
    return;
  }
  try {
    const payload = verifyDeviceToken(header.slice('Bearer '.length));
    req.deviceAuth = { deviceId: payload.sub, userId: payload.userId, ver: payload.ver };
  } catch {
    reply.code(401).send({ error: 'Invalid or expired device token' });
  }
}
