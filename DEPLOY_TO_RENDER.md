# Deploy to Render — Step-by-Step Guide

Deploy the backend to Render's free tier so the GitHub App webhook works without ngrok.

---

## Prerequisites

- GitHub account
- Render account (free): https://render.com
- GitHub App already created (App ID: 3349093)

---

## Step 1: Push to GitHub

```bash
cd ai-code-reviewer
git add .
git commit -m "deploy: production-ready backend"
git push origin main
```

---

## Step 2: Create Render Service

1. Go to https://dashboard.render.com
2. Click **New → Web Service**
3. Connect your GitHub repo
4. Select the `ai-code-reviewer` directory as root

Render will auto-detect `render.yaml` and configure everything.

---

## Step 3: Set Secret Environment Variables

In Render dashboard → your service → **Environment**:

| Key                     | Value                         | Notes                                         |
| ----------------------- | ----------------------------- | --------------------------------------------- |
| `CORS_ORIGINS`          | `https://your-app.vercel.app` | Your frontend URL                             |
| `GITHUB_PRIVATE_KEY`    | _(paste full PEM content)_    | From `aesthenixai.2026-04-21.private-key.pem` |
| `GITHUB_TOKEN`          | `ghp_...`                     | Optional PAT fallback                         |
| `PR_BOT_WEBHOOK_SECRET` | _(from your .env)_            | Must match GitHub App webhook secret          |

**How to get the PEM content:**

```bash
cat ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
```

Copy the entire output including `-----BEGIN RSA PRIVATE KEY-----` headers.

---

## Step 4: Deploy

Click **Deploy** in Render dashboard. Wait ~3 minutes for the first build.

Your backend URL will be:

```
https://aesthenixai-backend.onrender.com
```

Test it:

```bash
curl https://aesthenixai-backend.onrender.com/api/health
# Expected: AI Code Reviewer is running
```

---

## Step 5: Update GitHub App Webhook

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click **Edit** → **Webhook**
3. Set **Webhook URL** to:
   ```
   https://aesthenixai-backend.onrender.com/api/webhooks/github
   ```
4. Webhook secret: read from your `.env` file:
   ```bash
   grep PR_BOT_WEBHOOK_SECRET ai-code-reviewer/.env
   ```
5. Subscribe to events: ✅ **Pull request**, ✅ **Check suite**
6. Click **Save changes**

---

## Step 6: Update Frontend (if deployed to Vercel)

In Vercel dashboard → your frontend project → **Settings → Environment Variables**:

```
VITE_API_URL = https://aesthenixai-backend.onrender.com/api
```

Redeploy the frontend.

---

## Step 7: Update VS Code Extension

In VS Code settings (`Ctrl+,`), search for `aesthenixai`:

```
aesthenixai.backendUrl = https://aesthenixai-backend.onrender.com/api
```

Or update `vscode-extension/package.json` default:

```json
"default": "https://aesthenixai-backend.onrender.com/api"
```

---

## Step 8: Test End-to-End

1. Install the GitHub App on a test repo:
   - https://github.com/settings/apps/aesthenixai → **Install App**

2. Create a test PR with bad Java code:

   ```bash
   git checkout -b test-review
   cat > Test.java << 'EOF'
   public class Test {
       public void foo() {
           String s = "";
           for (int i = 0; i < 100; i++) {
               s = s + i;
           }
       }
   }
   EOF
   git add Test.java
   git commit -m "test: bad code for review"
   git push origin test-review
   ```

3. Open a PR on GitHub

4. Watch for:
   - ✅ Check run appears in **Checks** tab
   - 💬 Review comment in **Conversation** tab
   - 📝 Inline annotations in **Files** tab

---

## Render Free Tier Notes

- **Cold starts**: First request after 15 min idle takes ~30s
- **No Ollama**: AI is disabled on free tier (`OLLAMA_ENABLED=false`)
- **Rules only**: Static analysis still works — fast and free
- **512MB RAM**: Sufficient for rule-based analysis

### To enable AI on Render

Option A: Use a paid Render instance with Ollama sidecar
Option B: Point `OLLAMA_URL` to a self-hosted Ollama server
Option C: Use a cloud AI API (OpenAI, Groq, Together) — modify `LocalAIService`

---

## Keep-Alive (Prevent Cold Starts)

Add a free UptimeRobot monitor:

1. Go to https://uptimerobot.com
2. Add monitor: `https://aesthenixai-backend.onrender.com/api/health`
3. Interval: 5 minutes

This keeps the service warm and prevents cold starts.

---

## Troubleshooting

### Webhook returns 401

- Check `PR_BOT_WEBHOOK_SECRET` matches between Render env and GitHub App settings

### Check run not appearing

- Verify GitHub App is installed on the repo
- Check Render logs for `GitHubAppAuthService: initialized`

### No comment posted

- Check `GITHUB_TOKEN` is set in Render env
- Or verify GitHub App has `issues:write` permission

### Build fails on Render

- Render uses `./mvnw` — ensure `mvnw` is executable:
  ```bash
  git update-index --chmod=+x mvnw
  git commit -m "fix: make mvnw executable"
  ```

---

## Architecture After Deployment

```
Developer opens PR
        ↓
GitHub sends webhook
        ↓
Render backend (https://aesthenixai-backend.onrender.com)
        ↓
Rule engine (instant, no AI needed)
        ↓
GitHub Check Run + PR Comment
```

With Ollama enabled (self-hosted):

```
        ↓
Snippet extraction (top-5 flagged regions)
        ↓
Ollama AI analysis (~3-8s)
        ↓
Richer annotations + improved code
```
