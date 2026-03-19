#!/bin/bash

# Get the directory where script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Go up 5 levels from src/main/resources/scripts/redis to reach project root
SERVICE_DIR="$(dirname "$(dirname "$(dirname "$(dirname "$(dirname "$SCRIPT_DIR")")")")")"

ENV_FILENAME=${1:-".env"}
ENV_FILE="$SERVICE_DIR/$ENV_FILENAME"

# Check for .env file in service directory
if [ ! -f "$ENV_FILE" ]; then
  echo "ERROR: Environment file not found at $ENV_FILE" >&2
  exit 1
fi

# Load variables from service .env
echo "Loading environment from $ENV_FILE..."
set -a
. "$ENV_FILE"
set +a

# Validate required variables
required_vars=(
  REDIS_HOST
  REDIS_PORT
)

for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "ERROR: Required variable '$var' is missing in $ENV_FILENAME" >&2
    exit 1
  fi
done

# Construct connection URL
PROTOCOL="redis"
if [ "$REDIS_SSL_ENABLED" == "true" ]; then
  PROTOCOL="rediss"
fi

# Build redis-cli command
if [ -n "$REDIS_PASSWORD" ]; then
  REDIS_CMD="redis-cli -u $PROTOCOL://:$REDIS_PASSWORD@$REDIS_HOST:$REDIS_PORT"
else
  REDIS_CMD="redis-cli -h $REDIS_HOST -p $REDIS_PORT"
fi

# Confirm destructive action
echo "--------------------------------------------------------"
echo "WARNING: This will FLUSH ALL keys in Redis!"
echo "Target:  $REDIS_HOST:$REDIS_PORT ($PROTOCOL)"
echo "Env File: $ENV_FILENAME"
echo "--------------------------------------------------------"
read -r -p "Are you sure? Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

# Run Redis flush
echo "Flushing Redis..."
$REDIS_CMD FLUSHALL

if [ $? -eq 0 ]; then
  echo "Redis cleaned successfully!"
else
  echo "ERROR: Failed to clean Redis. Make sure redis-cli is installed and server is reachable." >&2
  exit 1
fi
