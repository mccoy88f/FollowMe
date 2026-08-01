import type { FastifyInstance } from 'fastify';
import crypto from 'crypto';
import { v4 as uuidv4 } from 'uuid';
import { db } from '../db';
import { authenticateUser } from '../middleware/authenticate';
import { signDeviceToken } from '../services/token.service';
import { isDeviceOnline, isDeviceRecording, sendCommandToDevice } from '../ws/registry';
import type { DeviceRecord } from '../types';

const PAIRING_TOKEN_TTL_MS = 15 * 60 * 1000; // 15 minutes

function generatePairingToken(): string {
  return crypto.randomBytes(16).toString('hex');
}

export default async function deviceRoutes(app: FastifyInstance): Promise<void> {
  app.addHook('preHandler', authenticateUser);

  app.get('/api/devices', async (req, reply) => {
    const devices = await db<DeviceRecord>('devices')
      .where({ user_id: req.userId })
      .select('id', 'name', 'paired', 'status', 'last_seen_at', 'created_at');
    const withPresence = devices.map((d) => ({
      ...d,
      online: isDeviceOnline(d.id),
      recording: isDeviceRecording(d.id),
    }));
    return reply.send({ devices: withPresence });
  });

  app.post('/api/devices', async (req, reply) => {
    const { name } = (req.body as { name?: string }) ?? {};
    if (!name || typeof name !== 'string') {
      return reply.code(400).send({ error: 'Missing device name' });
    }

    const id = uuidv4();
    const pairingToken = generatePairingToken();
    const pairingTokenExpiresAt = new Date(Date.now() + PAIRING_TOKEN_TTL_MS);

    await db<DeviceRecord>('devices').insert({
      id,
      user_id: req.userId,
      name,
      pairing_token: pairingToken,
      pairing_token_expires_at: pairingTokenExpiresAt,
      paired: false,
      status: 'offline',
    });

    return reply.code(201).send({
      device: { id, name },
      pairingToken,
      pairingTokenExpiresAt,
    });
  });

  // Regenerates a pairing token, e.g. after the camera app was reinstalled.
  app.post('/api/devices/:id/regenerate-pairing-token', async (req, reply) => {
    const { id } = req.params as { id: string };
    const device = await db<DeviceRecord>('devices').where({ id, user_id: req.userId }).first();
    if (!device) return reply.code(404).send({ error: 'Device not found' });

    const pairingToken = generatePairingToken();
    const pairingTokenExpiresAt = new Date(Date.now() + PAIRING_TOKEN_TTL_MS);
    await db<DeviceRecord>('devices')
      .where({ id })
      .update({ pairing_token: pairingToken, pairing_token_expires_at: pairingTokenExpiresAt, paired: false });

    return reply.send({ pairingToken, pairingTokenExpiresAt });
  });

  app.delete('/api/devices/:id', async (req, reply) => {
    const { id } = req.params as { id: string };
    const deleted = await db<DeviceRecord>('devices').where({ id, user_id: req.userId }).delete();
    if (!deleted) return reply.code(404).send({ error: 'Device not found' });
    return reply.send({ ok: true });
  });

  app.post('/api/devices/:id/command', async (req, reply) => {
    const { id } = req.params as { id: string };
    const { action, type } = (req.body as { action?: string; type?: string }) ?? {};

    if (action !== 'start' && action !== 'stop') {
      return reply.code(400).send({ error: "action must be 'start' or 'stop'" });
    }
    if (action === 'start' && type !== 'audio' && type !== 'video' && type !== 'audio_video') {
      return reply.code(400).send({ error: "type must be 'audio', 'video' or 'audio_video'" });
    }

    const device = await db<DeviceRecord>('devices').where({ id, user_id: req.userId }).first();
    if (!device) return reply.code(404).send({ error: 'Device not found' });

    const delivered = sendCommandToDevice(id, {
      action,
      type: type as 'audio' | 'video' | 'audio_video' | undefined,
    });

    if (!delivered) {
      // TODO(phase 3): fall back to FCM wake push using device.fcm_token
      // once the camera app registers for push notifications.
      return reply.code(202).send({ delivered: false, reason: 'device_offline' });
    }

    return reply.send({ delivered: true });
  });
}

// Called by the camera app during first-run setup: exchanges a short-lived
// pairing token (shown/entered by the owner) for a long-lived device token.
export async function pairDeviceRoute(app: FastifyInstance): Promise<void> {
  app.post('/api/devices/pair', async (req, reply) => {
    const { pairingToken } = (req.body as { pairingToken?: string }) ?? {};
    if (!pairingToken) return reply.code(400).send({ error: 'Missing pairingToken' });

    const device = await db<DeviceRecord>('devices').where({ pairing_token: pairingToken }).first();
    if (!device) return reply.code(404).send({ error: 'Invalid pairing token' });
    if (!device.pairing_token_expires_at || device.pairing_token_expires_at.getTime() < Date.now()) {
      return reply.code(410).send({ error: 'Pairing token expired' });
    }

    await db<DeviceRecord>('devices')
      .where({ id: device.id })
      .update({ paired: true, pairing_token: null, pairing_token_expires_at: null });

    const deviceToken = signDeviceToken(device.id, device.user_id, device.device_token_version);
    return reply.send({ deviceToken, deviceId: device.id, name: device.name });
  });
}
