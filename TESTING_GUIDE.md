# Testing Guide

## Overview

This guide covers testing the AI Code Reviewer system at all levels: unit, integration, and end-to-end.

## Quick Start Testing

### Test 1: Backend Health

```bash
curl http://localhost:8082/api/health
# Expected: "AI Code Reviewer is running"
```

### Test 2: Code Review API

```bash
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Test { public void foo() { String s=\"\"; for(int i=0;i<100;i++){s=s+i;} } }"
  }'

# Expected response:
# {
#   "score": 100,
#   "issues": [...],
#   "suggestions": [...],
#   "improvedCode": "..."
# }
```

### Test 3: Frontend Connectivity

```bash
curl http://localhost:3003
# Expected: HTML response (frontend is serving)
```

## API Testing

### Test Suite

#### 1. Health Endpoints

```bash
# Health check
curl http://localhost:8082/api/health

# Ping (for uptime monitoring)
curl http://localhost:8082/api/ping
```

#### 2. Code Review

**Single file:**

```bash
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{
    "code": "public class Example { public void process() { String result = \"\"; for(int i=0; i<1000; i++) { result += i; } } }"
  }'
```

**Multiple files:**

```bash
curl -X POST http://localhost:8082/api/review/multi \
  -H "Content-Type: application/json" \
  -d '{
    "files": [
      {"name": "Test1.java", "content": "public class Test1 {}"},
      {"name": "Test2.java", "content": "public class Test2 {}"}
    ]
  }'
```

**GitHub repository:**

```bash
curl -X POST http://localhost:8082/api/review/github \
  -H "Content-Type: application/json" \
  -d '{
    "repoUrl": "https://github.com/owner/repo"
  }'
```

#### 3. Metrics

```bash
curl http://localhost:8082/api/metrics
```

Expected response:

```json
{
  "successCount": 5,
  "failureCount": 0,
  "totalTimeMs": 25000,
  "avgTimeMs": 5000,
  "successRate": 100,
  "cacheSize": 3
}
```

#### 4. Webhook

**Ping test:**

```bash
curl -X POST http://localhost:8082/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -d '{"zen":"Design for failure."}'

# Expected: {"message":"AESTHENIXAI webhook active"}
```

**Pull request event:**

```bash
curl -X POST http://localhost:8082/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -H "X-GitHub-Delivery: test-delivery-id" \
  -d '{
    "action": "opened",
    "pull_request": {
      "number": 1,
      "head": {"sha": "abc123"},
      "url": "https://api.github.com/repos/owner/repo/pulls/1"
    },
    "repository": {"full_name": "owner/repo"}
  }'

# Expected: {"status":"pull_request queued"}
```

## Frontend Testing

### Manual Testing

1. **Open frontend:**

   ```
   http://localhost:3003
   ```

2. **Test code editor:**
   - Paste Java code
   - Click "Analyze"
   - Verify results appear

3. **Test GitHub scanner:**
   - Enter GitHub repo URL
   - Click "Scan"
   - Verify files are analyzed

4. **Test history:**
   - Perform multiple analyses
   - Click "History"
   - Verify previous analyses are listed

### Browser DevTools Testing

1. Open DevTools (F12)
2. Go to "Console" tab
3. Check for errors
4. Go to "Network" tab
5. Verify API calls are successful (200 status)

## Integration Testing

### Test 1: End-to-End Flow

```bash
# 1. Start backend
cd ai-code-reviewer
mvn spring-boot:run

# 2. Start frontend (new terminal)
cd frontend-react
npm run dev

# 3. Open browser
# http://localhost:3003

# 4. Paste code and analyze
# Verify results appear

# 5. Check backend logs
# Verify analysis pipeline executed
```

### Test 2: GitHub App Integration

```bash
# 1. Ensure backend is running with GitHub App enabled
# Check logs for: "GitHubAppAuthService: initialized — appId=3349093"

# 2. Create test PR
# Push a branch with Java code changes

# 3. Create PR on GitHub
# Backend should automatically analyze

# 4. Verify Check Run appears
# Go to PR → Checks tab
# Should see "AESTHENIXAI Code Review"

# 5. Verify annotations appear
# Go to PR → Files tab
# Should see inline comments on changed lines
```

### Test 3: Caching

```bash
# 1. Analyze code
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{"code":"public class Test {}"}'

# Note the response time (e.g., 2000ms)

# 2. Analyze same code again
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{"code":"public class Test {}"}'

# Response should be much faster (e.g., <10ms)
# This indicates cache hit
```

### Test 4: Rate Limiting

```bash
# Send 11 requests in quick succession
for i in {1..11}; do
  curl -X POST http://localhost:8082/api/review \
    -H "Content-Type: application/json" \
    -d '{"code":"public class Test {}"}'
done

# 11th request should return 429 (Too Many Requests)
```

## Performance Testing

### Load Testing

Using Apache JMeter or similar:

1. **Setup:**
   - Target: `http://localhost:8082/api/review`
   - Method: POST
   - Concurrency: 10 threads
   - Duration: 60 seconds

