import type { FastifyInstance } from 'fastify';
import { v4 as uuidv4 } from 'uuid';
import { db } from '../db';
import { hashPassword, verifyPassword } from '../services/password.service';
import { signAccessToken, signRefreshToken, verifyRefreshToken } from '../services/token.service';
import type { UserRecord } from '../types';

const credentialsSchema = {
  body: {
    type: 'object',
    required: ['email', 'password'],
    properties: {
      email: { type: 'string', format: 'email' },
      password: { type: 'string', minLength: 8 },
    },
  },
};

export default async function authRoutes(app: FastifyInstance): Promise<void> {
  app.post('/api/auth/register', { schema: credentialsSchema }, async (req, reply) => {
    const { email, password } = req.body as { email: string; password: string };
    const normalizedEmail = email.trim().toLowerCase();

    const existing = await db<UserRecord>('users').where({ email: normalizedEmail }).first();
    if (existing) {
      return reply.code(409).send({ error: 'Email already registered' });
    }

    const passwordHash = await hashPassword(password);
    const id = uuidv4();
    await db<UserRecord>('users').insert({
      id,
      email: normalizedEmail,
      password_hash: passwordHash,
    });

    const accessToken = signAccessToken(id);
    const refreshToken = signRefreshToken(id, 0);
    return reply.code(201).send({ accessToken, refreshToken });
  });

  app.post('/api/auth/login', { schema: credentialsSchema }, async (req, reply) => {
    const { email, password } = req.body as { email: string; password: string };
    const normalizedEmail = email.trim().toLowerCase();

    const user = await db<UserRecord>('users').where({ email: normalizedEmail }).first();
    if (!user || !(await verifyPassword(password, user.password_hash))) {
      return reply.code(401).send({ error: 'Invalid email or password' });
    }

    const accessToken = signAccessToken(user.id);
    const refreshToken = signRefreshToken(user.id, user.refresh_token_version);
    return reply.send({ accessToken, refreshToken });
  });

  app.post('/api/auth/refresh', async (req, reply) => {
    const { refreshToken } = (req.body as { refreshToken?: string }) ?? {};
    if (!refreshToken) {
      return reply.code(400).send({ error: 'Missing refreshToken' });
    }

    let payload;
    try {
      payload = verifyRefreshToken(refreshToken);
    } catch {
      return reply.code(401).send({ error: 'Invalid or expired refresh token' });
    }

    const user = await db<UserRecord>('users').where({ id: payload.sub }).first();
    if (!user || user.refresh_token_version !== payload.ver) {
      return reply.code(401).send({ error: 'Refresh token has been revoked' });
    }

    const accessToken = signAccessToken(user.id);
    return reply.send({ accessToken });
  });

  // Revokes all outstanding refresh tokens for the current user (logout everywhere).
  app.post('/api/auth/logout-all', async (req, reply) => {
    const { refreshToken } = (req.body as { refreshToken?: string }) ?? {};
    if (!refreshToken) {
      return reply.code(400).send({ error: 'Missing refreshToken' });
    }
    let payload;
    try {
      payload = verifyRefreshToken(refreshToken);
    } catch {
      return reply.code(401).send({ error: 'Invalid or expired refresh token' });
    }
    await db<UserRecord>('users')
      .where({ id: payload.sub })
      .increment('refresh_token_version', 1);
    return reply.send({ ok: true });
  });
}
