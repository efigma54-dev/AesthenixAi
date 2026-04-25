# Deployment Guide

## Overview

This guide covers deploying the AI Code Reviewer to production on Render (backend) and Vercel (frontend).

## Prerequisites

- GitHub account with repository
- Render account (https://render.com)
- Vercel account (https://vercel.com)
- GitHub App created (ID: 3349093)
- Private key downloaded

## Backend Deployment (Render)

### Step 1: Connect Repository to Render

1. Go to https://render.com
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Select the repository containing the code

### Step 2: Configure Build Settings

**Name**: `ai-code-reviewer-backend`

**Environment**: `Docker`

**Build Command**: (leave default)

**Start Command**: (leave default)

### Step 3: Add Environment Variables

Click "Advanced" and add these environment variables:

| Variable                 | Value                              | Notes                                    |
| ------------------------ | ---------------------------------- | ---------------------------------------- |
| `GITHUB_APP_ID`          | `3349093`                          | Your GitHub App ID                       |
| `GITHUB_PRIVATE_KEY`     | (paste full PEM content)           | Full private key, not path               |
| `PR_BOT_WEBHOOK_SECRET`  | (from .env)                        | Webhook secret                           |
| `OLLAMA_URL`             | `http://127.0.0.1:11434`           | Local Ollama (or cloud)                  |
| `OLLAMA_MODEL`           | `qwen2.5-coder:7b`                 | Model name                               |
| `SPRING_PROFILES_ACTIVE` | `prod`                             | Production profile                       |
| `CORS_ORIGINS`           | `https://your-frontend.vercel.app` | Frontend URL                             |
| `SERVER_PORT`            | `8082`                             | (optional, Render assigns automatically) |

### Step 4: Deploy

Click "Create Web Service"

Render will:

1. Build the Docker image
2. Deploy the backend
3. Provide a URL (e.g., `https://ai-code-reviewer-backend.onrender.com`)

### Step 5: Verify Deployment

```bash
curl https://ai-code-reviewer-backend.onrender.com/api/health
# Expected: "AI Code Reviewer is running"
```

## Frontend Deployment (Vercel)

### Step 1: Connect Repository to Vercel

1. Go to https://vercel.com
2. Click "Add New..." → "Project"
3. Import your GitHub repository

### Step 2: Configure Build Settings

**Framework Preset**: `Vite`

**Root Directory**: `frontend-react`

**Build Command**: `npm run build`

**Output Directory**: `dist`

### Step 3: Add Environment Variables

Click "Environment Variables" and add:

| Variable       | Value                                               |
| -------------- | --------------------------------------------------- |
| `VITE_API_URL` | `https://ai-code-reviewer-backend.onrender.com/api` |

### Step 4: Deploy

Click "Deploy"

Vercel will:

1. Build the React app
2. Deploy to CDN
3. Provide a URL (e.g., `https://ai-code-reviewer.vercel.app`)

### Step 5: Verify Deployment

Open `https://ai-code-reviewer.vercel.app` in your browser

You should see the AI Code Reviewer UI.

## GitHub App Configuration

### Step 1: Update Webhook URL

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Edit" → "Webhook"
3. Update Payload URL to: `https://ai-code-reviewer-backend.onrender.com/api/webhooks/github`
4. Click "Save changes"

### Step 2: Verify Webhook Secret

Ensure `PR_BOT_WEBHOOK_SECRET` in Render matches the webhook secret in GitHub App settings.

### Step 3: Test Webhook

1. Go to: https://github.com/settings/apps/aesthenixai
2. Click "Webhooks"
3. Click "Recent Deliveries"
4. You should see successful deliveries (200 OK)

## Production Checklist

### Security

- [ ] GitHub private key stored as environment variable (not in code)
- [ ] Webhook secret configured and verified
- [ ] CORS restricted to frontend domain
- [ ] HTTPS enabled (automatic on Render/Vercel)
- [ ] Rate limiting enabled (10 req/min)

### Performance

- [ ] Caching enabled (Caffeine, 30 min TTL)
- [ ] Database connection pooling configured
- [ ] CDN enabled for frontend (automatic on Vercel)
- [ ] Compression enabled

### Monitoring

- [ ] Health check endpoint configured
- [ ] Logging enabled (SLF4J)
- [ ] Error tracking configured (optional: Sentry)
- [ ] Uptime monitoring configured (optional: UptimeRobot)

### Testing

- [ ] Backend health check passing
- [ ] Frontend loads without errors
- [ ] GitHub webhook working
- [ ] PR analysis working end-to-end

## Keep-Alive (Render Free Tier)

Render free tier instances spin down after 15 minutes of inactivity. To prevent this:

### Option 1: UptimeRobot (Recommended)

1. Go to https://uptimerobot.com
2. Create a new monitor
3. URL: `https://ai-code-reviewer-backend.onrender.com/api/ping`
4. Interval: 5 minutes
5. Save

### Option 2: GitHub Actions

Create `.github/workflows/keep-alive.yml`:

```yaml
name: Keep Alive

on:
  schedule:
    - cron: '*/5 * * * *'

jobs:
  ping:
    runs-on: ubuntu-latest
    steps:
      - name: Ping backend
        run: curl https://ai-code-reviewer-backend.onrender.com/api/ping
```

## Troubleshooting

### Backend not starting

**Check logs:**

1. Go to Render dashboard
2. Click on your service
3. Click "Logs"
4. Look for error messages

**Common issues:**

- Missing environment variables
- Private key format incorrect
- Port already in use

### Frontend not connecting to backend

**Check:**

1. `VITE_API_URL` is set correctly
2. Backend is running and accessible
3. CORS is configured correctly

**Test:**

```bash
curl https://ai-code-reviewer-backend.onrender.com/api/health
```

### Webhook not triggering

**Check:**

1. Webhook URL is correct in GitHub App settings
2. Webhook secret matches
3. GitHub App is installed on repository
4. Check webhook deliveries in GitHub App settings

## Scaling

### Horizontal Scaling

Render allows scaling to multiple instances:

1. Go to Render dashboard
2. Click on your service
3. Click "Settings"
4. Increase "Num Instances"

### Database Scaling

If adding a database:

1. Create PostgreSQL instance on Render
2. Add connection string to environment variables
3. Update Spring Boot configuration

### Caching

Current caching strategy:

- Caffeine LRU (200 entries, 30 min TTL)
- Can be upgraded to Redis for distributed caching

## Cost Optimization

### Render

- **Free tier**: $0/month (limited to 1 instance, spins down after 15 min)
- **Starter**: $7/month (always running)
- **Standard**: $12/month (better performance)

### Vercel

- **Free tier**: $0/month (unlimited deployments)
- **Pro**: $20/month (priority support)

### Ollama

- **Local**: $0/month (runs on your machine)
- **Cloud**: Varies by provider

## Monitoring

### Health Checks

```bash
# Backend health
curl https://ai-code-reviewer-backend.onrender.com/api/health

# Frontend health
curl https://ai-code-reviewer.vercel.app
```

### Metrics

```bash
# Backend metrics
curl https://ai-code-reviewer-backend.onrender.com/api/metrics
```

### Logs

**Render:**

1. Go to Render dashboard
2. Click on your service
3. Click "Logs"

**Vercel:**

1. Go to Vercel dashboard
2. Click on your project
3. Click "Deployments"
4. Click on a deployment
5. Click "Logs"

## Rollback

### Render

1. Go to Render dashboard
2. Click on your service
3. Click "Deployments"
4. Click on a previous deployment
5. Click "Redeploy"

### Vercel

1. Go to Vercel dashboard
2. Click on your project
3. Click "Deployments"
4. Click on a previous deployment
5. Click "Redeploy"

## Next Steps

1. ✅ Deploy backend to Render
2. ✅ Deploy frontend to Vercel
3. ✅ Update GitHub webhook URL
4. ✅ Test end-to-end
5. ✅ Set up monitoring
6. ✅ Configure keep-alive

## Support

For deployment issues:

1. Check Render/Vercel logs
2. Verify environment variables
3. Test health endpoints
4. Review GitHub webhook deliveries

---

**Status**: 🟢 Ready for production deployment
