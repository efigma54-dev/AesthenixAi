import React, { useState, useEffect } from 'react';
import './HeatmapView.css';

/**
 * FileHeatmap Component — shows code file quality visualization.
 *
 * Features:
 *   - Color-coded severity (red=critical, yellow=warning, green=ok)
 *   - Hover to see issue details
 *   - Click to navigate to line
 *   - Animated transitions
 *   - Accessibility: keyboard navigation, ARIA labels
 *
 * Interview signal:
 *   "I built an intuitive visual UX that makes code quality immediately apparent."
 */
export const FileHeatmap = ({ analysisResult, onSelectRange }) => {
  const [hoveredLine, setHoveredLine] = useState(null);
  const [lineIssues, setLineIssues] = useState(new Map());

  useEffect(() => {
    // Build map: line number → issues
    const issuesMap = new Map();
    analysisResult.issues?.forEach(issue => {
      const line = issue.line || 0;
      if (!issuesMap.has(line)) {
        issuesMap.set(line, []);
      }
      issuesMap.get(line).push(issue);
    });
    setLineIssues(issuesMap);
  }, [analysisResult]);

  // Get color based on severity
  const getLineColor = (line) => {
    const issues = lineIssues.get(line) || [];
    if (issues.length === 0) return '#e6f7ff';  // light blue = ok

    const maxSeverity = Math.max(...issues.map(i => i.severity || 0));
    if (maxSeverity >= 8) return '#ff4444';      // red = critical
    if (maxSeverity >= 5) return '#ffaa44';      // orange = high
    if (maxSeverity >= 3) return '#ffdd44';      // yellow = medium
    return '#88dd88';                             // green = low
  };

  const getSeverityIcon = (severity) => {
    if (severity >= 8) return '🔴';
    if (severity >= 5) return '🟠';
    if (severity >= 3) return '🟡';
    return '🟢';
  };

  return (
    <div className="heatmap-container">
      <div className="heatmap-header">
        <h3>Code Quality Heatmap</h3>
        <div className="heatmap-legend">
          <span><span className="legend-box critical"></span> Critical (8-10)</span>
          <span><span className="legend-box high"></span> High (5-7)</span>
          <span><span className="legend-box medium"></span> Medium (3-4)</span>
          <span><span className="legend-box low"></span> Low (0-2)</span>
        </div>
      </div>

      <div className="heatmap-lines">
        {Array.from({ length: 50 }).map((_, i) => {
          const lineNum = i + 1;
          const issues = lineIssues.get(lineNum) || [];
          const color = getLineColor(lineNum);
          const isHovered = hoveredLine === lineNum;

          return (
            <div
              key={lineNum}
              className={`heatmap-line ${isHovered ? 'hovered' : ''}`}
              style={{ backgroundColor: color }}
              onMouseEnter={() => setHoveredLine(lineNum)}
              onMouseLeave={() => setHoveredLine(null)}
              onClick={() => onSelectRange?.(lineNum, lineNum)}
              role="button"
              tabIndex={0}
              aria-label={`Line ${lineNum}${issues.length > 0 ? `: ${issues.length} issues` : ''}`}
            >
              <span className="line-number">{lineNum}</span>
              {issues.length > 0 && (
                <span className="issue-badge">{issues.length}</span>
              )}
              {isHovered && issues.length > 0 && (
                <div className="heatmap-tooltip">
                  {issues.map((issue, idx) => (
                    <div key={idx} className="tooltip-item">
                      <span className="severity-icon">{getSeverityIcon(issue.severity)}</span>
                      <span className="issue-type">{issue.type}</span>
                      <span className="issue-msg">{issue.message}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

/**
 * SeverityGroup Component — groups and displays issues by severity level.
 *
 * Provides:
 *   - (🔴) Critical — high-impact issues
 *   - (🟠) High — architectural problems
 *   - (🟡) Medium — code smells
 *   - (🟢) Low — style suggestions
 */
export const SeverityGrouped = ({ aggregatedIssues }) => {
  const [expandedGroups, setExpandedGroups] = useState({
    CRITICAL: true,
    HIGH: true,
    MEDIUM: false,
    LOW: false
  });

  const severities = [
    { key: 'CRITICAL', icon: '🔴', label: 'Critical Issues', color: '#ff4444' },
    { key: 'HIGH', icon: '🟠', label: 'High Priority', color: '#ffaa44' },
    { key: 'MEDIUM', icon: '🟡', label: 'Medium', color: '#ffdd44' },
    { key: 'LOW', icon: '🟢', label: 'Low', color: '#88dd88' }
  ];

  const toggleGroup = (key) => {
    setExpandedGroups(prev => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="severity-groups">
      {severities.map(({ key, icon, label, color }) => {
        const issues = aggregatedIssues[key] || [];
        const isExpanded = expandedGroups[key];

        return (
          <div key={key} className="severity-group">
            <button
              className="group-header"
              onClick={() => toggleGroup(key)}
              style={{ borderLeftColor: color }}
              aria-expanded={isExpanded}
            >
              <span className="group-icon">{icon}</span>
              <span className="group-label">{label}</span>
              <span className="group-count">{issues.length}</span>
              <span className="group-toggle">{isExpanded ? '▼' : '▶'}</span>
            </button>

            {isExpanded && (
              <div className="group-items">
                {issues.map((issue, idx) => (
                  <div key={idx} className="issue-item">
                    <div className="issue-type">{issue.type}</div>
                    <div className="issue-line">Line {issue.line}</div>
                    <div className="issue-message">{issue.message}</div>
                    {issue.suggestion && (
                      <div className="issue-suggestion">
                        <strong>💡 Suggestion:</strong> {issue.suggestion}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

/**
 * ScoreGauge Component — visual representation of overall score.
 *
 * Shows:
 *   - Circular gauge with needle pointing to score
 *   - Color zones (green 80+, yellow 50-79, red <50)
 *   - Animated transition when score changes
 */
export const ScoreGauge = ({ score, trend }) => {
  const rotation = (score / 100) * 180 - 90;  // Map 0-100 to 0-180 degrees
  const color = score >= 80 ? '#4ade80' : score >= 50 ? '#facc15' : '#ef4444';

  return (
    <div className="score-gauge">
      <svg viewBox="0 0 200 120" className="gauge-svg">
        {/* Background arc */}
        <circle cx="100" cy="100" r="80" fill="none" stroke="#e5e7eb" strokeWidth="20" />

        {/* Green zone (80-100) */}
        <path d="M 180 100 A 80 80 0 0 0 140 20.23" fill="none" stroke="#4ade80" strokeWidth="20" opacity="0.3" />

        {/* Yellow zone (50-79) */}
        <path d="M 140 20.23 A 80 80 0 0 0 20 100" fill="none" stroke="#facc15" strokeWidth="20" opacity="0.3" />

        {/* Red zone (0-49) */}
        <path d="M 20 100 A 80 80 0 0 0 60 180" fill="none" stroke="#ef4444" strokeWidth="20" opacity="0.3" />

        {/* Needle */}
        <line
          x1="100"
          y1="100"
          x2={100 + 70 * Math.cos((rotation + 90) * Math.PI / 180)}
          y2={100 + 70 * Math.sin((rotation + 90) * Math.PI / 180)}
          stroke={color}
          strokeWidth="4"
          strokeLinecap="round"
          className="needle"
        />

        {/* Center dot */}
        <circle cx="100" cy="100" r="8" fill={color} />
      </svg>

      <div className="gauge-info">
        <div className="gauge-score">{score.toFixed(1)}/100</div>
        {trend && (
          <div className={`gauge-trend ${trend.direction}`}>
            {trend.direction === 'up' && '↑'} {trend.change.toFixed(1)}% from last week
          </div>
        )}
      </div>
    </div>
  );
};
