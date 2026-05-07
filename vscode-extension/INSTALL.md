# AESTHENIXAI VS Code Extension — Install Guide

## Install the .vsix

1. Open VS Code
2. Press `Ctrl+Shift+P` → type **"Install from VSIX"**
3. Select `aesthenixai-0.2.0.vsix` from this folder
4. Reload VS Code when prompted

## Start the backend first

```bash
cd ai-code-reviewer
mvn spring-boot:run
```

Wait for: `Tomcat started on port(s): 8082`

## Use it

Open any `.java` file, then:

| Action               | How                                                            |
| -------------------- | -------------------------------------------------------------- |
| Analyze current file | `Ctrl+Shift+P` → **AESTHENIXAI: Analyze Current File**         |
| Analyze selection    | Select code → right-click → **AESTHENIXAI: Analyze Selection** |
| Apply AI refactor    | Right-click → **AESTHENIXAI: Apply AI Refactor**               |
| Clear highlights     | `Ctrl+Shift+P` → **AESTHENIXAI: Clear Diagnostics**            |
| Auto-analyze on save | Settings → `aesthenixai.autoAnalyzeOnSave: true`               |

## What you get

- **Inline highlights** — red/yellow/blue lines on issue locations (like ESLint)
- **Problems panel** — all issues listed with line numbers
- **Hover messages** — hover any highlighted line to see the issue
- **Lightbulb quick-fix** — click 💡 on any issue line → "Apply AI refactor"
- **Diff preview** — before applying refactor, see a side-by-side diff
- **Results panel** — score card + issues + suggestions in a sidebar

## Settings

Open `Settings` → search `aesthenixai`:

| Setting             | Default                     | Description             |
| ------------------- | --------------------------- | ----------------------- |
| `backendUrl`        | `http://localhost:8082/api` | Backend URL             |
| `timeoutMs`         | `180000`                    | Request timeout (3 min) |
| `highlightIssues`   | `true`                      | Show inline highlights  |
| `autoAnalyzeOnSave` | `false`                     | Auto-analyze on save    |

## Troubleshooting

**"Backend not reachable"**
→ Make sure `mvn spring-boot:run` is running and shows port 8082

**Analysis takes 30-60 seconds**
→ Normal — Ollama runs locally on CPU. First call loads the model.

**No issues found on valid bad code**
→ The AI may have returned empty. Try again — Ollama responses vary.
