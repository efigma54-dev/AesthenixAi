#!/usr/bin/env bash
# Loads .env, kills any existing instance on port 8080, then starts the backend.
# Usage: ./start.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# ── Load .env ──────────────────────────────────────────────
if [ -f "$ENV_FILE" ]; then
  echo "Loading environment from .env"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "Error: .env not found."
  echo "  cp .env.example .env  then add your OPENAI_API_KEY"
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
EXISTING_PID=$(cmd //c "netstat -ano 2>nul | findstr :${PORT}" 2>/dev/null \
  | awk '{print $NF}' | sort -u | head -1)

if [ -n "$EXISTING_PID" ] && [ "$EXISTING_PID" != "0" ]; then
  echo "Killing existing process on port $PORT (PID $EXISTING_PID)..."
  cmd //c "taskkill /PID $EXISTING_PID /F" 2>/dev/null || true
  sleep 2
fi

# Also try curl-based check as fallback
if curl -s --max-time 1 http://localhost:$PORT/api/health > /dev/null 2>&1; then
  echo "Warning: something still on port $PORT — trying to continue anyway"
fi

echo "Starting AESTHENIXAI backend on http://localhost:$PORT ..."

# Unset stale JAVA_HOME so mvnw uses java from PATH
unset JAVA_HOME

# Pass env vars explicitly as Spring Boot properties so they're guaranteed to load
exec "$SCRIPT_DIR/mvnw" spring-boot:run \
  -Dspring-boot.run.arguments="\
--ai.provider=${AI_PROVIDER:-openai} \
--ollama.url=${OLLAMA_URL:-http://localhost:11434} \
--ollama.model=${OLLAMA_MODEL:-qwen3.5:9b} \
--ollama.timeout=${OLLAMA_TIMEOUT:-300} \
--openai.api-key=${OPENAI_API_KEY:-} \
--github.token=${GITHUB_TOKEN:-} \
--cors.allowed-origins=${CORS_ORIGINS:-*}"
