# Quick Start - GitHub App

## 🚀 Three Commands to Success

```bash
# 1. Test environment (verify .env is correct)
./test-env.sh

# 2. Restart everything (kills backend + ngrok)
./restart-all.sh

# 3. Start ngrok in NEW terminal
ngrok http 8080
```

Then:

- Copy ngrok URL
- Update GitHub webhook: https://github.com/settings/apps/aesthenixai
- Test: `git commit --allow-empty -m "test" && git push origin test-ai`

## ✅ Success Indicators

**Backend logs show:**

```
GitHubAppAuthService: initialized — appId=3349093
✓ Private key file exists: C:/ai-code-reviewe/...
```

**Webhook returns:**

```
200 OK
{"status":"check_suite queued"}
```

**PR shows:**

- ✅ Checks tab: AESTHENIXAI check run
- ✅ Conversation tab: Summary comment
- ✅ Files tab: Inline comments

## 🔧 If Something Fails

| Problem               | Solution                      |
| --------------------- | ----------------------------- |
| Empty GITHUB_APP_ID   | Run updated `./start.sh`      |
| Webhook 500           | Restart backend               |
| ngrok error           | `taskkill //F //IM ngrok.exe` |
| Private key not found | Check path in `.env`          |

## 📚 Full Documentation

- **GITHUB_APP_STARTUP.md** - Complete guide
- **FIXES_APPLIED.md** - What was fixed and why
- **ARCHITECTURE_GUIDE.md** - System architecture

## 🎯 Current Configuration

```
GitHub App ID: 3349093
Private Key: C:/ai-code-reviewe/ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
Webhook Secret: a3f9c8e1b7d4a6c9e2f1a3b5d6c7e8f9a0b1c2d3
Repo: efigma54-dev/AesthenixAi
Branch: test-ai
PR: #1
```

## 💡 Pro Tips

- Always run `./test-env.sh` before starting
- Use `./restart-all.sh` for clean restarts
- ngrok URL changes every restart → update webhook each time
- Check backend logs for "initialized — appId=3349093"
- Webhook should return 200, not 500

---

**Ready?** Run `./test-env.sh` now! 🚀
