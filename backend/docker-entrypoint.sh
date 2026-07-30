#!/bin/sh
set -e

echo "Running database migrations..."
npx knex --knexfile dist/db/knexfile.js migrate:latest

exec "$@"
