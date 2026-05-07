#!/bin/bash

# GitHub App Integration Startup Script
# This script properly loads environment variables and starts the backend
# with GitHub App authentication enabled

set -e

echo "🚀 Starting AI Code Reviewer with GitHub App Integration"
echo ""

# Load environment variables from .env
if [ -f .env ]; then
    echo "📋 Loading environment from .env..."
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Environment loaded"
else
    echo "❌ .env file not found!"
    exit 1
fi

# Verify critical variables
echo ""
echo "🔍 Verifying GitHub App configuration..."

if [ -z "$GITHUB_APP_ID" ]; then
    echo "❌ GITHUB_APP_ID not set"
    exit 1
fi
echo "✓ GITHUB_APP_ID: $GITHUB_APP_ID"

if [ -z "$GITHUB_PRIVATE_KEY_PATH" ]; then
    echo "❌ GITHUB_PRIVATE_KEY_PATH not set"
    exit 1
fi

# Convert Windows path to Unix path if needed
PRIVATE_KEY_PATH=$(echo "$GITHUB_PRIVATE_KEY_PATH" | sed 's/\\/\//g' | sed 's/C:/\/c/')

if [ ! -f "$PRIVATE_KEY_PATH" ]; then
    echo "❌ Private key file not found: $PRIVATE_KEY_PATH"
    exit 1
fi
echo "✓ Private key file exists: $PRIVATE_KEY_PATH"

if [ -z "$PR_BOT_WEBHOOK_SECRET" ]; then
    echo "⚠️  PR_BOT_WEBHOOK_SECRET not set (webhook signature verification disabled)"
else
    echo "✓ PR_BOT_WEBHOOK_SECRET configured"
fi

echo ""
echo "✅ All GitHub App configuration verified!"
echo ""

# Display configuration summary
echo "📊 Configuration Summary:"
echo "  - GitHub App ID: $GITHUB_APP_ID"
echo "  - Private Key: $PRIVATE_KEY_PATH"
echo "  - Ollama URL: $OLLAMA_URL"
echo "  - Ollama Model: $OLLAMA_MODEL"
echo "  - Server Port: 8082"
echo ""

# Start backend
echo "🔧 Starting Spring Boot backend..."
echo ""

./mvnw spring-boot:run \
    -Dspring-boot.run.arguments="--github.app-id=$GITHUB_APP_ID --github.private-key-path=$PRIVATE_KEY_PATH --pr-bot.webhook-secret=$PR_BOT_WEBHOOK_SECRET"
