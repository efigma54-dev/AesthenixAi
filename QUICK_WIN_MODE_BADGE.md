# ⚡ Quick Win: Mode Badge Implementation

**Time:** 30 minutes  
**ROI:** 3x (trust building)  
**Priority:** High

---

## 🎯 What This Does

Adds a visible mode indicator in the VS Code extension to show users:

- What mode they're in (Rule/AI/Hybrid)
- Why results are fast or slow
- What's happening under the hood

**This builds trust through transparency.**

---

## 🛡️ The Three Modes

```
⚡ Rule Mode     — Fast, reliable, no AI (< 2 seconds)
🧠 AI Mode       — Deep analysis, slower (5-10 seconds)
🛡️ Hybrid Mode   — Best of both (default, 3-5 seconds)
```

---

## 📝 Implementation

### 1. Add to VS Code Extension Status Bar

**File:** `vscode-extension/src/extension.ts`

```typescript
import * as vscode from 'vscode';

let statusBarItem: vscode.StatusBarItem;

export function activate(context: vscode.ExtensionContext) {
  // Create status bar item
  statusBarItem = vscode.window.createStatusBarItem(
    vscode.StatusBarAlignment.Right,
    100,
  );

  // Set initial mode
  updateModeIndicator('hybrid');
  statusBarItem.show();

  context.subscriptions.push(statusBarItem);

  // Add command to toggle mode
  let toggleMode = vscode.commands.registerCommand(
    'aesthenixai.toggleMode',
    () => {
      const modes = ['rule', 'ai', 'hybrid'];
      const config = vscode.workspace.getConfiguration('aesthenixai');
      const currentMode = config.get('analysisMode', 'hybrid');
      const currentIndex = modes.indexOf(currentMode);
      const nextMode = modes[(currentIndex + 1) % modes.length];

      config.update('analysisMode', nextMode, true);
      updateModeIndicator(nextMode);

      vscode.window.showInformationMessage(
        `Analysis mode: ${getModeDescription(nextMode)}`,
      );
    },
  );

  context.subscriptions.push(toggleMode);
}

function updateModeIndicator(mode: string) {
  const modeConfig = {
    rule: { icon: '⚡', name: 'Rule', tooltip: 'Fast, reliable, no AI' },
    ai: { icon: '🧠', name: 'AI', tooltip: 'Deep analysis, slower' },
    hybrid: { icon: '🛡️', name: 'Hybrid', tooltip: 'Best of both (default)' },
  };

  const config = modeConfig[mode] || modeConfig['hybrid'];

  statusBarItem.text = `${config.icon} ${config.name}`;
  statusBarItem.tooltip = `AESTHENIXAI: ${config.tooltip}\nClick to change mode`;
  statusBarItem.command = 'aesthenixai.toggleMode';
}

function getModeDescription(mode: string): string {
  const descriptions = {
    rule: '⚡ Rule Mode — Fast, reliable, no AI',
    ai: '🧠 AI Mode — Deep analysis, slower',
    hybrid: '🛡️ Hybrid Mode — Best of both',
  };
  return descriptions[mode] || descriptions['hybrid'];
}
```

---

### 2. Add Configuration to package.json

**File:** `vscode-extension/package.json`

```json
{
  "contributes": {
    "configuration": {
      "title": "AESTHENIXAI",
      "properties": {
        "aesthenixai.analysisMode": {
          "type": "string",
          "enum": ["rule", "ai", "hybrid"],
          "default": "hybrid",
          "description": "Analysis mode: rule (fast), ai (deep), or hybrid (balanced)",
          "enumDescriptions": [
            "⚡ Rule Mode — Fast, reliable, no AI (< 2 seconds)",
            "🧠 AI Mode — Deep analysis, slower (5-10 seconds)",
            "🛡️ Hybrid Mode — Best of both (default, 3-5 seconds)"
          ]
        }
      }
    },
    "commands": [
      {
        "command": "aesthenixai.toggleMode",
        "title": "AESTHENIXAI: Toggle Analysis Mode"
      }
    ]
  }
}
```

---

### 3. Update Analysis Request to Include Mode

**File:** `vscode-extension/src/extension.ts`

```typescript
async function analyzeCode() {
  const editor = vscode.window.activeTextEditor;
  if (!editor) return;

  const config = vscode.workspace.getConfiguration('aesthenixai');
  const mode = config.get('analysisMode', 'hybrid');

  // Update status bar to show analyzing
  statusBarItem.text = `$(sync~spin) Analyzing...`;

  try {
    const response = await fetch(`${API_URL}/review`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        code: editor.document.getText(),
        filename: editor.document.fileName,
        mode: mode, // Send mode to backend
      }),
    });

    const result = await response.json();

    // Show which mode was used
    vscode.window.showInformationMessage(
      `Analysis complete (${getModeIcon(result.modeUsed)} ${result.modeUsed})`,
    );

    // Restore mode indicator
    updateModeIndicator(mode);
  } catch (error) {
    vscode.window.showErrorMessage(`Analysis failed: ${error.message}`);
    updateModeIndicator(mode);
  }
}

function getModeIcon(mode: string): string {
  const icons = { rule: '⚡', ai: '🧠', hybrid: '🛡️' };
  return icons[mode] || '🛡️';
}
```

