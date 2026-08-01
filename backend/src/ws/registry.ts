import type { Socket } from 'socket.io';

// Tracks live socket connections so REST route handlers can push realtime
// events without depending on the socket.io server setup directly.
const deviceSockets = new Map<string, Socket>(); // deviceId -> socket
const userSockets = new Map<string, Set<Socket>>(); // userId -> sockets (controller app instances)

// Last known recording state per device, so a controller opening/refreshing
// the device list learns "is it recording right now" immediately via
// GET /api/devices instead of only from a live 'status' WebSocket event it
// may have missed (e.g. app was closed, or connected after the event).
const deviceRecordingState = new Map<string, boolean>();

export function registerDeviceSocket(deviceId: string, socket: Socket): void {
  deviceSockets.set(deviceId, socket);
}

export function unregisterDeviceSocket(deviceId: string): void {
  deviceSockets.delete(deviceId);
  deviceRecordingState.delete(deviceId);
}

export function setDeviceRecording(deviceId: string, recording: boolean): void {
  deviceRecordingState.set(deviceId, recording);
}

export function isDeviceRecording(deviceId: string): boolean {
  return deviceRecordingState.get(deviceId) ?? false;
}

export function registerUserSocket(userId: string, socket: Socket): void {
  const set = userSockets.get(userId) ?? new Set<Socket>();
  set.add(socket);
  userSockets.set(userId, set);
}

export function unregisterUserSocket(userId: string, socket: Socket): void {
  const set = userSockets.get(userId);
  if (!set) return;
  set.delete(socket);
  if (set.size === 0) userSockets.delete(userId);
}

export function isDeviceOnline(deviceId: string): boolean {
  return deviceSockets.has(deviceId);
}

export interface DeviceCommand {
  action: 'start' | 'stop';
  type?: 'audio' | 'video' | 'audio_video';
  recordingId?: string;
}

export function sendCommandToDevice(deviceId: string, command: DeviceCommand): boolean {
  const socket = deviceSockets.get(deviceId);
  if (!socket) return false;
  socket.emit('command', command);
  return true;
}

export function broadcastToUser(userId: string, event: string, payload: unknown): void {
  const sockets = userSockets.get(userId);
  if (!sockets) return;
  for (const socket of sockets) {
    socket.emit(event, payload);
  }
}
