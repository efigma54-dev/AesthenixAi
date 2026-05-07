# 📢 Distribution Playbook

> **Goal:** Get from 0 → 100 users in 30 days

---

## 🎯 Distribution Strategy Overview

```
Week 1: Personal Network + VS Code Marketplace → 10 users
Week 2: Reddit + Dev.to → 30 users
Week 3: Hacker News + Product Hunt → 50 users
Week 4: Optimization + Retention → 100 users
```

---

## 📅 Week 1: Foundation (0 → 10 users)

### Day 1: VS Code Marketplace

**Publish Extension:**

```bash
# Install vsce
npm install -g @vscode/vsce

# Package extension
cd vscode-extension
vsce package

# Publish (requires Azure DevOps account)
vsce publish
```

**Marketplace Listing Optimization:**

**Title:**

```
AESTHENIXAI - PR Quality Assistant
```

**Short Description (80 chars):**

```
Catch risky code before review. AI-powered analysis in < 5 seconds.
```

**Long Description:**

```
AESTHENIXAI reviews Java, JavaScript, TypeScript, and Python code directly in VS Code.

✅ Instant feedback (< 5 seconds)
✅ Inline issue highlights
✅ AI-powered suggestions
✅ GitHub PR integration
✅ Quality scores and metrics

Perfect for developers who want to ship cleaner pull requests.

Features:
• Hybrid static + AI analysis
• Support for Java, JS, TS, Python
• Inline code decorations
• Problems panel integration
• One-click fixes
• GitHub Check annotations
• Quality gate enforcement

Free during early access. No signup required.
```

**Tags:**

```
code-review, ai, quality, github, pr, static-analysis, linter, java, javascript, typescript, python
```

**Categories:**

```
- Linters
- Programming Languages
- Other
```

**Screenshots:**

1. VS Code analysis view
2. Problems panel
3. GitHub PR review
4. Score dashboard
5. Diff view

**Demo GIF:**

- Upload your 30-second demo GIF
- Shows: open file → analyze → see results → apply fix

### Day 1-2: Personal Network

**LinkedIn Post:**

```
🚀 Just launched AESTHENIXAI — a PR quality assistant for developers

After months of building, I'm excited to share this with the dev community.

What it does:
✅ Reviews code in < 5 seconds
✅ Works in VS Code + GitHub
✅ Supports Java, JS, TS, Python
✅ Free during early access

Why I built it:
Existing code review tools are either slow or expensive. I wanted something fast, local, and affordable.

Tech stack:
• Backend: Java + Spring Boot
• Frontend: React + Vite
• AI: Ollama (local, no API costs)
• Static analysis: JavaParser

Try it: [VS Code Extension Link]
GitHub: [Repo Link]

Would love your feedback! 🙏

#SoftwareEngineering #CodeReview #AI #OpenSource #VSCode
```

**Twitter/X Thread:**

```
1/ 🚀 Launched AESTHENIXAI today — a PR quality assistant for developers

Analyzes Java, JS, TS, Python in < 5 seconds
Works in VS Code + GitHub
Free during early access

[Demo GIF]

2/ Why I built this:

❌ Existing tools are slow/expensive
❌ PRs sit for days waiting for review
❌ Issues get missed until production

✅ Wanted: fast, local, affordable

3/ How it works:

• Hybrid static + AI analysis
• JavaParser for rule-based checks
• Ollama for context-aware suggestions
• GitHub Checks API for PR annotations

No cloud dependency. Runs locally.

4/ Tech stack:

Backend: Java + Spring Boot
Frontend: React + Vite
Extension: TypeScript
AI: Ollama (qwen3.5:9b)
Static: JavaParser

All open source 🎉

5/ Features:

⚡ Instant feedback (< 5 seconds)
🎯 Inline issue highlights
🤖 AI-powered suggestions
🔀 GitHub PR reviews
📊 Quality scores
🛡 Fail-safe mode (rules-only fallback)

6/ Try it:

VS Code: [link]
GitHub App: [link]
Repo: [link]
Live demo: [link]

Would love your feedback! 🙏

What features would you want to see?
```

**Dev Discord Servers:**

Find servers for:

- Java developers
- JavaScript developers
- VS Code users
- Open source projects

**Message template:**

