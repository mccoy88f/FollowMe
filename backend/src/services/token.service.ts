import jwt from 'jsonwebtoken';
import { env } from '../config/env';
import type { AccessTokenPayload, RefreshTokenPayload, DeviceTokenPayload } from '../types';

export function signAccessToken(userId: string): string {
  const payload: AccessTokenPayload = { sub: userId, type: 'access' };
  return jwt.sign(payload, env.jwt.accessSecret, {
    expiresIn: env.jwt.accessExpiresIn,
  } as jwt.SignOptions);
}

export function signRefreshToken(userId: string, version: number): string {
  const payload: RefreshTokenPayload = { sub: userId, type: 'refresh', ver: version };
  return jwt.sign(payload, env.jwt.refreshSecret, {
    expiresIn: env.jwt.refreshExpiresIn,
  } as jwt.SignOptions);
}

export function signDeviceToken(deviceId: string, userId: string, version: number): string {
  const payload: DeviceTokenPayload = { sub: deviceId, userId, type: 'device', ver: version };
  // Device tokens are long-lived; they are revoked by bumping device_token_version,
  // not by expiry, since the camera device may stay paired for a long time.
  return jwt.sign(payload, env.jwt.deviceSecret, { expiresIn: '3650d' } as jwt.SignOptions);
}

export function verifyAccessToken(token: string): AccessTokenPayload {
  return jwt.verify(token, env.jwt.accessSecret) as AccessTokenPayload;
}

export function verifyRefreshToken(token: string): RefreshTokenPayload {
  return jwt.verify(token, env.jwt.refreshSecret) as RefreshTokenPayload;
}

export function verifyDeviceToken(token: string): DeviceTokenPayload {
  return jwt.verify(token, env.jwt.deviceSecret) as DeviceTokenPayload;
}
