# 🚀 Deploy Tonight — Step-by-Step Guide

**Time Required:** 90 minutes  
**Goal:** Get AESTHENIXAI live with high-impact features

---

## ⚡ Quick Start (Do This First)

### 1. Test Locally (15 minutes)

```bash
# Terminal 1: Start backend
cd ai-code-reviewer
./mvnw spring-boot:run

# Terminal 2: Start frontend
cd frontend-react
npm install
npm run dev

# Open browser: http://localhost:3000/app
```

**Test these features:**

- ✅ Run code analysis
- ✅ Click "Fix Safe Issues" button
- ✅ Click "Copy PR Summary" button
- ✅ Verify mode badge shows "🛡 Hybrid Mode"

---

## 🌐 Step 1: Deploy Backend to Render (30 minutes)

### A. Prepare for Deployment

```bash
cd ai-code-reviewer

# Ensure pom.xml has correct Java version
# Should be: <java.version>17</java.version>

# Test build
./mvnw clean package -DskipTests
```

### B. Deploy to Render

1. Go to https://render.com
2. Click "New +" → "Web Service"
3. Connect your GitHub repo
4. Configure:
   - **Name:** `aesthenixai-backend`
   - **Root Directory:** `ai-code-reviewer`
   - **Build Command:** `./mvnw clean package -DskipTests`
   - **Start Command:** `java -jar target/ai-code-reviewer-0.0.1-SNAPSHOT.jar`
   - **Instance Type:** Free

### C. Set Environment Variables

In Render dashboard, add these:

```
SPRING_PROFILES_ACTIVE=prod
GITHUB_APP_ID=<your-github-app-id>
GITHUB_PRIVATE_KEY=<your-private-key-pem-content>
CORS_ORIGINS=https://aesthenixai.vercel.app
```

**Optional (if using OpenAI instead of Ollama):**

```
OPENAI_API_KEY=<your-key>
AI_PROVIDER=openai
```

### D. Verify Deployment

```bash
# Check health
curl https://aesthenixai-backend.onrender.com/api/health

# Test safe fixes endpoint
curl -X POST https://aesthenixai-backend.onrender.com/api/fix/safe \
  -H "Content-Type: application/json" \
  -d '{"code":"var x = 1; console.log(x);"}'
```

---

## 🎨 Step 2: Deploy Frontend to Vercel (20 minutes)

### A. Prepare Frontend

```bash
cd frontend-react

# Update .env with production API URL
echo "VITE_API_URL=https://aesthenixai-backend.onrender.com/api" > .env.production

# Test build
npm run build
```

### B. Deploy to Vercel

1. Go to https://vercel.com
2. Click "Add New" → "Project"
3. Import your GitHub repo
4. Configure:
   - **Framework Preset:** Vite
   - **Root Directory:** `ai-code-reviewer/frontend-react`
   - **Build Command:** `npm run build`
   - **Output Directory:** `dist`

### C. Set Environment Variables

In Vercel dashboard:

```
VITE_API_URL=https://aesthenixai-backend.onrender.com/api
```

### D. Verify Deployment

1. Open https://aesthenixai.vercel.app
2. Navigate to `/app`
3. Test:
   - ✅ Code analysis works
   - ✅ Fix Safe Issues button works
   - ✅ Copy PR Summary works
   - ✅ Mode badge visible

---

## 🔗 Step 3: Configure GitHub App Webhook (10 minutes)

### Update Webhook URL

1. Go to https://github.com/settings/apps
2. Select your GitHub App
3. Update webhook URL:
   ```
   https://aesthenixai-backend.onrender.com/api/webhooks/github
   ```
4. Verify webhook secret matches your env var
5. Save changes

### Test Webhook

1. Open a test PR in a repo with your app installed
2. Check Render logs for webhook event
3. Verify PR comment appears
4. Check GitHub Checks tab for results

---

## 📹 Step 4: Create Demo GIF (20 minutes)

**This is your most valuable marketing asset.**

### Recording Flow (30-40 seconds)

1. **Open bad code** (5s)

   ```java
   public class Example {
       public String build(String[] words) {
           String result = "";
           for (int i = 0; i < words.length; i++) {
               result += words[i];
           }
           return result;
       }
   }
   ```

2. **Run analysis** (5s)
   - Click "Run Analysis"
   - Show loading state

3. **Issues appear** (5s)
   - Score: 65/100
   - Issues highlighted
   - Mode badge visible

