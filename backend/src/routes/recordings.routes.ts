import type { FastifyInstance } from 'fastify';
import fs from 'fs';
import { v4 as uuidv4 } from 'uuid';
import { db } from '../db';
import { authenticateUser, authenticateDevice } from '../middleware/authenticate';
import {
  recordingFilePath,
  ensureDeviceDir,
  appendChunk,
  fileExists,
  deleteFile,
} from '../services/storage.service';
import type { DeviceRecord, RecordingRecord, RecordingType } from '../types';

const RECORDING_TYPES: RecordingType[] = ['audio', 'video', 'audio_video'];

async function ownedRecording(recordingId: string, userId: string) {
  return db<RecordingRecord & { device_user_id: string }>('recordings')
    .join('devices', 'devices.id', 'recordings.device_id')
    .where('recordings.id', recordingId)
    .andWhere('devices.user_id', userId)
    .select('recordings.*')
    .first();
}

// Routes called by the Android controller app (user-authenticated).
export default async function recordingsRoutes(app: FastifyInstance): Promise<void> {
  app.addHook('preHandler', authenticateUser);

  app.get('/api/recordings', async (req, reply) => {
    const { deviceId, limit = '20', offset = '0' } = req.query as Record<string, string>;
    if (!deviceId) return reply.code(400).send({ error: 'Missing deviceId query param' });

    const device = await db<DeviceRecord>('devices').where({ id: deviceId, user_id: req.userId }).first();
    if (!device) return reply.code(404).send({ error: 'Device not found' });

    const recordings = await db<RecordingRecord>('recordings')
      .where({ device_id: deviceId })
      .orderBy('started_at', 'desc')
      .limit(Math.min(parseInt(limit, 10) || 20, 100))
      .offset(parseInt(offset, 10) || 0);

    return reply.send({ recordings });
  });

  app.get('/api/recordings/:id', async (req, reply) => {
    const { id } = req.params as { id: string };
    const recording = await ownedRecording(id, req.userId!);
    if (!recording) return reply.code(404).send({ error: 'Recording not found' });
    return reply.send({ recording });
  });

  app.get('/api/recordings/:id/download', async (req, reply) => {
    const { id } = req.params as { id: string };
    const recording = await ownedRecording(id, req.userId!);
    if (!recording) return reply.code(404).send({ error: 'Recording not found' });
    if (!fileExists(recording.file_path)) {
      return reply.code(404).send({ error: 'Recording file not available' });
    }

    const stat = fs.statSync(recording.file_path);
    reply.header('Content-Length', stat.size);
    reply.header('Content-Disposition', `attachment; filename="${id}${extname(recording.file_path)}"`);
    reply.type(recording.type === 'audio' ? 'audio/aac' : 'video/mp4');
    return reply.send(fs.createReadStream(recording.file_path));
  });

  app.delete('/api/recordings/:id', async (req, reply) => {
    const { id } = req.params as { id: string };
    const recording = await ownedRecording(id, req.userId!);
    if (!recording) return reply.code(404).send({ error: 'Recording not found' });

    deleteFile(recording.file_path);
    await db<RecordingRecord>('recordings').where({ id }).delete();
    return reply.send({ ok: true });
  });
}

function extname(filePath: string): string {
  const idx = filePath.lastIndexOf('.');
  return idx === -1 ? '' : filePath.slice(idx);
}

// Routes called by the Android camera-device app (device-token authenticated).
export async function deviceRecordingsRoutes(app: FastifyInstance): Promise<void> {
  app.addHook('preHandler', authenticateDevice);

  app.post('/api/device/recordings', async (req, reply) => {
    const { type } = (req.body as { type?: string }) ?? {};
    if (!type || !RECORDING_TYPES.includes(type as RecordingType)) {
      return reply.code(400).send({ error: `type must be one of ${RECORDING_TYPES.join(', ')}` });
    }

    const deviceId = req.deviceAuth!.deviceId;
    const id = uuidv4();
    ensureDeviceDir(deviceId);
    const filePath = recordingFilePath(deviceId, id, type as RecordingType);

    await db<RecordingRecord>('recordings').insert({
      id,
      device_id: deviceId,
      type: type as RecordingType,
      status: 'recording',
      file_path: filePath,
      bytes_received: 0,
    });

    return reply.code(201).send({ recordingId: id });
  });

  app.post('/api/device/recordings/:id/chunk', async (req, reply) => {
    const { id } = req.params as { id: string };
    const deviceId = req.deviceAuth!.deviceId;

    const recording = await db<RecordingRecord>('recordings')
      .where({ id, device_id: deviceId, status: 'recording' })
      .first();
    if (!recording) return reply.code(404).send({ error: 'Recording not found or not active' });

    const chunk = req.body as Buffer;
    if (!Buffer.isBuffer(chunk) || chunk.length === 0) {
      return reply.code(400).send({ error: 'Expected a non-empty binary chunk body' });
    }

    const newSize = appendChunk(recording.file_path, chunk);
    await db<RecordingRecord>('recordings').where({ id }).update({ bytes_received: newSize });

    return reply.send({ bytesReceived: newSize });
  });

  app.post('/api/device/recordings/:id/complete', async (req, reply) => {
    const { id } = req.params as { id: string };
    const deviceId = req.deviceAuth!.deviceId;
    const { durationSeconds } = (req.body as { durationSeconds?: number }) ?? {};

    const recording = await db<RecordingRecord>('recordings')
      .where({ id, device_id: deviceId })
      .first();
    if (!recording) return reply.code(404).send({ error: 'Recording not found' });

    await db<RecordingRecord>('recordings')
      .where({ id })
      .update({
        status: 'completed',
        ended_at: new Date(),
        duration_seconds: durationSeconds ?? null,
      });

    return reply.send({ ok: true });
  });

  app.post('/api/device/recordings/:id/fail', async (req, reply) => {
    const { id } = req.params as { id: string };
    const deviceId = req.deviceAuth!.deviceId;

    const updated = await db<RecordingRecord>('recordings')
      .where({ id, device_id: deviceId })
      .update({ status: 'failed', ended_at: new Date() });
    if (!updated) return reply.code(404).send({ error: 'Recording not found' });

    return reply.send({ ok: true });
  });
}
