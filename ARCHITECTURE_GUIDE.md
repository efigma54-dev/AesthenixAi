# AESTHENIX AI — Ultra-Pro Code Analysis System

## 🎯 Executive Summary

Transformed the codebase from a basic static analyzer into a **production-grade, modular analysis pipeline** with:

✅ **Pluggable Rule Engine** — 6+ concrete analysis rules
✅ **Hybrid Scoring** — AI + Rules-based analysis (70/30 weighted)
✅ **Dependency Graph** — Circular dependency detection & coupling metrics
✅ **Diff-Based Analysis** — PR-focused (analyze only changed lines)
✅ **GitHub PR Bot** — Webhook-driven CI/CD integration with inline comments
✅ **Persistent Storage** — History tracking, trend analysis, regression detection
✅ **CLI Tool** — Professional-grade `aesthenix` command
✅ **Metrics/Observability** — Prometheus + Micrometer integration
✅ **Multi-Language Support** — Extensible analyzer interface (Java ✅, Python/JS ready)
✅ **Advanced UX** — File heatmap, severity grouping, gauge visualization
✅ **Security Hardening** — Input validation, rate limiting, injection prevention

---

## 🏗 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND LAYER                              │
│  (React: HeatmapView, SeverityGrouped, ScoreGauge, CodeEditor)     │
│  - File quality heatmap with color-coded severity                 │
│  - Grouped issues by severity level (🔴🟠🟡🟢)                     │
│  - Animated score gauge visualization                              │
└─────────────────────────────────────────────────────────────────────┘
                                  ↕
┌─────────────────────────────────────────────────────────────────────┐
│                  REST API GATEWAY (Spring Boot)                     │
│  - /api/review         (single file analysis)                       │
│  - /api/repo/scan      (directory/repo analysis)                    │
│  - /api/history        (trend analysis)                             │
│  - /webhook/github/pr  (PR bot entry point)                         │
└─────────────────────────────────────────────────────────────────────┘
                                  ↕
┌─────────────────────────────────────────────────────────────────────┐
│           ANALYSIS PIPELINE (5-Stage Hybrid Engine)                 │
│                                                                     │
│  Input Code                                                         │
│    ↓                                                                │
│  [1] PREPROCESSOR → JavaParser AST + Metadata                      │
│    ↓                                                                │
│  [2] RULE ENGINE → Apply 6+ pluggable rules                        │
│    │  - LongMethodRule (>30 lines)                                 │
│    │  - NestedLoopRule (>2 levels)                                 │
│    │  - GodClassRule (>15 methods)                                 │
│    │  - NamingConventionRule                                       │
│    │  - CircularDependencyRule                                     │
│    │  - ExceptionHandlingRule                                      │
│    ↓                                                                │
│  [3] AI ENGINE → Ollama (with retry + fallback)                    │
│    │  - 3 retries with exponential backoff                         │
│    │  - Graceful degradation if AI unavailable                     │
│    ↓                                                                │
│  [4] GRAPH ANALYZER → Dependency analysis                          │
│    │  - Circular dependency detection                              │
│    │  - Coupling metrics (per-module)                              │
│    ↓                                                                │
│  [5] AGGREGATOR → Merge + Deduplicate                              │
│    │  - Remove duplicate issues (same type+line)                   │
│    │  - Sort by severity                                           │
│    │  - Group by severity for UI                                   │
│    ↓                                                                │
│  Output → Scored Analysis + Issues + Suggestions                   │
└─────────────────────────────────────────────────────────────────────┘
                                  ↕
┌─────────────────────────────────────────────────────────────────────┐
│         SUPPORTING SERVICES (Resilience & Intelligence)             │
│                                                                     │
│  • SecurityValidator    → Input validation, rate limiting          │
│  • AnalysisHistoryService → Persist results, trend detection       │
│  • MetricsService       → Prometheus metrics, SLO tracking         │
│  • PRReviewBotService   → GitHub bot logic                         │
│  • LanguageAnalyzerFactory → Multi-language dispatch               │
└─────────────────────────────────────────────────────────────────────┘
                                  ↕
┌─────────────────────────────────────────────────────────────────────┐
│              DATA PERSISTENCE LAYER                                 │
│  • database: AnalysisRecord (JPA) — history, audit trail           │
│  • cache: Caffeine — query results (30min TTL)                     │
│  * Redis support: ready for distributed deployments               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🔌 Component Details

### 1. **Rule Engine (6+ Rules)**

**File:** `src/analysis/rules/*.java`

Rules detect code smells, architectural issues, and quality metrics:

