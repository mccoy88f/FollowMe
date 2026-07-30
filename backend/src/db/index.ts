import knexLib from 'knex';
import knexConfig from './knexfile';

export const db = knexLib(knexConfig);
