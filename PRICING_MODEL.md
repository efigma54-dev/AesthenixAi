# AESTHENIXAI — Pricing Model

## ₹0 Launch Strategy

Start free, build trust, then monetize. This is how CodeRabbit, Codium AI, and
Sourcery all grew — free tier creates viral distribution through PR comments.

---

## Tier Structure

### Free — ₹0/month

**Target**: Individual developers, open-source projects

- 50 PR reviews/month
- Rule-based analysis (Java, JS, TS, Python)
- Quality score + inline annotations
- VS Code extension (unlimited)
- Public repos only

**Why free works**: Every PR comment says "Reviewed by AESTHENIXAI" — free marketing.

---

### Pro — ₹499/month (~$6)

**Target**: Individual developers who use it daily

- Unlimited PR reviews
- AI-powered analysis (Ollama)
- One-click fix suggestions in GitHub
- Private repos
- Custom score gate threshold
- Priority support

---

### Team — ₹1,999/month (~$24)

**Target**: Small teams (5–20 developers)

- Everything in Pro
- Up to 10 seats
- Team dashboard with score trends
- Per-repo analytics
- Slack/email notifications
- Custom rules

---

### Enterprise — Custom pricing

**Target**: Companies with 20+ developers

- Unlimited seats
- Self-hosted deployment
- Custom AI model (bring your own Ollama)
- SSO/SAML
- SLA + dedicated support
- Audit logs

---

## Cost Structure (₹0 stack)

| Component         | Tool                    | Cost |
| ----------------- | ----------------------- | ---- |
| Backend hosting   | Render free tier        | ₹0   |
| Frontend hosting  | Vercel free tier        | ₹0   |
| Database          | Supabase free tier      | ₹0   |
| AI                | Ollama (user's machine) | ₹0   |
| GitHub App        | GitHub                  | ₹0   |
| VS Code extension | Marketplace             | ₹0   |

**Total infrastructure cost at launch: ₹0**

---

## Growth Strategy

### Week 1–4: Free launch

1. Deploy backend on Render
2. Deploy frontend on Vercel
3. Publish VS Code extension
4. Submit GitHub Marketplace listing
5. Post on dev.to / Reddit / Twitter

### Month 2–3: Monetization

1. Add Stripe payment integration
2. Enable Pro tier
3. Add usage tracking (Supabase)
4. Email drip campaign to free users

### Month 4+: Scale

1. Team tier with dashboard
2. Enterprise outreach
3. Partner with dev tool newsletters

---

## Revenue Projections (conservative)

| Users | Free | Pro (10%) | Team (2%) | Monthly Revenue |
| ----- | ---- | --------- | --------- | --------------- |
| 100   | 88   | 10        | 2         | ₹8,990          |
| 500   | 440  | 50        | 10        | ₹44,990         |
| 1000  | 880  | 100       | 20        | ₹89,980         |
| 5000  | 4400 | 500       | 100       | ₹4,49,900       |

---

## Implementation Checklist

- [ ] Add `STRIPE_SECRET_KEY` to Render env vars
- [ ] Create Stripe products (Free, Pro, Team)
- [ ] Add `/api/billing/checkout` endpoint
- [ ] Add `/api/billing/webhook` for Stripe events
- [ ] Add `plan` field to user/org in Supabase
- [ ] Gate AI features behind Pro check
- [ ] Add usage counter (PR reviews/month) per user
- [ ] Add upgrade prompt when free limit hit

---

## Competitive Positioning

| Tool            | Price         | AI     | Self-hosted | Multi-lang  |
| --------------- | ------------- | ------ | ----------- | ----------- |
| CodeRabbit      | $12/user      | ✅     | ❌          | ✅          |
| Codium AI       | $19/user      | ✅     | ❌          | ✅          |
| Sourcery        | $14/user      | ✅     | ❌          | Python only |
| **AESTHENIXAI** | **₹499/user** | **✅** | **✅**      | **✅**      |

**Your edge**: Local AI (no data leaves your infra) + lowest price in the market.
