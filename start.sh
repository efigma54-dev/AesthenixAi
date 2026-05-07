#!/usr/bin/env bash
# Loads .env, kills any existing instance on port 8080, then starts the backend.
# Usage: ./start.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ── Load .env ──────────────────────────────────────────────
if [ -f "$ENV_FILE" ]; then
  echo "Loading environment from .env"
  # Export all variables from .env to make them available to Java
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  
  # Verify critical env vars loaded
  echo "DEBUG: AI_PROVIDER=$AI_PROVIDER"
  echo "DEBUG: GITHUB_APP_ID=$GITHUB_APP_ID"
  echo "DEBUG: GITHUB_PRIVATE_KEY_PATH=$GITHUB_PRIVATE_KEY_PATH"
  if [ -n "$PR_BOT_WEBHOOK_SECRET" ]; then
    echo "DEBUG: PR_BOT_WEBHOOK_SECRET=${PR_BOT_WEBHOOK_SECRET:0:10}..."
  fi
  
  # Verify private key file exists
  if [ -n "$GITHUB_PRIVATE_KEY_PATH" ] && [ ! -f "$GITHUB_PRIVATE_KEY_PATH" ]; then
    echo "WARNING: Private key file not found: $GITHUB_PRIVATE_KEY_PATH"
  elif [ -n "$GITHUB_PRIVATE_KEY_PATH" ]; then
    echo "✓ Private key file exists: $GITHUB_PRIVATE_KEY_PATH"
  fi
else
  echo "Error: .env not found."
  echo "  cp .env.example .env  then add your configuration"
  exit 1
fi

# ── Validate required key (skip when using Ollama) ────────
if [ "${AI_PROVIDER:-openai}" != "ollama" ]; then
  if [ -z "$OPENAI_API_KEY" ] || \
     [ "$OPENAI_API_KEY" = "sk-..." ] || \
     [ "$OPENAI_API_KEY" = "your-new-key-here" ] || \
     [ "$OPENAI_API_KEY" = "sk-PASTE-YOUR-NEW-KEY-HERE" ]; then
    echo "Error: OPENAI_API_KEY is not set in .env"
    echo "  Get a key at https://platform.openai.com/api-keys"
    echo "  Or switch to local AI: set AI_PROVIDER=ollama in .env"
    exit 1
  fi
  echo "✓ OpenAI API key loaded (${OPENAI_API_KEY:0:8}...)"
else
  echo "✓ Using local Ollama model: ${OLLAMA_MODEL:-qwen3.5:9b}"
fi

# ── Kill any existing process on port 8080 ─────────────────
PORT=8080
echo "Checking for processes on port $PORT..."

# Try multiple methods to kill port 8080
# Method 1: netstat + taskkill
PIDS=$(cmd //c "netstat -ano 2>nul | findstr :${PORT}" 2>/dev/null \
  | awk '{print $NF}' | sort -u | grep -v "^0$")

if [ -n "$PIDS" ]; then
  for PID in $PIDS; do
    echo "Killing process $PID on port $PORT..."
    cmd //c "taskkill /PID $PID /F" 2>/dev/null || true
  done
  sleep 3
fi

# Method 2: Kill all java.exe processes (nuclear option)
echo "Killing all Java processes..."
cmd //c "taskkill /F /IM java.exe" 2>/dev/null || true
sleep 2

echo "Starting AESTHENIXAI backend on http://localhost:$PORT ..."

# Unset stale JAVA_HOME so mvnw uses java from PATH
unset JAVA_HOME

# Export env vars so Spring Boot can read them directly
export GITHUB_APP_ID
export GITHUB_PRIVATE_KEY_PATH
export PR_BOT_WEBHOOK_SECRET
export AI_PROVIDER
export OLLAMA_URL
export OLLAMA_MODEL
export OLLAMA_TIMEOUT
export GITHUB_TOKEN
export CORS_ORIGINS

echo ""
echo "Starting Spring Boot with environment variables..."
echo ""

# Start Spring Boot - it will read from environment variables via application.yml
exec mvn spring-boot:run
