import fs from 'fs';
import path from 'path';
import { env } from '../config/env';
import type { RecordingType } from '../types';

const EXTENSION_BY_TYPE: Record<RecordingType, string> = {
  audio: 'aac',
  video: 'mp4',
  audio_video: 'mp4',
};

export function extensionForType(type: RecordingType): string {
  return EXTENSION_BY_TYPE[type];
}

export function recordingFilePath(deviceId: string, recordingId: string, type: RecordingType): string {
  const dir = path.join(env.storagePath, deviceId);
  const fileName = `${recordingId}.${extensionForType(type)}`;
  return path.join(dir, fileName);
}

export function ensureDeviceDir(deviceId: string): void {
  const dir = path.join(env.storagePath, deviceId);
  fs.mkdirSync(dir, { recursive: true });
}

export function appendChunk(filePath: string, chunk: Buffer): number {
  fs.appendFileSync(filePath, chunk);
  return fs.statSync(filePath).size;
}

export function fileExists(filePath: string): boolean {
  return fs.existsSync(filePath);
}

export function fileSize(filePath: string): number {
  return fs.existsSync(filePath) ? fs.statSync(filePath).size : 0;
}

export function deleteFile(filePath: string): void {
  if (fs.existsSync(filePath)) {
    fs.unlinkSync(filePath);
  }
}
