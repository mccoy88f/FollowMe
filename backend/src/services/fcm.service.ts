import { env } from '../config/env';

/**
 * Wakes a camera device that is not currently connected over WebSocket
 * (e.g. killed in background by Android Doze/battery optimization) by
 * sending a high-priority, data-only FCM push. The device app must be
 * registered for FCM and its push token stored server-side (add an
 * `fcm_token` column to `devices` when implementing this in phase 3).
 *
 * Not wired to a real Firebase project yet — set FCM_SERVER_KEY and
 * replace this with an actual call to the FCM HTTP v1 API before relying
 * on it in production. Until then it is a no-op and commands are only
 * delivered while the device is connected over WebSocket.
 */
export async function sendWakePush(fcmToken: string, data: Record<string, string>): Promise<void> {
  if (!env.fcmServerKey) {
    return;
  }
  // TODO(phase 3): call FCM HTTP v1 API with env.fcmServerKey, fcmToken, data.
  void fcmToken;
  void data;
}
