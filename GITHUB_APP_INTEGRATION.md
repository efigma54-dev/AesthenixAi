# GitHub App Integration Guide

## Overview

The AI Code Reviewer is fully integrated with GitHub as a GitHub App. This guide walks you through setting up and testing the integration.

## Current Status

✅ **GitHub App Created**

- App ID: 3349093
- Name: aesthenixai
- Private key: Downloaded and stored locally

✅ **Backend Implementation**

- GitHubAppAuthService: JWT signing + token exchange
- WebhookSignatureService: HMAC-SHA256 verification
- PRReviewBotService: PR analysis orchestration
- CodeReviewController: Webhook endpoint

✅ **Configuration**

- Environment variables configured in `.env`
- Webhook secret configured
- Private key path configured

## Setup Steps

### Step 1: Verify GitHub App Configuration

Check that your `.env` file has these values:

```bash
GITHUB_APP_ID=3349093
GITHUB_PRIVATE_KEY_PATH=C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
PR_BOT_WEBHOOK_SECRET=a3f9c8e1b7d4a6c9e2f1a3b5d6c7e8f9a0b1c2d3
```

Verify the private key file exists:

```bash
ls -la aesthenixai.2026-04-21.private-key.pem
```

### Step 2: Start Backend with GitHub App Support

**Option A: Using the startup script (recommended)**

```bash
cd ai-code-reviewer
chmod +x start-github-app.sh
./start-github-app.sh
```

**Option B: Manual startup**

```bash
cd ai-code-reviewer
export $(cat .env | grep -v '^#' | xargs)
mvn spring-boot:run
```

**Expected output:**

```
✓ Environment loaded
✓ GITHUB_APP_ID: 3349093
✓ Private key file exists: ...
✅ All GitHub App configuration verified!

🔧 Starting Spring Boot backend...

GitHubAppAuthService: initialized — appId=3349093
Tomcat started on port(s): 8082 (http)
Started AiCodeReviewerApplication in 16.47 seconds
```

### Step 3: Verify GitHub App is Enabled

Check backend logs for:

```
GitHubAppAuthService: initialized — appId=3349093
```

If you see this instead:

```
GitHubAppAuthService: no App ID configured — GitHub App auth disabled
```

Then the environment variables are not being loaded. Restart with the startup script.

### Step 4: Configure GitHub Webhook (if not already done)

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Edit" → "Webhook"
3. Set Payload URL to your backend:
   - **Local testing**: Use ngrok (see Step 5)
   - **Production**: `https://your-backend.onrender.com/api/webhooks/github`
4. Content type: `application/json`
5. Events: Select **Pull requests** and **Check suites**
6. Click "Save changes"

### Step 5: Local Testing with ngrok

To test locally, you need to expose your backend to the internet using ngrok.

**Start ngrok:**

```bash
ngrok http 8082
```

**Copy the forwarding URL** (e.g., `https://abc123-def456.ngrok-free.dev`)

**Update GitHub webhook:**

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Edit" → "Webhook"
3. Update Payload URL to: `https://YOUR-NGROK-URL/api/webhooks/github`
4. Click "Save changes"

**Test the webhook:**

```bash
curl -X POST https://YOUR-NGROK-URL/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -d '{"zen":"Design for failure."}'
```

Expected response:

```json
{ "message": "AESTHENIXAI webhook active" }
```

## Testing the Integration

### Test 1: Webhook Connectivity

```bash
# Test webhook endpoint
curl -X GET http://localhost:8082/api/webhooks/github

# Expected response:
# {"status":"active","endpoint":"POST /api/webhooks/github","events":["pull_request"],"bot":"AESTHENIXAI"}
```

### Test 2: GitHub App Authentication

The backend will automatically:

1. Load the private key from the configured path
2. Generate a JWT signed with the private key
3. Exchange the JWT for an installation access token
4. Use the token for GitHub API calls

Check logs for:

```
Installation token obtained for repo owner/repo
Using GitHub App token for check run
Check run posted: conclusion=success, annotations=5
```

### Test 3: Create a Test PR

1. Create a test repository or use an existing one
2. Install the GitHub App on the repository:
   - Go to: https://github.com/settings/apps/aesthenixai
   - Click "Install App"
   - Select your test repository
3. Create a pull request with some Java code
4. The backend should automatically:
   - Receive the webhook
   - Analyze the code
   - Post a Check Run with annotations
   - Post a summary comment

### Test 4: Verify Check Run

After creating a PR:

1. Go to the PR
2. Click the "Checks" tab
3. You should see "AESTHENIXAI Code Review" with:
   - ✅ or ❌ status (based on score)
   - Annotations on changed lines
   - Summary comment in Conversation tab

## Troubleshooting

### Issue: "GitHub App auth disabled"

**Cause:** Environment variables not loaded

**Solution:**

```bash
# Verify .env exists
cat .env

# Verify variables are exported
echo $GITHUB_APP_ID

# If empty, use startup script
./start-github-app.sh
```

