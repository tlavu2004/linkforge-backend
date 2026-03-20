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

# Load variables from service .env (robustly without requiring quotes)
echo "Loading environment from $ENV_FILE..."
while IFS='=' read -r key value || [ -n "$key" ]; do
  if [[ $key =~ ^#.* ]] || [[ -z $key ]]; then
    continue
  fi
  key=$(echo "$key" | tr -d '\r' | xargs)
  value=$(echo "$value" | tr -d '\r' | xargs)
  if [ -n "$key" ]; then
    export "$key"="$value"
  fi
done < "$ENV_FILE"

# Determine Spring Profile from environment
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"

# Confirm destructive action
echo "--------------------------------------------------------"
echo "WARNING: This will FLUSH ALL keys in Redis!"
echo "Target Profile: $PROFILE"
echo "Env File:       $ENV_FILENAME"
echo "--------------------------------------------------------"
read -r -p "Are you sure? Type 'yes' to continue: " confirm
if [ "$confirm" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

# Change to service directory
cd "$SERVICE_DIR" || exit 1

# Run Redis flush using Standalone Java Utility via Maven
echo "Executing Redis Flush Utility (Standalone Mode)..."
mvn test -Dtest=RedisCleaningUtility -DfailIfNoTests=false -q

if [ $? -eq 0 ]; then
  echo "Redis cleaned successfully!"
else
  echo "ERROR: Failed to clean Redis via Maven." >&2
  exit 1
fi
