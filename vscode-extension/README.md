# AESTHENIXAI — AI Code Reviewer

AI-powered code review with inline fixes, diagnostics, and refactoring for **Java, JavaScript, TypeScript, and Python**.

Combines a local AI model (Ollama) with a fast static rule engine for instant + deep feedback.

---

## Features

| Feature                       | Description                                   |
| ----------------------------- | --------------------------------------------- |
| ⚡ **Instant rule analysis**  | Flags issues in <5ms before AI responds       |
| 🧠 **AI-powered review**      | Deep analysis via local Ollama model          |
| 💡 **Inline fix suggestions** | Copilot-style ghost text on flagged lines     |
| 🔧 **One-click refactor**     | Apply AI-improved code with diff preview      |
| 🔴 **Inline diagnostics**     | Colored left-border highlights per severity   |
| 📊 **Score + panel**          | Quality score, issues, suggestions in sidebar |
| 🌐 **Multi-language**         | Java, JavaScript, TypeScript, Python          |
| ✈️ **Offline mode**           | Rule-only analysis — no backend needed        |

---

## Quick Start

### 1. Install the extension

```bash
code --install-extension aesthenixai-0.3.0.vsix
```

Or install from the VS Code Marketplace (search "AESTHENIXAI").

### 2. Start the backend (for AI analysis)

```bash
# Clone the repo
git clone https://github.com/your-username/ai-code-reviewer
cd ai-code-reviewer

# Start backend
mvn spring-boot:run
```

Backend runs on `http://localhost:8082`.

### 3. Install Ollama + model

```bash
# Install Ollama: https://ollama.com
ollama pull qwen2.5-coder:7b
```

### 4. Analyze code

Open any `.java`, `.js`, `.ts`, or `.py` file and:

- Press `Ctrl+Shift+A` (Mac: `Cmd+Shift+A`)
- Or right-click → **AESTHENIXAI: Analyze Current File**

---

## Commands

| Command                    | Shortcut               | Description                           |
| -------------------------- | ---------------------- | ------------------------------------- |
| Analyze Current File       | `Ctrl+Shift+A`         | Full analysis of active file          |
| Analyze Selection          | —                      | Analyze selected code only            |
| Accept Fix on Current Line | `Ctrl+Shift+Enter`     | Apply inline fix suggestion           |
| Reject Fix on Current Line | `Ctrl+Shift+Backspace` | Dismiss inline suggestion             |
| Apply AI Refactor          | —                      | Replace file with AI-improved version |
| Clear Diagnostics          | —                      | Remove all highlights                 |
| Show Performance Metrics   | —                      | View latency/cache stats in output    |

---

## Settings

| Setting                         | Default                     | Description                 |
| ------------------------------- | --------------------------- | --------------------------- |
| `aesthenixai.backendUrl`        | `http://localhost:8082/api` | Backend API URL             |
| `aesthenixai.mode`              | `hybrid`                    | `hybrid` / `ai` / `offline` |
| `aesthenixai.timeoutMs`         | `180000`                    | Request timeout (ms)        |
| `aesthenixai.highlightIssues`   | `true`                      | Colored line highlights     |
| `aesthenixai.autoAnalyzeOnSave` | `false`                     | Auto-analyze on save        |
| `aesthenixai.inlineSuggestions` | `true`                      | Ghost text fix suggestions  |

### Modes

- **hybrid** _(recommended)_ — instant rule results shown immediately, AI refines in background
- **ai** — skip local rules, send everything to AI
- **offline** — local rules only, no backend needed, instant

---

## How It Works

```
Open file
    ↓
Stage 1: Local rule engine (<5ms)
    → Instant diagnostics + inline suggestions
    ↓
Stage 2: AI analysis (async)
    → Backend extracts flagged snippets
    → Sends to Ollama (qwen2.5-coder:7b)
    → Returns score + issues + improved code
    ↓
Results panel + diagnostics updated
```

The backend only sends **suspicious lines** to the AI (not the full file), cutting latency by 60–80%.

---

## Supported Languages

| Language   | Rule Engine | AI Analysis |
| ---------- | ----------- | ----------- |
| Java       | ✅ 7 rules  | ✅          |
| JavaScript | ✅ 6 rules  | ✅          |
| TypeScript | ✅ 6 rules  | ✅          |
| Python     | ✅ 6 rules  | ✅          |

---

## Requirements

- VS Code 1.85+
- Backend: Java 17+, Maven, Spring Boot (for AI analysis)
- Ollama with `qwen2.5-coder:7b` model (for AI analysis)
- Offline mode works without any backend

---

## Privacy

All analysis runs **locally** on your machine. No code is sent to external servers. The backend calls your local Ollama instance only.

---

## License

MIT — see [LICENSE](LICENSE)