### Issue: Webhook returns 401 Unauthorized

**Cause:** Webhook signature verification failed

**Solution:**

1. Verify webhook secret in `.env` matches GitHub App settings
2. Check that `PR_BOT_WEBHOOK_SECRET` is set
3. Temporarily disable verification for testing:
   ```bash
   export PR_BOT_WEBHOOK_SECRET=""
   ```

### Issue: "Private key not found"

**Cause:** Path is incorrect or file doesn't exist

**Solution:**

```bash
# Verify file exists
ls -la aesthenixai.2026-04-21.private-key.pem

# Update .env with correct path
# On Windows: C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
# On Linux: /home/user/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
```

### Issue: Check Run shows "403 Forbidden"

**Cause:** Using wrong authentication method

**Solution:**

1. Verify GitHubAppAuthService is initialized (check logs)
2. Ensure private key is loaded correctly
3. Check that GitHub App has "Checks" permission (read/write)

### Issue: Webhook not being triggered

**Cause:** GitHub App not installed on repository

**Solution:**

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Install App"
3. Select the repository
4. Create a new PR to trigger webhook

## Architecture

### Authentication Flow

```
1. Backend starts
   ↓
2. GitHubAppAuthService loads private key from file
   ↓
3. When webhook received:
   - Generate JWT (valid 9 min)
   - Exchange JWT for installation token (valid 1 hour)
   - Cache token, refresh before expiry
   ↓
4. Use token for GitHub API calls (Check Runs, annotations)
```

### Webhook Flow

```
GitHub PR opened
   ↓
GitHub sends webhook to /api/webhooks/github
   ↓
WebhookSignatureService verifies HMAC signature
   ↓
CodeReviewController routes to PRReviewBotService
   ↓
PRReviewBotService:
   - Fetches PR files using GitHub App token
   - Analyzes code (JavaParser + Ollama)
   - Posts Check Run with annotations
   - Posts summary comment
```

## Key Files

| File                           | Purpose                              |
| ------------------------------ | ------------------------------------ |
| `GitHubAppAuthService.java`    | JWT signing, token exchange, caching |
| `WebhookSignatureService.java` | HMAC-SHA256 signature verification   |
| `PRReviewBotService.java`      | PR analysis orchestration            |
| `CodeReviewController.java`    | Webhook endpoint routing             |
| `.env`                         | GitHub App credentials               |
| `start-github-app.sh`          | Environment loading + startup        |

## Environment Variables

```bash
# GitHub App
GITHUB_APP_ID=3349093
GITHUB_PRIVATE_KEY_PATH=C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
PR_BOT_WEBHOOK_SECRET=a3f9c8e1b7d4a6c9e2f1a3b5d6c7e8f9a0b1c2d3

# AI
OLLAMA_URL=http://127.0.0.1:11434
OLLAMA_MODEL=qwen2.5-coder:7b

# GitHub (optional fallback)
GITHUB_TOKEN=ghp_...

# Server
SERVER_PORT=8082
CORS_ORIGINS=*
```

## Production Deployment

### Render Deployment

1. Connect your GitHub repository to Render
2. Add environment variables:
   - `GITHUB_APP_ID`
   - `GITHUB_PRIVATE_KEY` (paste the full PEM content)
   - `PR_BOT_WEBHOOK_SECRET`
   - `SPRING_PROFILES_ACTIVE=prod`
3. Deploy
4. Update GitHub webhook URL to your Render URL

### GitHub Webhook Configuration

1. Go to: https://github.com/settings/apps/aesthenixai
2. Update Payload URL to: `https://your-backend.onrender.com/api/webhooks/github`
3. Verify webhook secret matches `PR_BOT_WEBHOOK_SECRET`

## Success Indicators

✅ Backend logs show:

```
GitHubAppAuthService: initialized — appId=3349093
```

✅ Webhook delivery shows:

```
Response: 200 OK
{"status":"check_suite queued"}
```

✅ Backend logs show:

```
Installation token obtained for repo owner/repo
Check run posted: conclusion=success, annotations=5
```

✅ GitHub PR shows:

- **Checks tab** with AESTHENIXAI check run
- **Conversation tab** with summary comment
- **Files tab** with inline comments on issues

## Next Steps

1. ✅ Verify GitHub App is enabled (check logs)
2. ✅ Test webhook connectivity
3. ✅ Create a test PR to verify integration
4. ✅ Deploy to production (Render)
5. ✅ Monitor webhook deliveries in GitHub App settings

## Support

For issues:

1. Check backend logs: `mvn spring-boot:run` output
2. Verify environment variables: `echo $GITHUB_APP_ID`
3. Check GitHub webhook deliveries: https://github.com/settings/apps/aesthenixai → Webhooks
4. Review GitHub App permissions: https://github.com/settings/apps/aesthenixai → Permissions

---

**Status**: 🟢 Ready for GitHub App integration testing
