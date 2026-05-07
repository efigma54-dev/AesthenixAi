#!/usr/bin/env bash
# Complete restart: kills backend, ngrok, then starts both fresh
# Usage: ./restart-all.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== AESTHENIXAI Complete Restart ==="
echo ""

# 1. Kill backend on port 8080
echo "Step 1: Killing backend processes..."
./kill-port-8080.sh
echo ""

# 2. Kill ngrok
echo "Step 2: Killing ngrok..."
taskkill //F //IM ngrok.exe 2>/dev/null || echo "ngrok not running"
sleep 2
echo ""

# 3. Start backend
echo "Step 3: Starting backend..."
echo "Backend will start in this terminal."
echo "Open a NEW terminal and run: ngrok http 8080"
echo ""
echo "Then update GitHub webhook URL with the new ngrok URL"
echo ""

# Start backend (this will block)
./start.sh
