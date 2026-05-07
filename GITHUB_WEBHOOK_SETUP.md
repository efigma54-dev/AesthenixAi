# GitHub Webhook Setup — One-Time Configuration

## Current ngrok URL

```
https://handprint-headlamp-conceal.ngrok-free.dev
```

## Webhook URL to set in GitHub App

```
https://handprint-headlamp-conceal.ngrok-free.dev/api/webhooks/github
```

> ⚠️ **ngrok URL changes on every restart.** After restarting ngrok, get the new URL with:
>
> ```bash
> curl http://127.0.0.1:4040/api/tunnels
> ```
>
> Then update the GitHub App webhook URL again (step 4 below).
> For a permanent URL, deploy to Render.

## Steps (do this once)

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click **"Edit"** on the app
3. Scroll to **"Webhook"** section
4. Set **Webhook URL** to:
   ```
   https://handprint-headlamp-conceal.ngrok-free.dev/api/webhooks/github
   ```
5. Set **Webhook secret** to the value of `PR_BOT_WEBHOOK_SECRET` from your `.env` file:
   ```bash
   # Read it with:
   grep PR_BOT_WEBHOOK_SECRET ai-code-reviewer/.env
   ```
6. Under **"Subscribe to events"**, check:
   - ✅ Pull request
   - ✅ Check suite (optional, for check runs)
7. Click **"Save changes"**

## Verify it works

After saving, GitHub will send a ping event. Check backend logs for:

```
GitHub webhook: event=ping delivery=...
```

## Test with a real PR

1. Create a branch in your repo:

   ```bash
   git checkout -b test-ai-review
   ```

2. Add a Java file with bad code:

   ```bash
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
   git commit -m "test: add code for AI review"
   git push origin test-ai-review
   ```

3. Open a PR on GitHub

4. Watch backend logs — you should see:

   ```
   GitHub webhook: event=pull_request delivery=...
   PR #1 in owner/repo (action=opened, sha=abc1234)
   Analyzing Test.java (87 chars, full-file)
   AI call succeeded in Xms
   Summary comment posted for PR #1
   Check run posted: conclusion=success, annotations=1
   ```

5. On the PR you should see:
   - 💬 A comment from AESTHENIXAI with score + issues
   - ✅ A check run in the Checks tab

## What happens when ngrok URL changes

ngrok free tier gives a new URL each restart. When that happens:

1. Get new URL: `curl http://127.0.0.1:4040/api/tunnels`
2. Update GitHub App webhook URL (step 1-7 above)

For a permanent URL, deploy to Render (see DEPLOY_TO_RENDER.md).

## Troubleshooting

### Webhook not received

- Check ngrok is running: `curl http://127.0.0.1:4040/api/tunnels`
- Check backend is running: `curl http://localhost:8082/api/health`
- Check GitHub App webhook URL is correct

### Signature verification failed

- Make sure webhook secret in GitHub App matches `PR_BOT_WEBHOOK_SECRET` in `.env`
- Never paste the secret in chat or logs — read it from the file directly

### No comment posted on PR

- The bot uses the GitHub App installation token for comments (no PAT required)
- Make sure the App is installed on the repo:
  https://github.com/settings/apps/aesthenixai → Install App
- Optionally add a fresh PAT to `.env` as `GITHUB_TOKEN` for fallback:
  1. Go to https://github.com/settings/tokens → Generate new token (classic)
  2. Select scopes: `repo`, `read:org`
  3. Update `GITHUB_TOKEN` in `.env` and restart backend

### Check run not appearing

- Requires GitHub App to be installed on the repo
- Go to: https://github.com/settings/apps/aesthenixai → Install App
- App needs `checks:write` and `pull_requests:read` permissions
