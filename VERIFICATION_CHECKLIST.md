# Verification Checklist

Use this checklist to verify that the AI Code Reviewer system is fully operational.

## ✅ System Running

- [ ] Backend running on port 8082

  ```bash
  curl http://localhost:8082/api/health
  # Expected: "AI Code Reviewer is running"
  ```

- [ ] Frontend running on port 3003

  ```bash
  curl http://localhost:3003 | head -5
  # Expected: HTML response
  ```

- [ ] Ollama running with model installed
  ```bash
  ollama list
  # Expected: qwen2.5-coder:7b listed
  ```

## ✅ Backend Configuration

- [ ] Ollama URL is 127.0.0.1:11434 (not localhost)

  ```bash
  grep "OLLAMA_URL" ai-code-reviewer/.env
  # Expected: OLLAMA_URL=http://127.0.0.1:11434
  ```

- [ ] Model is qwen2.5-coder:7b

  ```bash
  grep "OLLAMA_MODEL" ai-code-reviewer/.env
  # Expected: OLLAMA_MODEL=qwen2.5-coder:7b
  ```

- [ ] Server port is 8082

  ```bash
  grep "SERVER_PORT" ai-code-reviewer/.env
  # Expected: SERVER_PORT=8082 (or not set, defaults to 8082)
  ```

- [ ] GitHub App ID configured

  ```bash
  grep "GITHUB_APP_ID" ai-code-reviewer/.env
  # Expected: GITHUB_APP_ID=3349093
  ```

- [ ] Private key file exists
  ```bash
  ls -la ai-code-reviewer/aesthenixai.2026-04-21.private-key.pem
  # Expected: File exists
  ```

## ✅ Frontend Configuration

- [ ] API URL is correct
  ```bash
  cat ai-code-reviewer/frontend-react/.env
  # Expected: VITE_API_URL=http://localhost:8082/api
  ```

## ✅ API Endpoints

- [ ] Health endpoint

  ```bash
  curl http://localhost:8082/api/health
  # Expected: "AI Code Reviewer is running"
  ```

- [ ] Ping endpoint

  ```bash
  curl http://localhost:8082/api/ping
  # Expected: JSON response
  ```

- [ ] Webhook endpoint (GET)

  ```bash
  curl http://localhost:8082/api/webhooks/github
  # Expected: JSON with status "active"
  ```

- [ ] Webhook endpoint (POST - ping)
  ```bash
  curl -X POST http://localhost:8082/api/webhooks/github \
    -H "Content-Type: application/json" \
    -H "X-GitHub-Event: ping" \
    -d '{"zen":"Design for failure."}'
  # Expected: {"message":"AESTHENIXAI webhook active"}
  ```

## ✅ Code Review API

- [ ] Single file review

  ```bash
  curl -X POST http://localhost:8082/api/review \
    -H "Content-Type: application/json" \
    -d '{"code":"public class Test { public void foo() { String s=\"\"; for(int i=0;i<100;i++){s=s+i;} } }"}'
  # Expected: JSON with score, issues, suggestions, improvedCode
  ```

- [ ] Response contains required fields

  ```bash
  # Check response has: score, issues, suggestions, improvedCode
  ```

- [ ] AI suggestions are present
  ```bash
  # Check that suggestions array is not empty
  ```

## ✅ Frontend UI

- [ ] Frontend loads without errors

  ```bash
  # Open http://localhost:3003 in browser
  # Expected: UI loads, no console errors
  ```

- [ ] Code editor is visible

  ```bash
  # Expected: Monaco editor visible
  ```

- [ ] Analyze button is visible

  ```bash
  # Expected: "Analyze" button visible
  ```

- [ ] Can paste code and analyze

  ```bash
  # Paste code, click Analyze
  # Expected: Results appear
  ```

- [ ] Results display correctly
  ```bash
  # Expected: Score, issues, suggestions visible
  ```

## ✅ GitHub App Integration

- [ ] GitHub App is initialized

  ```bash
  # Check backend logs for:
  # "GitHubAppAuthService: initialized — appId=3349093"
  ```

- [ ] Private key is loaded

  ```bash
  # Check backend logs for:
  # "Loading GitHub App private key from file"
  ```

- [ ] Webhook secret is configured
  ```bash
  grep "PR_BOT_WEBHOOK_SECRET" ai-code-reviewer/.env
  # Expected: Secret is set
  ```

## ✅ Performance

