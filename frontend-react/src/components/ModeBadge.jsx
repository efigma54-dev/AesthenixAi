/**
 * ModeBadge — shows current analysis mode (AI / Rule / Hybrid)
 * 
 * This is a TRUST feature that explains:
 * - Why analysis was fast
 * - Why AI was skipped
 * - Current engine state
 * 
 * Huge trust improvement for users.
 */

export default function ModeBadge({ mode = 'hybrid', size = 'normal' }) {
  const modes = {
    ai: {
      icon: '🧠',
      label: 'AI Mode',
      color: '#7f5af0',
      bg: 'rgba(127, 90, 240, 0.1)',
      description: 'Deep AI analysis with local model'
    },
    rule: {
      icon: '⚡',
      label: 'Rule Mode',
      color: '#10b981',
      bg: 'rgba(16, 185, 129, 0.1)',
      description: 'Fast rule-based analysis'
    },
    hybrid: {
      icon: '🛡',
      label: 'Hybrid Mode',
      color: '#f59e0b',
      bg: 'rgba(245, 158, 11, 0.1)',
      description: 'Rule engine + AI enhancement'
    }
  };

  const config = modes[mode] || modes.hybrid;
  const isSmall = size === 'small';

  return (
    <div
      title={config.description}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: isSmall ? 4 : 6,
        padding: isSmall ? '4px 8px' : '6px 12px',
        borderRadius: 6,
        background: config.bg,
        border: `1px solid ${config.color}33`,
        fontSize: isSmall ? 10 : 11,
        fontWeight: 600,
        color: config.color,
        cursor: 'help',
      }}
    >
      <span style={{ fontSize: isSmall ? 12 : 14 }}>{config.icon}</span>
      <span>{config.label}</span>
    </div>
  );
}
