import path from 'path';
import type { Knex } from 'knex';
import { env } from '../config/env';

const config: Knex.Config = {
  client: 'pg',
  connection: {
    host: env.postgres.host,
    port: env.postgres.port,
    database: env.postgres.database,
    user: env.postgres.user,
    password: env.postgres.password,
  },
  migrations: {
    // Absolute path so this resolves correctly whether run via ts-node
    // against src/ in dev, or via plain node against dist/ in production.
    directory: path.join(__dirname, 'migrations'),
    extension: 'ts',
  },
  pool: { min: 2, max: 10 },
};

export default config;
