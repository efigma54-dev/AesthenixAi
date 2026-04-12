import { Component } from 'react';

/**
 * Catches React render errors so the whole app doesn't crash.
 * Shows a minimal recovery UI with a reload button.
 */
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // In production you'd send this to Sentry / LogRocket
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;

    return (
      <div
        role="alert"
        aria-live="assertive"
        style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', height: '100vh', gap: 12,
          background: '#0b0b0f', color: '#e2e8f0', textAlign: 'center', padding: 32,
        }}
      >
        <div style={{ fontSize: 32, opacity: 0.3 }}>⚠</div>
        <p style={{ fontSize: 14, fontWeight: 600 }}>Something went wrong</p>
        <p style={{ fontSize: 12, color: '#6b7280', maxWidth: 400 }}>
          {this.state.error.message ?? 'An unexpected error occurred in the UI.'}
        </p>
        <button
          onClick={() => window.location.reload()}
          style={{
            marginTop: 8, padding: '8px 20px', borderRadius: 8,
            background: '#7f5af0', color: '#fff', border: 'none',
            fontSize: 13, fontWeight: 600, cursor: 'pointer',
          }}
        >
          Reload page
        </button>
      </div>
    );
  }
}