---

### 4. Backend Support (Optional Enhancement)

**File:** `src/main/java/com/aicode/model/CodeReviewRequest.java`

```java
public class CodeReviewRequest {
    private String code;
    private String filename;
    private String language;

    @Builder.Default
    private AnalysisMode mode = AnalysisMode.HYBRID;

    public enum AnalysisMode {
        RULE,    // Rule-based only
        AI,      // AI only
        HYBRID   // Both (default)
    }
}
```

**File:** `src/main/java/com/aicode/service/CodeReviewService.java`

```java
public CodeReviewResponse analyzeCode(CodeReviewRequest request) {
    log.info("Analyzing code in {} mode", request.getMode());

    CodeReviewResponse response = new CodeReviewResponse();
    response.setModeUsed(request.getMode().toString().toLowerCase());

    switch (request.getMode()) {
        case RULE:
            // Rule-based analysis only
            response.setIssues(ruleEngine.analyze(request.getCode()));
            response.setProcessingTime(calculateTime());
            break;

        case AI:
            // AI analysis only
            response.setIssues(aiEngine.analyze(request.getCode()));
            response.setProcessingTime(calculateTime());
            break;

        case HYBRID:
        default:
            // Both (existing logic)
            response.setIssues(hybridAnalysis(request.getCode()));
            response.setProcessingTime(calculateTime());
            break;
    }

    return response;
}
```

---

## 🎨 Visual Design

### Status Bar (Bottom Right)

```
Normal state:
┌─────────────────────────────────────┐
│ ... │ 🛡️ Hybrid │ Ln 42, Col 15 │ UTF-8 │
└─────────────────────────────────────┘

Analyzing:
┌─────────────────────────────────────┐
│ ... │ ⟳ Analyzing... │ Ln 42, Col 15 │
└─────────────────────────────────────┘

Complete:
┌─────────────────────────────────────┐
│ ... │ ✓ 3 issues │ 🛡️ Hybrid │ Ln 42 │
└─────────────────────────────────────┘
```

### Tooltip on Hover

```
AESTHENIXAI: Best of both (default)
⚡ Rule Mode: < 2 seconds
🧠 AI Mode: 5-10 seconds
🛡️ Hybrid Mode: 3-5 seconds (current)

Click to change mode
```

---

## 📊 Expected Impact

### Trust Building

- Users understand what's happening
- Users know why results are fast/slow
- Users can control the experience

### Engagement

- Users experiment with modes
- Users find their preferred mode
- Users feel in control

### Feedback Quality

- Users can report mode-specific issues
- You can track mode preferences
- You can optimize based on usage

---

## 🚀 Rollout Plan

### Phase 1: Basic Implementation (30 min)

- Add status bar item
- Add mode indicator
- Add click to toggle

### Phase 2: Backend Support (1 hour)

- Add mode parameter to API
- Implement mode-specific logic
- Return mode used in response

### Phase 3: Polish (30 min)

- Add keyboard shortcut (Ctrl+Shift+M)
- Add to command palette
- Add to settings UI

---

## 📝 Testing Checklist

- [ ] Status bar shows correct icon
- [ ] Tooltip shows correct description
- [ ] Click toggles between modes
- [ ] Mode persists across sessions
- [ ] Analysis respects selected mode
- [ ] Notification shows mode used
- [ ] Settings UI works correctly

---

## 🎯 Success Metrics

**Track:**

- Mode usage distribution (rule/ai/hybrid)
- Satisfaction by mode (thumbs up ratio)
- Performance by mode (avg time)
- Retention by mode preference

**Expected:**

- 60% use hybrid (default)
- 30% use rule (speed preference)
- 10% use AI (quality preference)

---

## 💡 Future Enhancements

### Auto Mode Selection

```typescript
// Automatically choose mode based on file size
if (fileSize < 100 lines) {
    mode = 'hybrid';  // Fast enough for both
} else if (fileSize < 500 lines) {
    mode = 'rule';    // Too slow for AI
} else {
    mode = 'rule';    // Definitely too slow
}
```

### Smart Mode

```typescript
// Learn user preferences over time
if (userAlwaysSkipsAI) {
  suggestMode = 'rule';
} else if (userAlwaysWaitsForAI) {
  suggestMode = 'ai';
}
```

---

## 🔥 Why This Matters

**Transparency builds trust.**

When users understand:

- What's happening
- Why it's fast/slow
- What they're getting

They trust the results more.

**This is a 30-minute investment that pays dividends in user trust.**

---

**Next:** Implement this, then move to demo GIF creation.