| Rule                   | Detects                     | Severity | Penalty |
| ---------------------- | --------------------------- | -------- | ------- |
| LongMethodRule         | Methods > 30 lines          | 7        | -15     |
| NestedLoopRule         | Loops nested > 2 levels     | 6        | -10     |
| GodClassRule           | Classes > 15 methods        | 8        | -20     |
| NamingConventionRule   | Non-camelCase identifiers   | 3        | -5      |
| CircularDependencyRule | Cyclic imports              | 5        | -25     |
| ExceptionHandlingRule  | Bare catch / missing throws | 7        | -20     |

**Registry:** Dynamically loaded via `@Component` stereotype.

---

### 2. **Dependency Graph Analysis**

**Service:** `CodeGraphAnalysisService`

```java
// Example usage
DependencyGraph graph = graphService.buildGraph(units, filePath);

// Detect issues
List<Issue> cycles = graphService.detectCircularDependencies(graph, filePath);
List<Issue> coupling = graphService.detectHighCoupling(graph, filePath, threshold);

// Metrics
double avgCoupling = graphService.getAverageCoupling(graph);
```

**Features:**

- Builds AST-based dependency graph
- Detects strongly connected components (cycles)
- Measures node centrality (coupling)
- Identifies architectural violations

---

### 3. **Diff-Based PR Analysis**

**Service:** `DiffAnalysisEngine`

Parses GitHub unified diff format and extracts changed lines:

```
@@ -10,6 +10,10 @@
 context line
-old line (removed)
+new line (added)  ← only analyze this
 more context
```

**Benefits:**

- Analyzes only changed lines (faster)
- Realistic PR review workflow
- Reduces false positives (unchanged code ignored)

---

### 4. **AI Service with Resilience**

**Service:** `LocalAIService`

```
┌─────────────────────────────────────┐
│   Analyze Code Request              │
└────────────┬───────────────────────┘
             ↓
    Check cache? (YES → return)
             ↓
    Call AI with retry (up to 3 attempts)
    Exponential backoff: 500ms → 1s → 2s
             ↓
    On failure → return neutral result (graceful degradation)
             ↓
    Cache successful result
```

**Configuration in `application.yml`:**

```yaml
ollama:
  enabled: ${OLLAMA_ENABLED:true}
  url: ${OLLAMA_URL:http://localhost:11434}
  model: ${OLLAMA_MODEL:qwen3.5:9b}
  timeout: 120
  retry-attempts: 3 # ← resilience setting
```

If AI unavailable, analysis continues with rules-only scoring.

---

### 5. **Configurable Scoring System**

**Configuration:** `application.yml` (no code changes needed)

```yaml
scoring:
  # Weights
  rule-weight: 0.7 # 70% static analysis
  ai-weight: 0.3 # 30% AI model

  # Thresholds
  base-score: 100
  max-penalties: 50
  long-method-threshold: 30
  god-class-method-threshold: 15

  # Penalties (tunable per stakeholder)
  long-method-penalty: 15
  nested-loop-penalty: 10
  complexity-penalty: 10
  # ... see application.yml for complete list
```

**Interview angle:** "Stakeholders can tune scoring weights without code redeploy—critical for fast iteration."

---

### 6. **GitHub PR Review Bot**

**Webhook Entry:** `GitHubWebhookController`

Flow:

```
GitHub PR opened/updated
    ↓
POST /webhook/github/pr (with HMAC-SHA256 signature)
    ↓
SecurityValidator checks signature
    ↓
Fetch changed files from GitHub API
    ↓
Parse unified diff patches
    ↓
Analyze ONLY added lines
    ↓
Generate 10 inline comments (max)
    ↓
Post on PR with format: "Line 52: Issue | Severity: HIGH | Fix: XYZ"
```

**Output Example:**

````
Line 52: Avoid nested loops → O(n²) complexity risk

💡 Suggestion:
```java
// Extract inner loop to method
private void processItems(List<Item> outer) {
    for (Item o : outer) {
        processInner(o);
    }
}
````

**Features:**

- HMAC signature verification (security)
- Max 10 comments per PR (avoid spam)
- Suggestion blocks with "Apply" button
- Overall summary comment with score

---

### 7. **Storage + History Tracking**

**Entity:** `AnalysisRecord` (JPA)

```java
@Entity
public class AnalysisRecord {
    // Persistence
    String filePath;
    String repositoryName;
    String commitHash;

    // Metrics
    Double score;
    int totalIssues, critical, high, medium, low;
    Long analysisTimeMs;

    // Audit trail
    LocalDateTime analyzedAt;
    String analysisSource;  // "cli", "pr-bot", "api"
    String triggeredBy;
}
```

**Services:**

- `AnalysisHistoryService` — track trends, detect regressions
- `AnalysisRecordRepository` — JPA queries (file history, date ranges)

**Queries:**

```java
// Get latest analysis
AnalysisRecord latest = repo.findFirstByFilePathOrderByAnalyzedAtDesc(file);

// Trend: compare last 30 days
TrendAnalysis trend = historyService.getTrend(file, days=30);
if (trend.scoreDropped > 10) alert("REGRESSION DETECTED");

