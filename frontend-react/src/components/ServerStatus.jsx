import { useState, useEffect } from 'react';
import { checkHealth } from '../lib/api';

/**
 * Polls the backend health endpoint every 15s.
 * Shows a green dot when reachable, red when not.
 */
export default function ServerStatus() {
  const [status, setStatus] = useState('checking'); // 'checking' | 'up' | 'down'

  const check = async () => {
    const ok = await checkHealth();
    setStatus(ok ? 'up' : 'down');
  };

  useEffect(() => {
    check();
    const interval = setInterval(check, 15_000);
    return () => clearInterval(interval);
  }, []);

  const COLOR = { checking: '#4b5563', up: '#4ade80', down: '#f87171' };
  const LABEL = { checking: 'Checking…', up: 'Backend online', down: 'Backend offline' };
  const TITLE = {
    checking: 'Checking backend…',
    up: 'Backend is running on port 8080',
    down: 'Cannot reach backend. Run: ./mvnw spring-boot:run',
  };

  return (
    <div
      title={TITLE[status]}
      style={{
        display: 'flex', alignItems: 'center', gap: 5,
        padding: '3px 8px', borderRadius: 5, cursor: 'default',
        background: status === 'down' ? 'rgba(248,113,113,0.08)' : 'transparent',
        border: status === 'down' ? '1px solid rgba(248,113,113,0.2)' : '1px solid transparent',
        transition: 'all 300ms ease',
      }}
    >
      <span style={{
        width: 6, height: 6, borderRadius: '50%',
        background: COLOR[status],
        display: 'inline-block',
        animation: status === 'checking' ? 'pulse-dot 1.5s ease-in-out infinite' : 'none',
        boxShadow: status === 'up' ? '0 0 6px rgba(74,222,128,0.5)' : 'none',
      }} />
      <span style={{ fontSize: 11, color: COLOR[status] }}>{LABEL[status]}</span>
      {status === 'down' && (
        <button
          onClick={check}
          style={{
            fontSize: 10, padding: '1px 6px', borderRadius: 3, cursor: 'pointer',
            background: 'rgba(248,113,113,0.12)', color: '#f87171',
            border: '1px solid rgba(248,113,113,0.25)', marginLeft: 2,
          }}
        >retry</button>
      )}
    </div>
  );
}
