# AESTHENIXAI — VS Code Extension

Analyze Java code directly inside VS Code using the AESTHENIXAI backend.

## Features

- **Right-click → Analyze** — analyze any `.java` file from the context menu
- **Inline issue highlighting** — red/yellow/blue left-border decorations per severity
- **VS Code Diagnostics** — issues appear in the Problems panel like ESLint errors
- **Results panel** — score, issues, and suggestions in a side panel
- **Analyze selection** — analyze only the selected code block
- **Auto-analyze on save** — optional, off by default
- **Health check** — tells you immediately if the backend is not running
- **Retry on failure** — error notification includes a Retry button

## Requirements

The AESTHENIXAI backend must be running:

```bash
cd ai-code-reviewer
export OPENAI_API_KEY=sk-...
./mvnw spring-boot:run
```

## Usage

1. Open any `.java` file
2. Right-click in the editor → **AESTHENIXAI: Analyze Current File**
   — or use the Command Palette (`Ctrl+Shift+P`) → `AESTHENIXAI: Analyze`
3. Results appear in a side panel; issues are highlighted inline

## Commands

| Command                             | Description                |
| ----------------------------------- | -------------------------- |
| `AESTHENIXAI: Analyze Current File` | Analyze the full file      |
| `AESTHENIXAI: Analyze Selection`    | Analyze selected code only |
| `AESTHENIXAI: Clear Diagnostics`    | Remove all highlights      |

## Settings

| Setting                         | Default                     | Description               |
| ------------------------------- | --------------------------- | ------------------------- |
| `aesthenixai.backendUrl`        | `http://localhost:8080/api` | Backend API URL           |
| `aesthenixai.timeoutMs`         | `30000`                     | Request timeout (ms)      |
| `aesthenixai.highlightIssues`   | `true`                      | Show inline decorations   |
| `aesthenixai.autoAnalyzeOnSave` | `false`                     | Auto-analyze on file save |

## Development

```bash
cd ai-code-reviewer/vscode-extension
npm install
npm run compile

# Press F5 in VS Code to launch the Extension Development Host
```

To package as a `.vsix` for distribution:

```bash
npm run package
# → aesthenixai-0.1.0.vsix
```

Install locally:

```bash
code --install-extension aesthenixai-0.1.0.vsix
```