// Repository metrics
RepositoryMetrics metrics = historyService.getRepositoryMetrics(repoName);
```

---

### 8. **Professional CLI Tool**

**Entry Point:** `cli_tool.py → main()`

Commands:

```bash
# Analyze file or directory
aesthenix analyze src/                    # directory
aesthenix analyze Main.java               # single file

# Show trends
aesthenix history src/Example.java --days 30

# Export results
aesthenix export Main.java --format json  # or csv, html

# System metrics
aesthenix metrics
```

**Output Format:**

```
✓ Main.java                   82.5/100  issues:2 (🔴0 🟠1 🟡1)
⚠ Service.java                61.3/100  issues:8 (🔴1 🟠4 🟡3)
✗ Utils.java                  38.9/100  issues:12 (🔴3 🟠5 🟡4)

════════════════════════════════════
Summary:
  Files analyzed:     3
  Average score:      60.9/100
  Total issues:       22 (🔴4 🟠10 🟡8)
  Total time:         1,234ms
```

**Features:**

- Colored output (red=critical, yellow=warning, green=ok)
- Progress tracking
- Aggregated metrics
- ANSI terminal support

---

### 9. **Metrics & Observability**

**Service:** `MetricsService`

Integrates with **Micrometer** → **Prometheus**:

```java
// Timers (latency distribution)
Timer pipelineLatency;     // p50, p95, p99
Timer preprocessLatency;
Timer ruleEngineLatency;
Timer aiServiceLatency;
Timer aggregationLatency;

// Counters
Counter analysisSuccess;   // total successful analyses
Counter analysisFailure;   // total failures
Counter aiCalls;           // AI invocations
Counter aiFailures;        // AI failures

// Gauges (point-in-time values)
cacheHitRate = hits / (hits + misses)
```

**Prometheus Endpoint:**

```
GET /actuator/prometheus
```

Metrics example:

```
analysis_pipeline_latency_seconds{quantile="0.95",} 2.543
analysis_pipeline_latency_seconds{quantile="0.99",} 3.891
analysis_success_total 1234
analysis_failure_total 3
ai_calls_total 456
cache_hit_rate 0.78
```

---

### 10. **Multi-Language Extensibility**

**Interface:** `LanguageAnalyzer`

```java
public interface LanguageAnalyzer {
    List<Issue> analyze(String sourceCode, String filePath);
    Language getLanguage();
    enum Language { JAVA, PYTHON, JAVASCRIPT, GO }
}

// Current implementations
- JavaLanguageAnalyzer (JavaParser)

// TODO: Future implementations
- PythonLanguageAnalyzer (AST module + pylint)
- JavaScriptAnalyzer (ESLint + Babel)
- GoAnalyzer (go/analysis package)
```

**Factory Pattern:**

```java
LanguageAnalyzer analyzer = factory.getAnalyzer("file.py");
// → returns PythonLanguageAnalyzer (when implemented)
```

---

### 11. **Advanced Frontend UX**

**Components:** `HeatmapView.jsx`

1. **File Heatmap**
   - Per-line color coding (red=critical, yellow=warning, etc.)
   - Hover tooltips showing issue details
   - Selectable ranges for navigation

2. **Severity Grouped**
   - (🔴) Critical Issues (8-10)
   - (🟠) High Priority (5-7)
   - (🟡) Medium (3-4)
   - (🟢) Low (0-2)
   - Expandable sections with full issue details

3. **Score Gauge**
   - Circular gauge with needle (0-100)
   - Color zones (green ≥80, yellow 50-79, red <50)
   - Trend indicator (↑ +5% vs last week)

---

### 12. **Security Hardening**

**Service:** `SecurityValidator`

Input validation:

```java
validateCode(code, filePath)
  ├─ Check size ≤ 5MB
  ├─ Validate file extension (.java, .py, etc.)
  ├─ Prevent path traversal (../, /etc/)
  └─ Detect injection patterns (cmd/eval/exec)

validateBatchRequest(fileCount)
  └─ Max 100 files per batch
```

Rate limiting:

```java
RateLimiter.isAllowed(clientIp)
  └─ 60 requests/minute per IP (configurable)
```

**Configuration:**

```yaml
security:
  max-code-size: 5242880 # 5MB
  max-files-per-batch: 100
  allowed-file-extensions: .java,.py,.js,.ts,.go
  rate-limit.requests-per-minute: 60
