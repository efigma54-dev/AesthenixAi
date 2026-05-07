#!/bin/bash

# Load environment variables from .env file
set -a
source .env
set +a

# Export critical variables for Spring Boot
export GITHUB_APP_ID
export GITHUB_PRIVATE_KEY_PATH
export PR_BOT_WEBHOOK_SECRET
export OLLAMA_URL
export OLLAMA_MODEL
export GITHUB_TOKEN
export CORS_ORIGINS

# Debug output
echo "=== Environment Loaded ==="
echo "GITHUB_APP_ID: $GITHUB_APP_ID"
echo "GITHUB_PRIVATE_KEY_PATH: $GITHUB_PRIVATE_KEY_PATH"
echo "OLLAMA_URL: $OLLAMA_URL"
echo "OLLAMA_MODEL: $OLLAMA_MODEL"
echo "PR_BOT_WEBHOOK_SECRET: $PR_BOT_WEBHOOK_SECRET"
echo "CORS_ORIGINS: $CORS_ORIGINS"
echo ""

# Verify private key file exists
if [ -f "$GITHUB_PRIVATE_KEY_PATH" ]; then
    echo "✓ Private key file exists: $GITHUB_PRIVATE_KEY_PATH"
else
    echo "✗ Private key file NOT found: $GITHUB_PRIVATE_KEY_PATH"
fi
echo ""

# Start Spring Boot
echo "Starting Spring Boot on port 8082..."
mvn spring-boot:run
