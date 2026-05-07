#!/usr/bin/env bash
# Kills all processes on port 8080

echo "Killing all processes on port 8080..."
cmd //c "taskkill /F /IM java.exe" 2>/dev/null || true
sleep 2
echo "Done. Port 8080 should now be free."
