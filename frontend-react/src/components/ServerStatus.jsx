import { useState, useEffect } from 'react';
import { checkHealth } from '../lib/api';

/**
 * ServerStatus — shows backend connection status
 * 
 * Critical for trust when backend sleeps on Render free tier.
 * Users keep using the app instead of thinking it's broken.
 */
export default function ServerStatus() {
  const [status, setStatus] = useState('checking'); // checking | online | offline
  const [showBanner, setShowBanner] = useState(false);

  useEffect(() => {
    const check = async () => {
      const healthy = await checkHealth();
      setStatus(healthy ? 'online' : 'offline');
      setShowBanner(!healthy);
    };

    check();
    const interval = setInterval(check, 30000); // Check every 30s
    return () => clearInterval(interval);
  }, []);

  if (!showBanner) return null;

  return (
    <div
      role="alert"
      style={{
        position: 'fixed',
        top: 12,
        right: 12,
        zIndex: 1000,
        padding: '10px 16px',
        borderRadius: 8,
        background: 'rgba(245, 158, 11, 0.1)',
        border: '1px solid rgba(245, 158, 11, 0.3)',
        backdropFilter: 'blur(10px)',
        display: 'flex',
        alignItems: 'center',
        gap: 10,
        fontSize: 12,
        color: '#f59e0b',
        boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
        animation: 'slideIn 300ms ease',
      }}
    >
      <span style={{ fontSize: 16 }}>⚡</span>
      <div>
        <div style={{ fontWeight: 600, marginBottom: 2 }}>Running in Offline Rule Mode</div>
        <div style={{ fontSize: 11, opacity: 0.8 }}>
          AI unavailable — static analysis still active
        </div>
      </div>
      <button
        onClick={() => setShowBanner(false)}
        style={{
          background: 'none',
          border: 'none',
          color: '#f59e0b',
          cursor: 'pointer',
          fontSize: 16,
          padding: 4,
          opacity: 0.6,
        }}
        onMouseEnter={(e) => (e.target.style.opacity = 1)}
        onMouseLeave={(e) => (e.target.style.opacity = 0.6)}
      >
        ×
      </button>
    </div>
  );
}