```
Hey everyone! 👋

I just launched a VS Code extension for automated code review. Would love feedback from this community.

AESTHENIXAI:
• Reviews Java, JS, TS, Python
• < 5 seconds analysis
• Inline highlights + suggestions
• GitHub PR integration
• Free during early access

Built it because existing tools were slow/expensive.

VS Code: [link]
GitHub: [link]

Happy to answer any questions! 🙏
```

### Day 3-7: Engage + Iterate

**Daily tasks:**

- [ ] Respond to every comment/question
- [ ] Fix critical bugs immediately
- [ ] Track metrics (installs, stars, feedback)
- [ ] Improve onboarding based on feedback
- [ ] Thank early users personally

**Metrics to track:**

```
Day 1: ___ installs, ___ stars
Day 2: ___ installs, ___ stars
Day 3: ___ installs, ___ stars
Day 7: ___ installs, ___ stars

Goal: 10 installs, 5 stars by end of week
```

---

## 📅 Week 2: Scale (10 → 30 users)

### Reddit Strategy

**Target Subreddits:**

1. **r/programming** (6M members)
   - Best day: Tuesday/Wednesday
   - Best time: 8-10 AM EST
   - Flair: "Project"

2. **r/webdev** (1.5M members)
   - Best day: Monday/Tuesday
   - Best time: 9-11 AM EST
   - Flair: "Showoff Saturday" (post on Saturday)

3. **r/java** (200K members)
   - Best day: Any weekday
   - Best time: 9 AM - 12 PM EST
   - Flair: "Project"

4. **r/javascript** (2.5M members)
   - Best day: Tuesday/Wednesday
   - Best time: 8-10 AM EST
   - Flair: "Showoff"

5. **r/vscode** (100K members)
   - Best day: Any weekday
   - Best time: 9 AM - 12 PM EST
   - Flair: "Extension"

**Post Template:**

```
Title: Built a lightweight AI PR reviewer for VS Code + GitHub

Body:
I built AESTHENIXAI because existing code review tools were either slow or expensive.

Main features:
• Fast local analysis (< 5 seconds)
• Hybrid static + AI (JavaParser + Ollama)
• VS Code extension with inline highlights
• GitHub App with automatic PR reviews
• Supports Java, JS, TS, Python

Tech stack:
• Backend: Java + Spring Boot
• Frontend: React + Vite
• AI: Ollama (local, no API costs)
• Static analysis: JavaParser

[Demo GIF]

It's free during early access. Would love honest feedback from developers using it on real repos.

Links:
• VS Code Extension: [link]
• GitHub: [link]
• Live demo: [link]

Happy to answer any technical questions!
```

**Reddit Rules:**

- ❌ Don't post to all subreddits at once (looks like spam)
- ✅ Space out posts (1-2 days apart)
- ❌ Don't delete and repost (gets you banned)
- ✅ Engage with every comment
- ❌ Don't be defensive about criticism
- ✅ Thank people for feedback
- ❌ Don't ask for upvotes
- ✅ Post during peak hours (8-10 AM EST)

### Dev.to Article

**Title:**

```
How I Built an AI PR Reviewer with Java + Ollama + VS Code
```

**Outline:**

```
1. Introduction
   - Problem: slow/expensive code review
   - Solution: fast, local, affordable

2. Architecture
   - Spring Boot backend
   - JavaParser for static analysis
   - Ollama for AI analysis
   - GitHub Checks API integration

3. Technical Challenges
   - Diff-based analysis (only changed lines)
   - Annotation batching (GitHub 50-limit)
   - Hybrid scoring (static + AI)
   - Rate limiting + caching

4. Results
   - < 5 second analysis
   - GitHub PR integration
   - VS Code extension
   - Free during early access

5. Lessons Learned
   - What worked
   - What didn't
   - What I'd do differently

6. Try It
   - Links to extension, GitHub, demo
   - Call to action
```

**Article Structure:**