4. **Click "Fix Safe Issues"** (5s)
   - Button click
   - Show fixes applied
   - Code updates

5. **Re-run analysis** (5s)
   - Score improves to 85/100
   - Fewer issues

6. **Copy PR Summary** (5s)
   - Click "Copy PR Summary"
   - Show "✓ Copied" feedback
   - Paste into fake PR comment

### Tools for Recording

**Option 1: ScreenToGif (Windows)**

```
https://www.screentogif.com/
```

**Option 2: Kap (Mac)**

```
https://getkap.co/
```

**Option 3: LICEcap (Cross-platform)**

```
https://www.cockos.com/licecap/
```

### GIF Settings

- **Resolution:** 1280x720
- **FPS:** 15-20
- **Duration:** 30-40 seconds
- **File size:** < 5MB

---

## 🎯 Step 5: Launch Checklist

### Pre-Launch Verification

- [ ] Backend health check passes
- [ ] Frontend loads without errors
- [ ] Code analysis works end-to-end
- [ ] Fix Safe Issues button works
- [ ] Copy PR Summary works
- [ ] Mode badge displays correctly
- [ ] GitHub webhook receives events
- [ ] PR comments post successfully
- [ ] Demo GIF recorded and optimized

### Launch Assets

- [ ] Demo GIF uploaded to repo
- [ ] README updated with GIF
- [ ] Screenshots taken
- [ ] Landing page updated
- [ ] Social media posts drafted

---

## 🐛 Troubleshooting

### Backend Issues

**Problem:** Health check fails

```bash
# Check Render logs
# Verify environment variables
# Ensure Java 17 is used
```

**Problem:** CORS errors

```bash
# Verify CORS_ORIGINS includes your Vercel domain
# Check browser console for exact error
```

**Problem:** Safe fixes endpoint 500 error

```bash
# Check Render logs for stack trace
# Verify SafeFixService is autowired correctly
# Test with simple code first
```

### Frontend Issues

**Problem:** API calls fail

```bash
# Verify VITE_API_URL is correct
# Check browser network tab
# Ensure backend is running
```

**Problem:** Copy to clipboard doesn't work

```bash
# Requires HTTPS (works on Vercel)
# Check browser console for errors
# Verify navigator.clipboard is available
```

**Problem:** Mode badge not showing

```bash
# Check browser console for import errors
# Verify ModeBadge.jsx exists
# Clear browser cache
```

---

## 📊 Post-Launch Monitoring

### First 24 Hours

**Watch these metrics:**

- Backend uptime (Render dashboard)
- Frontend errors (Vercel dashboard)
- API response times
- User feedback

**Check these endpoints:**

```bash
# Health
curl https://aesthenixai-backend.onrender.com/api/health

# Metrics
curl https://aesthenixai-backend.onrender.com/api/metrics

# Dashboard
curl https://aesthenixai-backend.onrender.com/api/dashboard
```

### Cold Start Handling

**Render free tier sleeps after 15 min inactivity.**

**Solution:** Set up UptimeRobot

1. Go to https://uptimerobot.com
2. Add monitor:
   - **Type:** HTTP(s)
   - **URL:** `https://aesthenixai-backend.onrender.com/api/health/ping`
   - **Interval:** 5 minutes
3. This keeps your backend warm

---

## 🎉 Success Criteria

### Day 1 Goals

- [ ] 10+ code analyses run
- [ ] 5+ "Fix Safe Issues" clicks
- [ ] 3+ PR summaries copied
- [ ] 0 critical errors
- [ ] < 5s average response time

### Week 1 Goals

- [ ] 50+ unique users
- [ ] 200+ analyses
- [ ] 10+ GitHub stars
- [ ] 5+ pieces of feedback
- [ ] 70%+ thumbs up ratio

---

## 🚀 You're Ready!

**Everything is implemented and tested.**

**Next steps:**

1. ✅ Deploy backend (30 min)
2. ✅ Deploy frontend (20 min)
3. ✅ Configure webhook (10 min)
4. ✅ Create demo GIF (20 min)
5. ✅ Launch! (10 min)

**Total time:** 90 minutes

**Then:**

- Share on Twitter/LinkedIn
- Post in relevant communities
- Get feedback
- Iterate based on real usage

---

**You've got this! 🚀**

The product is ready. The features are high-impact. The deployment is straightforward.

**Now go launch.** 🎯
