package com.aicode.analysis;

import com.aicode.model.AIResult;
import com.aicode.model.Issue;
import com.aicode.model.Suggestion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Post-processor that merges results from rule engine and AI analysis.
 * Handles deduplication, prioritization, and refinement of issues and
 * suggestions.
 */
@Service
public class PostProcessor {

  /**
   * Merge rule-based and AI-based analysis results.
   */
  public AnalysisPipeline.MergedResult merge(RuleEngine.RuleResult ruleResult,
      AIResult aiResult,
      AnalysisPipeline.ParsedCode parsedCode) {

    List<Issue> mergedIssues = new ArrayList<>();
    List<Suggestion> mergedSuggestions = new ArrayList<>();

    // Add rule-based issues
    mergedIssues.addAll(ruleResult.getIssues());

    // Add AI-based issues, avoiding duplicates
    Set<String> existingIssueTitles = new HashSet<>();
    mergedIssues.forEach(issue -> existingIssueTitles.add(issue.getTitle()));

    for (Issue aiIssue : aiResult.getIssues()) {
      if (!existingIssueTitles.contains(aiIssue.getTitle())) {
        mergedIssues.add(aiIssue);
      }
    }

    // Add AI-based suggestions
    mergedSuggestions.addAll(aiResult.getSuggestions());

    // Sort issues by severity (highest first)
    mergedIssues.sort((a, b) -> Integer.compare(b.getSeverity(), a.getSeverity()));

    return new AnalysisPipeline.MergedResult(mergedIssues, mergedSuggestions);
  }
}