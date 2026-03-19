#!/bin/bash

# Get the directory where script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Go up 5 levels from src/main/resources/scripts/postgres to reach project root
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

# Validate required variables (aligned with LinkForge .env)
required_vars=(
  DB_URL
  DB_USERNAME
  DB_PASSWORD
)

for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    echo "ERROR: Required variable '$var' is missing in $ENV_FILENAME" >&2
    exit 1
  fi
done

# Confirm destructive action
echo "--------------------------------------------------------"
echo "WARNING: This will DROP all tables and data in the DB!"
echo "Target URL: $DB_URL"
echo "Env File:   $ENV_FILENAME"
echo "--------------------------------------------------------"
read -r -p "Are you sure? Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

# Change to service directory
cd "$SERVICE_DIR" || {
  echo "ERROR: Cannot cd to $SERVICE_DIR" >&2
  exit 1
}

# Run Flyway clean with explicit parameters from .env
mvn flyway:clean -Dflyway.cleanDisabled=false -Dflyway.url="$DB_URL" -Dflyway.user="$DB_USERNAME" -Dflyway.password="$DB_PASSWORD"

echo "PostgreSQL database cleaned successfully!"