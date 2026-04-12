# 🔌 GitHub Webhook Testing Guide

## Current Setup Status

✅ **What You Have:**
- ngrok running and forwarding to port 8080
- ngrok URL: `https://handprint-headlamp-conceal.ngrok-free.dev`
- GitHub webhook configured (or ready to configure)
- Spring Boot backend code ready

⚠️ **What We're Fixing:**
- Maven wrapper path issue (Windows Java path with spaces)
- Will use Docker alternative or manual start

---

## 🚀 Option 1: Quick Test Without Backend (RECOMMENDED FOR NOW)

### Step 1: Check ngrok is capturing traffic

1. Open **ngrok dashboard**: `http://127.0.0.1:4040`
2. You should see the ngrok tunnel forwarding to localhost:8080
3. Keep this open for monitoring

### Step 2: Test webhook endpoint manually

**Create a test file to manually send webhook:**

```powershell
# Create test webhook payload
$payload = @{
    "action" = "opened"
    "pull_request" = @{
        "id" = 12345
        "number" = 1
        "title" = "Test PR"
        "head" = @{
            "sha" = "abc123"
        }
        "base" = @{
            "sha" = "def456"
        }
    }
    "repository" = @{
        "name" = "test-repo"
        "owner" = @{
            "login" = "yourusername"
        }
    }
} | ConvertTo-Json

# Send to webhook
$headers = @{
    "Content-Type" = "application/json"
    "User-Agent" = "GitHub-Hookshot"
}

try {
    $response = Invoke-WebRequest `
        -Uri "https://handprint-headlamp-conceal.ngrok-free.dev/api/github/webhook" `
        -Method POST `
        -Body $payload `
        -Headers $headers `
        -SkipCertificateCheck
    
    Write-Host "✅ Webhook test successful!"
    Write-Host "Status Code: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)"
}
```

**What to expect:**
- ✅ Status 200 = Backend is running and received it
- ❌ Connection refused = Backend not running (expected for now)
- ⚠️ Timeout = ngrok URL wrong or not forwarding

---

## 🐳 Option 2: Docker Alternative (EASIEST)

If you have Docker installed, create a simple mock backend:

**Create `Dockerfile.webhook`:**

```dockerfile
FROM python:3.9-slim
WORKDIR /app
COPY - <<'EOF' webhook_server.py
from flask import Flask, request, jsonify
import json

app = Flask(__name__)

@app.route('/api/github/webhook', methods=['GET', 'POST'])
def webhook():
    if request.method == 'POST':
        data = request.get_json()
        pr_number = data.get('pull_request', {}).get('number', 'unknown')
        print(f"✅ PR received: {pr_number}")
        return jsonify({"status": "ok"}), 200
    return jsonify({"status": "webhook ready"}), 200

@app.route('/api/github/webhook/health', methods=['GET'])
def health():
    return jsonify({"status": "healthy"}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
EOF

RUN pip install flask
CMD ["python", "webhook_server.py"]
```

**Run it:**
```bash
docker build -f Dockerfile.webhook -t webhook-mock .
docker run -p 8080:8080 webhook-mock
```

---

## 🔧 Option 3: Fix Maven & Start Backend Properly

### The Problem
Windows Java path has spaces: `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot`

The `mvnw.cmd` script doesn't quote paths properly.

### The Solution

**Fix the mvnw.cmd file:**

Replace line 8-9 with:
```batch
if defined JAVA_HOME (
  set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_CMD=java.exe"
)
```

✅ **Already done!** Now try:

```powershell
# In PowerShell
cd c:\ai-code-reviewe\ai-code-reviewer

# Set Java home explicitly
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"

# Use cmd /k to stay in cmd.exe (avoids PowerShell issues)
cmd /k "set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot && mvnw.cmd spring-boot:run"
```

⏳ **Wait for:** "Started AiCodeReviewerApplication in X seconds"

---

## ✅ Once Backend is Running

### Test 1: Direct Health Check

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health -SkipCertificateCheck
```

**Expected response:**
```json
{
  "status": "UP"
}
```

### Test 2: Create a Real PR in GitHub

1. Go to your repository
2. Create a new PR (or update an existing one)
3. GitHub will send webhook to ngrok URL
4. Watch your backend logs for:
   ```
   PR received: owner/repo #123
   ```

### Test 3: Check GitHub Webhook Deliveries

1. Go to **Settings → Webhooks**
2. Click your webhook URL
3. Scroll to "Recent Deliveries"
4. Click on the latest delivery
5. View the **Response** tab:
   - ✅ Status 200 = Success
   - ❌ 4xx/5xx = Error (check backend logs)

---

## 🔍 Debugging Checklist

| Check | Command | Expected |
|-------|---------|----------|
| ngrok running? | `netstat -ano \| findstr :4040` | See process listening on 4040 |
| Backend running? | `netstat -ano \| findstr :8080` | See process listening on 8080 |
| Java works? | `java -version` | Shows OpenJDK 17 |
| Webhook reachable? | `Invoke-WebRequest https://handprint-headlamp-conceal.ngrok-free.dev/api/github/webhook` | Status 200 or 405 (POST required) |
| GitHub received it? | GitHub Settings → Webhooks → Recent Deliveries | Status 200 |

---

## 🎯 Quick Test Workflow

**STAGE 1: Is ngrok forwarding?**
```powershell
curl -i https://handprint-headlamp-conceal.ngrok-free.dev/api/github/webhook
```

**STAGE 2: Is backend running?**
```powershell
curl -i http://localhost:8080/actuator/health
```

**STAGE 3: Create a PR**
- Open your repo → create/update PR → GitHub sends webhook

**STAGE 4: Check backend sees it**
- Terminal shows: `PR received: owner/repo #123`

**STAGE 5: Verify GitHub got response**
- GitHub Settings → Webhooks → Recent Deliveries → Status 200

---

## 📝 Quick Note on ngrok URL Changes

**Important:** ngrok free tier changes URL on restart
- Each `ngrok http 8080` restart = new URL
- Solution: Use ngrok reserved domain (paid feature) OR
- Update GitHub webhook URL each time

**To prevent URL change:**
```bash
# Reserve a domain (premium)
ngrok http 8080 --domain your-reserved-domain.ngrok-free.app
```

---

## 🎬 Live Demo Workflow

When ready to demo in interview:

1. **Show ngrok panel** (http://127.0.0.1:4040) — "Here's the tunnel between GitHub and my server"
2. **Create PR in real GitHub repo** — "Watch the webhook come in live"
3. **Check backend logs** — "See the request hit my API"
4. **Show GitHub webhook delivery** — "200 status = success"
5. **Show automated PR comment** — "Bot left a code review comment"

---

## 🚀 Next Steps

### Immediate (Today)
- [ ] Verify ngrok is running
- [ ] Test webhook endpoint accessibility
- [ ] Create real PR to trigger webhook
- [ ] Check GitHub webhook deliveries for 200 status

### Soon (Before Interview)
- [ ] Get backend running (use Docker if Maven won't cooperate)
- [ ] Trigger PR → See automated response
- [ ] Record 30-second demo video
- [ ] Practice demo narrative

---

## 💡 Pro Tips

1. **ngrok inspection is your friend** — Every webhook visible at http://127.0.0.1:4040
2. **Check GitHub first** — If 200 status there, backend received it
3. **Use `-SkipCertificateCheck` in PowerShell** for https ngrok URLs
4. **Keep ngrok panel open** while testing — Real-time visibility

---

**You've got this! The webhook architecture is solid. Just need to verify the connection. 🚀**