```markdown
# How I Built an AI PR Reviewer with Java + Ollama + VS Code

[Hero image: screenshot of VS Code with analysis]

## The Problem

Code review is slow. PRs sit for days. Issues get missed until production.

I wanted a tool that:

- Gives instant feedback (< 5 seconds)
- Works locally (no cloud dependency)
- Costs nothing (no API fees)
- Integrates with my workflow (VS Code + GitHub)

Existing tools were either slow or expensive. So I built AESTHENIXAI.

## The Solution

[Architecture diagram]

AESTHENIXAI is a hybrid static + AI code reviewer that:

1. Analyzes code in < 5 seconds
2. Works in VS Code + GitHub
3. Supports Java, JS, TS, Python
4. Runs locally (no cloud costs)

## Architecture

### Backend: Spring Boot + JavaParser + Ollama

[Code snippet: analysis pipeline]

The backend has 5 stages:

1. **Preprocess** — parse code with JavaParser
2. **Rule Engine** — static analysis (nested loops, long methods, etc.)
3. **AI Engine** — Ollama (qwen3.5:9b) for context-aware suggestions
4. **PostProcessor** — merge + deduplicate issues
5. **Scoring** — blend static + AI scores

### Frontend: React + Monaco Editor

[Code snippet: Monaco integration]

The frontend uses Monaco Editor (same as VS Code) with:

- Inline issue decorations
- Hover tooltips
- Problems panel
- Diff view

### VS Code Extension: TypeScript

[Code snippet: extension activation]

The extension provides:

- Right-click → Analyze
- Inline highlights
- Problems panel integration
- Code actions (quick fixes)

### GitHub Integration: Checks API

[Code snippet: GitHub webhook handler]

When a PR is opened:

1. Webhook → backend
2. Fetch diff (only changed lines)
3. Run analysis
4. Post Check annotations
5. Complete Check run (pass/fail)

## Technical Challenges

### 1. Diff-based Analysis

[Code snippet: diff parsing]

GitHub's unified diff format:
```

