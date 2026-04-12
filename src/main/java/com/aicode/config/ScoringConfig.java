package com.aicode.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable scoring parameters loaded from application.yml.
 * Allows customization of scoring weights and penalties without code changes.
 */
@Component
@ConfigurationProperties(prefix = "scoring")
public class ScoringConfig {

  // Weight distribution
  private double ruleWeight = 0.7;
  private double aiWeight = 0.3;

  // Base scoring
  private int baseScore = 100;
  private int maxPenalties = 50;

  // Rule-specific penalties
  private int longMethodPenalty = 15;
  private int nestedLoopPenalty = 10;
  private int namingConventionPenalty = 5;
  private int complexityPenalty = 10;
  private int exceptionHandlingPenalty = 20;
  private int loopRatioPenalty = 8;

  // AI scoring parameters
  private double aiIssuePenalty = 3.0;

  public double getRuleWeight() {
    return ruleWeight;
  }

  public void setRuleWeight(double ruleWeight) {
    this.ruleWeight = ruleWeight;
  }

  public double getAiWeight() {
    return aiWeight;
  }

  public void setAiWeight(double aiWeight) {
    this.aiWeight = aiWeight;
  }

  public int getBaseScore() {
    return baseScore;
  }

  public void setBaseScore(int baseScore) {
    this.baseScore = baseScore;
  }

  public int getMaxPenalties() {
    return maxPenalties;
  }

  public void setMaxPenalties(int maxPenalties) {
    this.maxPenalties = maxPenalties;
  }

  public int getLongMethodPenalty() {
    return longMethodPenalty;
  }

  public void setLongMethodPenalty(int longMethodPenalty) {
    this.longMethodPenalty = longMethodPenalty;
  }

  public int getNestedLoopPenalty() {
    return nestedLoopPenalty;
  }

  public void setNestedLoopPenalty(int nestedLoopPenalty) {
    this.nestedLoopPenalty = nestedLoopPenalty;
  }

  public int getNamingConventionPenalty() {
    return namingConventionPenalty;
  }

  public void setNamingConventionPenalty(int namingConventionPenalty) {
    this.namingConventionPenalty = namingConventionPenalty;
  }

  public int getComplexityPenalty() {
    return complexityPenalty;
  }

  public void setComplexityPenalty(int complexityPenalty) {
    this.complexityPenalty = complexityPenalty;
  }

  public int getExceptionHandlingPenalty() {
    return exceptionHandlingPenalty;
  }

  public void setExceptionHandlingPenalty(int exceptionHandlingPenalty) {
    this.exceptionHandlingPenalty = exceptionHandlingPenalty;
  }

  public int getLoopRatioPenalty() {
    return loopRatioPenalty;
  }

  public void setLoopRatioPenalty(int loopRatioPenalty) {
    this.loopRatioPenalty = loopRatioPenalty;
  }

  public double getAiIssuePenalty() {
    return aiIssuePenalty;
  }

  public void setAiIssuePenalty(double aiIssuePenalty) {
    this.aiIssuePenalty = aiIssuePenalty;
  }
}