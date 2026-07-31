import path from 'path';
import dotenv from 'dotenv';

// Loaded with an explicit path (rather than relying on process.cwd())
// because the knex CLI changes the working directory before requiring
// this module, which would otherwise cause `.env` to be missed.
dotenv.config({ path: path.join(__dirname, '..', '..', '.env') });

function required(name: string, fallback?: string): string {
  const value = process.env[name] ?? fallback;
  if (value === undefined) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

export const env = {
  nodeEnv: process.env.NODE_ENV ?? 'development',
  port: parseInt(process.env.PORT ?? '3000', 10),
  publicUrl: process.env.PUBLIC_URL ?? 'http://localhost:3000',

  postgres: {
    host: required('POSTGRES_HOST', process.env.NODE_ENV === 'production' ? 'postgres' : 'localhost'),
    port: parseInt(process.env.POSTGRES_PORT ?? '5432', 10),
    database: required('POSTGRES_DB', 'followme'),
    user: required('POSTGRES_USER', 'followme'),
    password: required('POSTGRES_PASSWORD', 'followme'),
  },

  jwt: {
    accessSecret: required('JWT_ACCESS_SECRET'),
    refreshSecret: required('JWT_REFRESH_SECRET'),
    deviceSecret: required('JWT_DEVICE_SECRET'),
    accessExpiresIn: process.env.JWT_ACCESS_EXPIRES_IN ?? '15m',
    refreshExpiresIn: process.env.JWT_REFRESH_EXPIRES_IN ?? '30d',
  },

  storagePath: process.env.STORAGE_PATH ?? '/data/recordings',

  fcmServerKey: process.env.FCM_SERVER_KEY ?? '',

  corsOrigins: (process.env.CORS_ORIGINS ?? '*').split(',').map((s) => s.trim()),
};