2. **Expected results:**
   - Response time: < 5 seconds (avg)
   - Error rate: < 1%
   - Throughput: > 2 req/sec

### Stress Testing

1. **Increase concurrency to 50 threads**
2. **Expected results:**
   - System should handle gracefully
   - Response times may increase
   - No crashes or data loss

## Security Testing

### Test 1: Webhook Signature Verification

```bash
# Valid signature
curl -X POST http://localhost:8082/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=..." \
  -d '{"zen":"Design for failure."}'

# Expected: 200 OK

# Invalid signature
curl -X POST http://localhost:8082/api/webhooks/github \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: ping" \
  -H "X-Hub-Signature-256: sha256=invalid" \
  -d '{"zen":"Design for failure."}'

# Expected: 401 Unauthorized
```

### Test 2: Input Validation

```bash
# Empty code
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d '{"code":""}'

# Expected: 400 Bad Request

# Code too large (> 100KB)
# Expected: 400 Bad Request

# Invalid JSON
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -d 'invalid json'

# Expected: 400 Bad Request
```

### Test 3: CORS

```bash
# Request from different origin
curl -X POST http://localhost:8082/api/review \
  -H "Content-Type: application/json" \
  -H "Origin: https://example.com" \
  -d '{"code":"public class Test {}"}'

# Expected: 200 OK (CORS_ORIGINS=* allows all)
```

## Automated Testing

### Unit Tests

```bash
cd ai-code-reviewer
mvn test
```

### Integration Tests

```bash
cd ai-code-reviewer
mvn verify
```

### Frontend Tests

```bash
cd frontend-react
npm test
```

## Debugging

### Backend Debugging

**Enable debug logging:**

```bash
export LOGGING_LEVEL_COM_AICODE=DEBUG
mvn spring-boot:run
```

**Check specific service:**

```bash
# LocalAIService logs
grep "LocalAIService" logs/app.log

# PRReviewBotService logs
grep "PRReviewBotService" logs/app.log

# GitHub App logs
grep "GitHubAppAuthService" logs/app.log
```

### Frontend Debugging

**Browser DevTools:**

1. Open DevTools (F12)
2. Go to "Console" tab
3. Check for errors
4. Go to "Network" tab
5. Check API responses

**React DevTools:**

1. Install React DevTools extension
2. Inspect component state
3. Check props

## Test Scenarios

### Scenario 1: Simple Code Review

**Input:**

```java
public class Bad {
    public void process() {
        String result = "";
        for (int i = 0; i < 1000; i++) {
            result += i;
        }
    }
}
```

**Expected output:**

- Score: 60-80
- Issues: String concatenation in loop
- Suggestions: Use StringBuilder
- Improved code: Refactored version

### Scenario 2: Complex Code Review

**Input:**

```java
public class Complex {
    private String field1;
    private String field2;
    // ... 10+ more fields

    public void method1() { /* ... */ }
    public void method2() { /* ... */ }
    // ... 15+ more methods
}
```

**Expected output:**

- Score: 40-60
- Issues: God class, too many fields/methods
- Suggestions: Split into multiple classes

### Scenario 3: Good Code Review

**Input:**

```java
public class Good {
    private final String name;

    public Good(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
```

**Expected output:**

- Score: 90-100
- Issues: None or minimal
- Suggestions: None or minor improvements

## Continuous Integration

### GitHub Actions

Create `.github/workflows/test.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: cd ai-code-reviewer && mvn test

  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: '18'
      - run: cd frontend-react && npm install && npm test
```

## Test Results

### Expected Results

| Test              | Expected Result           |
| ----------------- | ------------------------- |
| Health check      | 200 OK                    |
| Code review       | 200 OK + analysis results |
| Webhook ping      | 200 OK + active message   |
| Webhook PR        | 200 OK + queued message   |
| Frontend load     | 200 OK + HTML             |
| Caching           | 2nd request < 10ms        |
| Rate limiting     | 11th request = 429        |
| Invalid signature | 401 Unauthorized          |
| Empty code        | 400 Bad Request           |

## Troubleshooting

### Test fails with "Connection refused"

**Cause:** Backend not running

**Solution:**

```bash
cd ai-code-reviewer
mvn spring-boot:run
```

### Test fails with "Ollama not responding"

**Cause:** Ollama not running

**Solution:**

```bash
ollama serve
```

### Test fails with "Model not found"

**Cause:** Model not installed

**Solution:**

```bash
ollama pull qwen2.5-coder:7b
```

### Frontend test fails with "API URL not set"

**Cause:** `.env` file missing

**Solution:**

```bash
cd frontend-react
echo "VITE_API_URL=http://localhost:8082/api" > .env
```

## Next Steps

1. ✅ Run quick start tests
2. ✅ Run API tests
3. ✅ Run integration tests
4. ✅ Run performance tests
5. ✅ Run security tests
6. ✅ Set up CI/CD

---

**Status**: 🟢 Ready for comprehensive testing