- [ ] First analysis completes in < 10 seconds

  ```bash
  time curl -X POST http://localhost:8082/api/review \
    -H "Content-Type: application/json" \
    -d '{"code":"public class Test {}"}'
  # Expected: real < 10s
  ```

- [ ] Cached analysis completes in < 100ms

  ```bash
  # Run same code twice, second should be much faster
  ```

- [ ] Metrics endpoint works
  ```bash
  curl http://localhost:8082/api/metrics
  # Expected: JSON with successCount, failureCount, etc.
  ```

## ✅ Caching

- [ ] Cache is working

  ```bash
  # Analyze same code twice
  # Second response should be < 100ms
  ```

- [ ] Cache size increases
  ```bash
  curl http://localhost:8082/api/metrics | grep cacheSize
  # Expected: cacheSize > 0
  ```

## ✅ Error Handling

- [ ] Empty code returns error

  ```bash
  curl -X POST http://localhost:8082/api/review \
    -H "Content-Type: application/json" \
    -d '{"code":""}'
  # Expected: 400 Bad Request
  ```

- [ ] Invalid JSON returns error
  ```bash
  curl -X POST http://localhost:8082/api/review \
    -H "Content-Type: application/json" \
    -d 'invalid json'
  # Expected: 400 Bad Request
  ```

## ✅ Logging

- [ ] Backend logs are visible

  ```bash
  # Check terminal where mvn spring-boot:run is running
  # Expected: Logs visible for each request
  ```

- [ ] Request IDs are logged

  ```bash
  # Check logs for [request-id] pattern
  ```

- [ ] AI response is logged
  ```bash
  # Check logs for "OLLAMA RAW RESPONSE"
  ```

## ✅ Documentation

- [ ] README.md exists and is readable

  ```bash
  cat ai-code-reviewer/README.md | head -20
  ```

- [ ] SYSTEM_STATUS.md exists

  ```bash
  ls -la ai-code-reviewer/SYSTEM_STATUS.md
  ```

- [ ] QUICK_ACCESS.md exists

  ```bash
  ls -la ai-code-reviewer/QUICK_ACCESS.md
  ```

- [ ] GITHUB_APP_INTEGRATION.md exists

  ```bash
  ls -la ai-code-reviewer/GITHUB_APP_INTEGRATION.md
  ```

- [ ] DEPLOYMENT_GUIDE.md exists

  ```bash
  ls -la ai-code-reviewer/DEPLOYMENT_GUIDE.md
  ```

- [ ] TESTING_GUIDE.md exists
  ```bash
  ls -la ai-code-reviewer/TESTING_GUIDE.md
  ```

## ✅ Build Status

- [ ] Backend compiles without errors

  ```bash
  cd ai-code-reviewer
  mvn clean compile
  # Expected: BUILD SUCCESS
  ```

- [ ] Frontend builds without errors
  ```bash
  cd ai-code-reviewer/frontend-react
  npm run build
  # Expected: ✓ built in X.XXs
  ```

## ✅ Environment

- [ ] .env file exists

  ```bash
  ls -la ai-code-reviewer/.env
  ```

- [ ] All required variables are set

  ```bash
  grep -E "GITHUB_APP_ID|OLLAMA_URL|OLLAMA_MODEL" ai-code-reviewer/.env
  # Expected: All three present
  ```

- [ ] No secrets in code
  ```bash
  grep -r "ghp_" ai-code-reviewer/src/
  # Expected: No results (secrets should be in .env)
  ```

## ✅ Ports

- [ ] Port 8082 is available

  ```bash
  netstat -ano | grep 8082
  # Expected: Only backend process
  ```

- [ ] Port 3003 is available

  ```bash
  netstat -ano | grep 3003
  # Expected: Only frontend process
  ```

- [ ] Port 11434 is available
  ```bash
  netstat -ano | grep 11434
  # Expected: Ollama process
  ```

## ✅ Final Verification

- [ ] All checks above passed
- [ ] System is ready for use
- [ ] System is ready for deployment
- [ ] Documentation is complete

## 🎉 Success!

If all checks above are passing, your AI Code Reviewer system is:

✅ **Fully Operational**  
✅ **Production Ready**  
✅ **Ready for Deployment**

---

## Troubleshooting

If any check fails, refer to:

1. **System Status**: `SYSTEM_STATUS.md`
2. **Quick Access**: `QUICK_ACCESS.md`
3. **GitHub App Integration**: `GITHUB_APP_INTEGRATION.md`
4. **Testing Guide**: `TESTING_GUIDE.md`

---

**Last Verified**: April 25, 2026  
**Status**: 🟢 All systems operational
