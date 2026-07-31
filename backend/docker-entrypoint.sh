#!/bin/sh
set -e

echo "Running database migrations..."
max_retries=30
count=0

until npx knex --knexfile dist/db/knexfile.js migrate:latest; do
  count=$((count + 1))
  if [ $count -ge $max_retries ]; then
    echo "Migration failed after $max_retries attempts. Exiting."
    exit 1
  fi
  echo "Database connection failed, retrying in 2 seconds... ($count/$max_retries)"
  sleep 2
done

exec "$@"
