import type { Knex } from 'knex';

export async function up(knex: Knex): Promise<void> {
  await knex.schema.createTable('recordings', (table) => {
    table.uuid('id').primary();
    table
      .uuid('device_id')
      .notNullable()
      .references('id')
      .inTable('devices')
      .onDelete('CASCADE');
    table.text('type').notNullable();
    table.text('status').notNullable().defaultTo('recording');
    table.text('file_path').notNullable();
    table.bigInteger('bytes_received').notNullable().defaultTo(0);
    table.timestamp('started_at', { useTz: true }).notNullable().defaultTo(knex.fn.now());
    table.timestamp('ended_at', { useTz: true });
    table.integer('duration_seconds');
    table.timestamp('created_at', { useTz: true }).notNullable().defaultTo(knex.fn.now());
  });

  await knex.schema.raw(
    "ALTER TABLE recordings ADD CONSTRAINT recordings_type_check CHECK (type IN ('audio','video','audio_video'))"
  );
  await knex.schema.raw(
    "ALTER TABLE recordings ADD CONSTRAINT recordings_status_check CHECK (status IN ('recording','completed','failed'))"
  );
  await knex.schema.raw('CREATE INDEX recordings_device_id_idx ON recordings (device_id)');
}

export async function down(knex: Knex): Promise<void> {
  await knex.schema.dropTableIfExists('recordings');
}
