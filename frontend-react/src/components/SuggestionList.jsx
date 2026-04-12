export default function SuggestionList({ suggestions }) {
  if (!suggestions?.length) return null;
  return (
    <div className="card fade-in" style={{ padding: '10px 12px' }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 8 }}>
        Suggestions
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
        {suggestions.map((s, i) => (
          <div key={i} className="hover-row fade-in"
            style={{ animationDelay: `${i * 40}ms`, display: 'flex', gap: 8, padding: '6px 8px', alignItems: 'flex-start' }}>
            <span style={{ fontSize: 11, color: '#7f5af0', flexShrink: 0, marginTop: 1 }}>→</span>
            <p style={{ fontSize: 12, color: '#9ca3af', lineHeight: 1.5, margin: 0 }}>{s}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