@@ -10,5 +10,6 @@
public void method() {

- String result = "";
  for (User u : users) {

````

I parse this to extract added line numbers, then only analyze those lines.

### 2. Annotation Batching

[Code snippet: batching logic]

GitHub limits 50 annotations per request. I batch them:
```java
List<List<Annotation>> batches = Lists.partition(annotations, 50);
for (List<Annotation> batch : batches) {
    github.createCheckRun(batch);
}
````

### 3. Hybrid Scoring

[Code snippet: scoring formula]

```java
double finalScore = (aiScore * 0.6) + (staticScore * 0.4);
```

This balances AI creativity with rule reliability.

## Results

After 3 months of building:

- ✅ < 5 second analysis
- ✅ VS Code extension published
- ✅ GitHub App deployed
- ✅ 10+ early users
- ✅ Free during early access

[Demo GIF]

## Lessons Learned

### What Worked

- Local AI (Ollama) — no API costs, no rate limits
- Diff-based analysis — faster, more relevant
- GitHub Checks API — better UX than comments

### What Didn't

- Full-file analysis — too slow
- Comment-based reviews — too spammy
- OpenAI API — too expensive for free tier

### What I'd Do Differently

- Start with MVP (just rules, no AI)
- Get feedback earlier
- Focus on one language first (Java)

## Try It

Want to ship cleaner PRs?

- **VS Code Extension:** [link]
- **GitHub App:** [link]
- **Live Demo:** [link]
- **GitHub Repo:** [link]

Free during early access. No signup required.

Would love your feedback! 🙏

---

_Built with Java + Spring Boot + JavaParser + Ollama + React + TypeScript_

```

**Publishing:**
- Post on Dev.to
- Cross-post to Hashnode
- Share on LinkedIn
- Share on Twitter
- Submit to HackerNoon (if accepted)

### Metrics to Track

```

Week 2 Goals:

- 30 extension installs
- 15 GitHub stars
- 10 active users
- 1 testimonial
- 500+ article views

```

---

## 📅 Week 3: Amplify (30 → 50 users)

### Hacker News (Show HN)

**Best time to post:**
- Tuesday-Thursday
- 8-10 AM EST
- Avoid weekends

**Title format:**
```

Show HN: AESTHENIXAI – PR quality assistant for VS Code + GitHub

```

**Post template:**
```

Hi HN! 👋

I built AESTHENIXAI — a PR quality assistant that reviews code in < 5 seconds.

Why I built it:
Existing code review tools are either slow (SonarQube) or expensive (CodeClimate). I wanted something fast, local, and affordable.

How it works:
• Hybrid static + AI analysis
• JavaParser for rules (nested loops, long methods, etc.)
• Ollama for context-aware suggestions
• GitHub Checks API for PR annotations
• VS Code extension for inline highlights

Tech stack:
• Backend: Java + Spring Boot
• Frontend: React + Vite
• AI: Ollama (qwen3.5:9b, runs locally)
• Static: JavaParser

It's free during early access. No signup required.

Links:
• VS Code Extension: [link]
• GitHub: [link]
• Live demo: [link]

Would love feedback from the HN community! What features would you want to see?

```

**HN Rules:**
- ❌ Don't ask for upvotes
- ✅ Respond to every comment
- ❌ Don't be defensive
- ✅ Be humble and open to feedback
- ❌ Don't repost if it doesn't get traction
- ✅ Engage for at least 2-3 hours after posting

### Product Hunt Launch

**Preparation (1 week before):**
- [ ] Create Product Hunt account
- [ ] Build hunter network (ask friends to follow)
- [ ] Prepare assets (logo, screenshots, demo video)
- [ ] Write tagline and description
- [ ] Schedule launch for Tuesday/Wednesday

**Tagline (60 chars):**
```

Ship cleaner PRs with AI-powered code review in < 5 seconds

```

**Description:**
```

AESTHENIXAI is a PR quality assistant for developers.

✅ Instant feedback (< 5 seconds)
✅ Works in VS Code + GitHub
✅ Supports Java, JS, TS, Python
✅ Free during early access

Perfect for developers who want to catch risky code before review.

Features:
• Hybrid static + AI analysis
• Inline issue highlights
• One-click fixes
• GitHub PR annotations
• Quality scores and metrics
• Fail-safe mode (rules-only fallback)

Built with Java + Spring Boot + JavaParser + Ollama + React.

```

**Launch day checklist:**
- [ ] Post at 12:01 AM PST (first in queue)
- [ ] Share on Twitter, LinkedIn, Reddit
- [ ] Ask friends to upvote + comment
- [ ] Respond to every comment
- [ ] Update with "Product of the Day" badge if you win

### Metrics to Track

```

Week 3 Goals:

- 50 extension installs
- 30 GitHub stars
- 25 active users
- 3 testimonials
- 100+ Product Hunt upvotes
- 50+ HN points

```

---

## 📅 Week 4: Optimize (50 → 100 users)

### Focus on Retention

**Analyze drop-off points:**
```

Funnel:
Landing page → 1000 visitors
Extension install → 100 installs (10% conversion)
First analysis → 50 users (50% activation)
Second analysis → 25 users (50% retention)
Weekly active → 15 users (30% retention)

```

**Improve each stage:**
1. **Landing → Install:** Better demo GIF, clearer CTA
2. **Install → First analysis:** Onboarding tutorial
3. **First → Second:** Email reminder, in-app prompt
4. **Weekly active:** Add value (new features, better AI)

### Community Building

**GitHub Discussions:**
- [ ] Enable Discussions on repo
- [ ] Create categories: General, Ideas, Q&A, Show and Tell
- [ ] Post weekly updates
- [ ] Highlight user contributions
- [ ] Share roadmap

**Discord Server (optional):**
- [ ] Create server
- [ ] Channels: #general, #support, #feature-requests, #showcase
- [ ] Invite early users
- [ ] Post updates
- [ ] Host office hours

### Content Marketing

**Blog posts to write:**
1. "5 Code Smells AESTHENIXAI Catches Instantly"
2. "How to Set Up GitHub PR Quality Gates"
3. "Static Analysis vs AI: Which is Better?"
4. "Building a VS Code Extension: Lessons Learned"
5. "How We Analyze 1000+ Lines of Code in < 5 Seconds"

**Where to publish:**
- Your own blog (if you have one)
- Dev.to
- Hashnode
- Medium
- HackerNoon

### Metrics to Track

```

Week 4 Goals:

- 100 extension installs
- 50 GitHub stars
- 50 active users
- 10 testimonials
- 30% week-over-week retention
- 1000+ landing page visits

```

---

## 📊 Analytics Setup

### Track These Metrics

**Acquisition:**
- Landing page visits (Google Analytics)
- Extension installs (VS Code Marketplace)
- GitHub stars (GitHub API)
- Referral sources (where users come from)

**Activation:**
- First analysis completed
- Time to first analysis
- Issues found in first analysis

**Retention:**
- Daily active users
- Weekly active users
- Monthly active users
- Churn rate

**Engagement:**
- Analyses per user
- Issues fixed per user
- GitHub PRs reviewed
- Thumbs up/down ratio

**Growth:**
- Week-over-week growth rate
- Viral coefficient (how many users invite others)
- Net Promoter Score (NPS)

### Tools to Use

**Free:**
- Google Analytics (landing page)
- VS Code Marketplace analytics (installs)
- GitHub Insights (stars, forks, traffic)
- Plausible Analytics (privacy-friendly alternative)

**Paid (later):**
- Mixpanel (user behavior)
- Amplitude (product analytics)
- Hotjar (heatmaps, recordings)

---

## 🎯 Channel-Specific Tips

### VS Code Marketplace
- ✅ Update regularly (shows activity)
- ✅ Respond to reviews
- ✅ Add "What's New" section
- ✅ Use all 10 screenshot slots
- ✅ Optimize for search (tags, description)

### GitHub
- ✅ Pin important issues
- ✅ Use issue templates
- ✅ Add "good first issue" labels
- ✅ Respond to issues within 24 hours
- ✅ Celebrate contributors

### Reddit
- ✅ Post during peak hours (8-10 AM EST)
- ✅ Use relevant flair
- ✅ Engage with comments
- ✅ Don't delete and repost
- ✅ Be humble and open to feedback

### Twitter/X
- ✅ Use hashtags (#coding, #vscode, #ai)
- ✅ Tag relevant accounts (@code, @github)
- ✅ Post demo GIFs (high engagement)
- ✅ Engage with replies
- ✅ Retweet user testimonials

### LinkedIn
- ✅ Use professional tone
- ✅ Add relevant hashtags
- ✅ Tag your university/company (if relevant)
- ✅ Post during work hours (9 AM - 5 PM)
- ✅ Engage with comments

### Hacker News
- ✅ Post Tuesday-Thursday, 8-10 AM EST
- ✅ Use "Show HN:" prefix
- ✅ Respond to every comment
- ✅ Be humble and technical
- ✅ Don't ask for upvotes

### Product Hunt
- ✅ Launch Tuesday-Wednesday
- ✅ Post at 12:01 AM PST
- ✅ Prepare assets in advance
- ✅ Ask friends to upvote + comment
- ✅ Respond to every comment

---

## 🚫 What NOT to Do

### ❌ Avoid These Mistakes

1. **Spamming:**
   - Don't post to every subreddit at once
   - Don't DM people unsolicited
   - Don't comment on unrelated posts with your link

2. **Being defensive:**
   - Don't argue with criticism
   - Don't dismiss feedback
   - Don't take it personally

3. **Overpromising:**
   - Don't claim to be "better than X"
   - Don't use hyperbole ("revolutionary", "best-ever")
   - Don't promise features you haven't built

4. **Ignoring feedback:**
   - Don't ghost users
   - Don't ignore bug reports
   - Don't dismiss feature requests

5. **Focusing only on acquisition:**
   - Don't neglect retention
   - Don't ignore churn
   - Don't forget about existing users

---

## ✅ Success Checklist

### Week 1
- [ ] Published VS Code extension
- [ ] Posted to LinkedIn
- [ ] Posted to Twitter
- [ ] Shared in 3 Discord servers
- [ ] Got first 10 users
- [ ] Collected feedback

### Week 2
- [ ] Posted to 3 subreddits
- [ ] Published Dev.to article
- [ ] Responded to all comments
- [ ] Fixed critical bugs
- [ ] Got 30 users
- [ ] Got 1 testimonial

### Week 3
- [ ] Posted to Hacker News
- [ ] Launched on Product Hunt
- [ ] Shared on all social media
- [ ] Engaged with community
- [ ] Got 50 users
- [ ] Got 3 testimonials

### Week 4
- [ ] Analyzed retention metrics
- [ ] Improved onboarding
- [ ] Built community (Discussions/Discord)
- [ ] Published 2 blog posts
- [ ] Got 100 users
- [ ] Got 10 testimonials

---

## 🎯 Next Steps

**Today:**
1. [ ] Publish VS Code extension
2. [ ] Post to LinkedIn
3. [ ] Post to Twitter

**This Week:**
1. [ ] Post to Reddit (3 subreddits)
2. [ ] Write Dev.to article
3. [ ] Get first 10 users

**Next Week:**
1. [ ] Post to Hacker News
2. [ ] Launch on Product Hunt
3. [ ] Get to 50 users

**This Month:**
1. [ ] Build community
2. [ ] Improve retention
3. [ ] Get to 100 users

---

**Remember:** Distribution is a marathon, not a sprint. Focus on building genuine relationships with users. Respond to every piece of feedback. Iterate quickly. The users will come. 🚀
```
