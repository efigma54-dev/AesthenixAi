#!/usr/bin/env bash
# Quick test to verify .env is loaded correctly
# Usage: ./test-env.sh

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

echo "=== Environment Variable Test ==="
echo ""

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ .env file not found at: $ENV_FILE"
  exit 1
fi

echo "✅ .env file exists"
echo ""

# Load .env
set -a
source "$ENV_FILE"
set +a

echo "Testing environment variables:"
echo ""

# Test each critical variable
test_var() {
  local name=$1
  local value=${!name}
  if [ -z "$value" ]; then
    echo "❌ $name is empty or not set"
    return 1
  else
    if [ ${#value} -gt 50 ]; then
      echo "✅ $name = ${value:0:50}..."
    else
      echo "✅ $name = $value"
    fi
    return 0
  fi
}

all_good=true

test_var "AI_PROVIDER" || all_good=false
test_var "OLLAMA_URL" || all_good=false
test_var "OLLAMA_MODEL" || all_good=false
test_var "GITHUB_APP_ID" || all_good=false
test_var "GITHUB_PRIVATE_KEY_PATH" || all_good=false
test_var "PR_BOT_WEBHOOK_SECRET" || all_good=false

echo ""

# Test private key file exists
if [ -n "$GITHUB_PRIVATE_KEY_PATH" ]; then
  if [ -f "$GITHUB_PRIVATE_KEY_PATH" ]; then
    echo "✅ Private key file exists: $GITHUB_PRIVATE_KEY_PATH"
  else
    echo "❌ Private key file NOT FOUND: $GITHUB_PRIVATE_KEY_PATH"
    all_good=false
  fi
fi

echo ""

if [ "$all_good" = true ]; then
  echo "🎉 All environment variables are configured correctly!"
  echo ""
  echo "You can now run: ./start.sh"
else
  echo "⚠️  Some environment variables are missing or incorrect"
  echo ""
  echo "Please check your .env file"
fi
