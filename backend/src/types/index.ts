export interface UserRecord {
  id: string;
  email: string;
  password_hash: string;
  refresh_token_version: number;
  created_at: Date;
  updated_at: Date;
}

export interface DeviceRecord {
  id: string;
  user_id: string;
  name: string;
  pairing_token: string | null;
  pairing_token_expires_at: Date | null;
  paired: boolean;
  device_token_version: number;
  status: 'online' | 'offline';
  last_seen_at: Date | null;
  created_at: Date;
  updated_at: Date;
}

export type RecordingType = 'audio' | 'video' | 'audio_video';
export type RecordingStatus = 'recording' | 'completed' | 'failed';

export interface RecordingRecord {
  id: string;
  device_id: string;
  type: RecordingType;
  status: RecordingStatus;
  file_path: string;
  bytes_received: string | number;
  started_at: Date;
  ended_at: Date | null;
  duration_seconds: number | null;
  created_at: Date;
}

export interface AccessTokenPayload {
  sub: string; // user id
  type: 'access';
}

export interface RefreshTokenPayload {
  sub: string; // user id
  type: 'refresh';
  ver: number; // refresh_token_version at issuance time
}

export interface DeviceTokenPayload {
  sub: string; // device id
  userId: string;
  type: 'device';
  ver: number; // device_token_version at issuance time
}
