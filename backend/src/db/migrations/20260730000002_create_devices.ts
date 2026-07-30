import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('devices', (table) => {
    table.uuid('id').primary();
    table
      .uuid('user_id')
      .notNullable()
      .references('id')
      .inTable('users')
      .onDelete('CASCADE');
    table.text('name').notNullable();
    table.text('pairing_token').unique();
    table.timestamp('pairing_token_expires_at', { useTz: true });
    table.boolean('paired').notNullable().defaultTo(false);
    table.integer('device_token_version').notNullable().defaultTo(0);
    table.text('status').notNullable().defaultTo('offline');
    table.timestamp('last_seen_at', { useTz: true });
    table.timestamp('created_at', { useTz: true }).notNullable().defaultTo(knex.fn.now());
    table.timestamp('updated_at', { useTz: true }).notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.raw(
    "ALTER TABLE devices ADD CONSTRAINT devices_status_check CHECK (status IN ('online','offline'))"
  );
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('devices');
}
