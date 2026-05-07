# GitHub App Startup Guide

## Quick Start (After Context Transfer)

The GitHub App integration is **fully implemented** but needs proper startup sequence.

### Current Status

- ✅ GitHub App created (ID: 3349093)
- ✅ Private key downloaded
- ✅ Webhook secret configured
- ✅ Backend code complete (GitHubAppAuthService, WebhookSignatureService, check_suite handler)
- ✅ `.env` file configured
- ⚠️ Need to restart with proper environment loading

### Issue from Previous Session

The backend was showing empty values for `GITHUB_APP_ID` and `GITHUB_PRIVATE_KEY_PATH` because:

1. Environment variables weren't being exported properly
2. ngrok was already running (blocking restart)

### Solution: Complete Restart

#### Step 1: Kill Everything

```bash
cd /c/ai-code-reviewe/ai-code-reviewer

# Kill backend
./kill-port-8080.sh

# Kill ngrok
taskkill //F //IM ngrok.exe
```

#### Step 2: Start Backend (Updated Script)

```bash
./start.sh
```

**What the updated script does:**

- Loads `.env` file
- **Exports** environment variables (not just sourcing)
- Verifies private key file exists
- Shows debug output for all critical vars
- Starts Spring Boot with environment variables

**Expected output:**

```
Loading environment from .env
DEBUG: AI_PROVIDER=ollama
DEBUG: GITHUB_APP_ID=3349093
DEBUG: GITHUB_PRIVATE_KEY_PATH=C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
DEBUG: PR_BOT_WEBHOOK_SECRET=a3f9c8e1b7...
✓ Private key file exists: C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
...
GitHubAppAuthService: initialized — appId=3349093
```

#### Step 3: Start ngrok (New Terminal)

```bash
ngrok http 8080
```

**Copy the new URL** (e.g., `https://xyz-abc-def.ngrok-free.dev`)

#### Step 4: Update GitHub Webhook

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Edit" → "Webhook"
3. Update URL to: `https://YOUR-NEW-NGROK-URL/api/webhooks/github`
4. Click "Save changes"

#### Step 5: Test

```bash
cd /c/ai-code-reviewe
git commit --allow-empty -m "test github app"
git push origin test-ai
```

**Expected result:**

- GitHub sends `check_suite` event
- Backend logs: `check_suite triggered analysis for PR #1`
- PR shows **AESTHENIXAI** in Checks tab
- Webhook returns `200 OK` (not 500)

---

## Troubleshooting

### Backend shows empty GITHUB_APP_ID

**Cause:** Environment variables not exported  
**Fix:** Use updated `start.sh` script (exports vars before starting Spring Boot)

### Webhook returns 500

**Cause:** GitHubAppAuthService not initialized (missing env vars)  
**Fix:** Restart backend with updated script, verify debug output shows values

### ngrok error: "endpoint already online"

**Cause:** ngrok still running from previous session  
**Fix:** `taskkill //F //IM ngrok.exe` then restart

### Private key not found

**Cause:** Path incorrect or file moved  
**Fix:** Verify file exists at `C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem`

### Check Run shows 403 Forbidden

**Cause:** Using PAT instead of GitHub App token  
**Fix:** Ensure GitHubAppAuthService initialized (check logs for "initialized — appId=3349093")

---

## Architecture

### Authentication Flow

1. **GitHubAppAuthService** loads private key from file
2. Generates JWT signed with private key (valid 9 min)
3. Exchanges JWT for installation access token (valid 1 hour)
4. Caches token, refreshes 5 min before expiry
5. Uses token for Check Runs API

### Webhook Flow

1. GitHub sends `check_suite.requested` event
2. **WebhookSignatureService** verifies HMAC-SHA256 signature
3. **CodeReviewController** routes to `prReviewBotService.handleCheckSuite()`
4. **PRReviewBotService** extracts PR info, fetches files with App token
5. **AnalysisPipeline** analyzes code (JavaParser + Ollama)
6. Posts Check Run with annotations (Checks tab)
7. Posts summary comment (Conversation tab)
8. Posts inline comments (Files tab)

### Key Files

- `GitHubAppAuthService.java` — JWT signing, token exchange, caching
- `WebhookSignatureService.java` — HMAC signature verification
- `PRReviewBotService.java` — check_suite handler, PR analysis orchestration
- `CodeReviewController.java` — webhook endpoint, event routing
- `.env` — GitHub App credentials
- `start.sh` — environment loading, backend startup

---

## Next Steps After Successful Startup

1. **Verify Checks Tab** — PR should show AESTHENIXAI with score
2. **Test Multiple PRs** — Push more commits, verify each triggers review
3. **Check Annotations** — Click into Check Run, verify inline annotations appear
4. **Monitor Logs** — Watch for "Installation token obtained" messages
5. **Deploy to Render** — Add env vars to Render dashboard, deploy

---

## Environment Variables Reference

```bash
# AI Provider
AI_PROVIDER=ollama
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=qwen3.5:9b

# GitHub App (required for Checks API)
GITHUB_APP_ID=3349093
GITHUB_PRIVATE_KEY_PATH=C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
PR_BOT_WEBHOOK_SECRET=a3f9c8e1b7d4a6c9e2f1a3b5d6c7e8f9a0b1c2d3

# Optional: GitHub PAT (fallback for comments)
GITHUB_TOKEN=ghp_...

# CORS
CORS_ORIGINS=*
```

---

## Success Indicators

✅ Backend logs show:

```
GitHubAppAuthService: initialized — appId=3349093
Loading GitHub App private key from file: C:/ai-code-reviewe/...
```

✅ Webhook delivery shows:

```
Response: 200 OK
{"status":"check_suite queued"}
```

✅ Backend logs show:

```
check_suite triggered analysis for PR #1
Using GitHub App token for check run
Check run posted: conclusion=success, annotations=5
```

✅ GitHub PR shows:

- **Checks tab** with AESTHENIXAI check run
- **Conversation tab** with summary comment
- **Files tab** with inline comments on issues

---

## Alternative: Use restart-all.sh

For convenience, use the all-in-one restart script:

```bash
cd /c/ai-code-reviewe/ai-code-reviewer
./restart-all.sh
```

This kills backend + ngrok, then starts backend.  
You still need to start ngrok manually in a new terminal.
