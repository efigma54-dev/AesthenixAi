package com.aicode.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses GitHub unified diff patches and extracts changed line information.
 *
 * GitHub PR files API returns a "patch" field in unified diff format:
 *
 *   @@ -10,6 +10,10 @@
 *   -old line
 *   +new line
 *    context line
 *
 * This engine extracts:
 *   - The line numbers of added/modified lines in the new file
 *   - The actual code content of those lines
 *
 * Used by PRReviewBotService to post inline comments on the exact changed lines.
 */
@Service
public class DiffAnalysisEngine {

    private static final Logger log = LoggerFactory.getLogger(DiffAnalysisEngine.class);

    /**
     * Parse a GitHub unified diff patch and return the changed lines.
     *
     * @param patch    The "patch" field from GitHub PR files API
     * @param filePath File path for logging
     * @return PatchResult with line numbers and code of added lines
     */
    public PatchResult parsePatch(String patch, String filePath) {
        if (patch == null || patch.isBlank()) {
            return PatchResult.empty();
        }

        List<ChangedLine> changedLines = new ArrayList<>();
        int currentNewLine = 0;

        for (String line : patch.split("\n")) {
            if (line.startsWith("@@")) {
                // Parse hunk header: @@ -oldStart,oldCount +newStart,newCount @@
                // Example: @@ -10,6 +10,10 @@
                currentNewLine = parseNewStart(line);

            } else if (line.startsWith("+") && !line.startsWith("+++")) {
                // Added line — this is what we want to comment on
                String code = line.substring(1); // strip the leading '+'
                changedLines.add(new ChangedLine(currentNewLine, code, ChangeType.ADDED));
                currentNewLine++;

            } else if (line.startsWith("-") && !line.startsWith("---")) {
                // Removed line — doesn't advance new-file line counter
                // (no-op for new-file line numbering)

            } else {
                // Context line — advances both old and new counters
                currentNewLine++;
            }
        }

        log.debug("Parsed patch for {} — {} changed lines", filePath, changedLines.size());
        return new PatchResult(changedLines);
    }

    /**
     * Extract only the added code as a single string (for analysis).
     * Skips lines with fewer than 3 changed lines (trivial changes).
     */
    public String extractAddedCode(String patch) {
        PatchResult result = parsePatch(patch, "");
        if (result.getAddedLines().size() < 3) return "";

        StringBuilder sb = new StringBuilder();
        for (ChangedLine line : result.getAddedLines()) {
            sb.append(line.getCode()).append("\n");
        }
        return sb.toString().trim();
    }

    // ── Helpers ────────────────────────────────────────────────

    private int parseNewStart(String hunkHeader) {
        try {
            // @@ -oldStart,oldCount +newStart,newCount @@
            // Find the '+' part: "+10,10"
            int plusIdx = hunkHeader.indexOf('+');
            if (plusIdx < 0) return 0;
            String newPart = hunkHeader.substring(plusIdx + 1).split(" ")[0]; // "10,10" or "10"
            String startStr = newPart.split(",")[0];
            return Integer.parseInt(startStr.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Result types ───────────────────────────────────────────

    public enum ChangeType { ADDED, REMOVED, CONTEXT }

    public static class ChangedLine {
        private final int        lineNumber;
        private final String     code;
        private final ChangeType type;

        public ChangedLine(int lineNumber, String code, ChangeType type) {
            this.lineNumber = lineNumber;
            this.code       = code;
            this.type       = type;
        }

        public int        getLineNumber() { return lineNumber; }
        public String     getCode()       { return code; }
        public ChangeType getType()       { return type; }
    }

    public static class PatchResult {
        private final List<ChangedLine> lines;

        public PatchResult(List<ChangedLine> lines) { this.lines = lines; }

        public static PatchResult empty() { return new PatchResult(List.of()); }

        public List<ChangedLine> getAllLines()   { return lines; }
        public List<Integer>     getLineNumbers() {
            return lines.stream().map(ChangedLine::getLineNumber).toList();
        }
        public List<ChangedLine> getAddedLines() {
            return lines.stream().filter(l -> l.getType() == ChangeType.ADDED).toList();
        }
        public int size()         { return lines.size(); }
        public boolean hasChanges() { return !lines.isEmpty(); }
    }
}