```

---

## 🧠 Interview Narrative

### Problem Statement

"Developers lack instant, structured feedback on code quality. Manual code reviews are slow, subjective, and miss edge cases."

### Solution: AESTHENIX AI

"A **modular analysis pipeline** combining:

1. **Static analysis** (JavaParser rules)
2. **AI reasoning** (local Ollama model)
3. **Architectural insights** (dependency graphs)
4. **Real-world integration** (GitHub PR bot)"

### Key Design Decisions

| Decision                   | Rationale                                       | Trade-off                   |
| -------------------------- | ----------------------------------------------- | --------------------------- |
| **Pluggable rules**        | Easy to add/disable rules without recompile     | Initial complexity setup    |
| **70/30 hybrid scoring**   | Combines reliability of rules with nuance of AI | Need both engines running   |
| **Diff-based PR analysis** | Realistic workflow, faster                      | Requires PR context         |
| **Local Ollama**           | Privacy, cost-free, no API keys                 | Slower than cloud APIs      |
| **Fallback to rules-only** | Gracefully degrade if AI fails                  | Doesn't include AI insights |
| **PersistentStorage**      | Track regression, measure improvement           | Storage overhead            |
| **CLI tool**               | Engineers trust CLI interfaces                  | Requires Python environment |

### Why This Matters (FAANG Signal)

✅ **Modular Architecture** — Separates concerns (rules, AI, aggregation)
✅ **Resilience** — Handles failures gracefully (retry, fallback, cache)
✅ **Real-world Integration** — PR bot, diff analysis, CLI (not just a demo)
✅ **Observability** — Prometheus metrics, SLO tracking (production mindset)
✅ **Extensibility** — Multi-language support designed in from day one
✅ **Security** — Input validation, rate limiting, injection prevention
✅ **Configurability** — Tunable weights, thresholds (no recompile)
✅ **Storage + History** — Enables trend detection, regression alerts

---

## 📊 Performance & Scalability

### Latency Budget (SLO)

```
Single file analysis (< 1000 LOC):
  ├─ Preprocess:      50ms (JavaParser)
  ├─ Rules:          100ms (6 rules)
  ├─ AI:             300ms (Ollama)
  └─ Aggregation:     50ms
  Total:            ~500ms (p95 target: 1s)
```

### Concurrency

```
Thread pool: 4-8 threads (tuneable)
Queue capacity: 50 requests
Fallback: reject if queue full (circuit breaker pattern)
```

### Caching

```
Cache: Caffeine (in-memory)
  ├─ TTL: 30 minutes
  ├─ Max entries: 200
  ├─ Hit rate: ~75% typical (code analysis often repeated)
```

### Database

```
AnalysisRecord table:
  ├─ Index on (filePath, analyzedAt)
  ├─ Index on (repositoryName, analyzedAt)
  └─ Partitioning: optional (by date for large deployments)
```

---

## 🚀 Deployment Considerations

### Requirements

- **Java 17+**
- **Spring Boot 3.2+**
- **PostgreSQL or MySQL** (for history)
- **Ollama server** (optional; falls back to rules-only)
- **GitHub App credentials** (preferred for PR bot)

### Environment Variables

```bash
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=qwen3.5:9b
OLLAMA_ENABLED=true

GITHUB_APP_ID=3349093
GITHUB_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----
# ... paste the full key here ...
# -----END RSA PRIVATE KEY-----

# Optional legacy GitHub token (not required for webhook app auth)
# GITHUB_TOKEN=ghp_xxxxx
PR_BOT_ENABLED=true
PR_BOT_WEBHOOK_SECRET=webhook_secret

DATABASE_URL=jdbc:postgresql://localhost:5432/code_reviewer
DATABASE_USER=postgres
DATABASE_PASSWORD=xxxxx
```

### Docker Deployment

```dockerfile
FROM openjdk:17-slim
RUN apt-get install -y ollama  # optional
COPY target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

---

## 📈 Future Roadmap

- [ ] Python analyzer (AST-based)
- [ ] JavaScript/TypeScript support
- [ ] Go language analyzer
- [ ] Redis for distributed caching
- [ ] Kubernetes deployment (Helm charts)
- [ ] Dashboard with historical trends
- [ ] Slack bot integration
- [ ] Custom rule builder UI
- [ ] ML-based anomaly detection (for regressions)
- [ ] Integration with CI/CD platforms (GitLab, Azure DevOps)

---

## 🎓 Key Takeaways

**This project demonstrates:**

1. **System Design** — modular pipeline with clear separation of concerns
2. **Resilience** — graceful degradation, retry logic, fallbacks
3. **Real-world thinking** — PR integration, history tracking, UX
4. **Production mindset** — metrics, security, rate limiting, observability
5. **Extensibility** — multi-language support, pluggable rules
6. **Communication** — this document proves ability to articulate architecture

**Perfect closing statement for interviews:**

> "I built a code quality platform that combines static analysis rules with AI reasoning. The system handles failures gracefully, integrates with GitHub workflows via webhooks, and provides observability for production deployment. Most importantly, it's extensible—adding Python or JavaScript analyzers requires only implementing a new language analyzer, not touching the core pipeline."

---

**Happy interviewing! 🚀**
