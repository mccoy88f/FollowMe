import type { Server as HttpServer } from 'http';
import { Server, Socket } from 'socket.io';
import { db } from '../db';
import { verifyDeviceToken, verifyAccessToken } from '../services/token.service';
import type { DeviceRecord } from '../types';
import {
  registerDeviceSocket,
  unregisterDeviceSocket,
  registerUserSocket,
  unregisterUserSocket,
  broadcastToUser,
} from './registry';

interface HandshakeAuth {
  token?: string;
  role?: 'device' | 'user';
}

export function setupWebSocket(httpServer: HttpServer): Server {
  const io = new Server(httpServer, {
    cors: { origin: '*' },
  });

  io.on('connection', (socket: Socket) => {
    void handleConnection(socket).catch((err) => {
      // eslint-disable-next-line no-console
      console.error('WebSocket connection error:', err);
      socket.disconnect(true);
    });
  });

  return io;
}

async function handleConnection(socket: Socket): Promise<void> {
  const auth = socket.handshake.auth as HandshakeAuth;

  if (auth.role === 'device') {
    await handleDeviceConnection(socket, auth.token);
  } else if (auth.role === 'user') {
    handleUserConnection(socket, auth.token);
  } else {
    socket.disconnect(true);
  }
}

async function handleDeviceConnection(socket: Socket, token?: string): Promise<void> {
  if (!token) {
    socket.disconnect(true);
    return;
  }

  let payload;
  try {
    payload = verifyDeviceToken(token);
  } catch {
    socket.disconnect(true);
    return;
  }

  const device = await db<DeviceRecord>('devices').where({ id: payload.sub }).first();
  if (!device || !device.paired || device.device_token_version !== payload.ver) {
    socket.disconnect(true);
    return;
  }

  const deviceId = device.id;
  registerDeviceSocket(deviceId, socket);
  await db<DeviceRecord>('devices')
    .where({ id: deviceId })
    .update({ status: 'online', last_seen_at: new Date() });
  broadcastToUser(device.user_id, 'device_status', { deviceId, status: 'online' });

  socket.on('status', (payload: unknown) => {
    broadcastToUser(device.user_id, 'device_recording_status', { deviceId, ...(payload as object) });
  });

  socket.on('heartbeat', () => {
    void db<DeviceRecord>('devices').where({ id: deviceId }).update({ last_seen_at: new Date() });
  });

  socket.on('disconnect', () => {
    unregisterDeviceSocket(deviceId);
    void db<DeviceRecord>('devices')
      .where({ id: deviceId })
      .update({ status: 'offline', last_seen_at: new Date() })
      .then(() => broadcastToUser(device.user_id, 'device_status', { deviceId, status: 'offline' }));
  });
}

function handleUserConnection(socket: Socket, token?: string): void {
  if (!token) {
    socket.disconnect(true);
    return;
  }

  let userId: string;
  try {
    userId = verifyAccessToken(token).sub;
  } catch {
    socket.disconnect(true);
    return;
  }

  registerUserSocket(userId, socket);
  socket.on('disconnect', () => unregisterUserSocket(userId, socket));
}
