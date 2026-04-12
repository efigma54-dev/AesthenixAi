import { useState, lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Sidebar from './components/Sidebar';
import Topbar from './components/Topbar';
import CommandPalette from './components/CommandPalette';
import ErrorBoundary from './components/ErrorBoundary';
import './styles/global.css';

// Lazy-load every page — only the active route is downloaded
const Landing = lazy(() => import('./pages/Landing'));
const EditorView = lazy(() => import('./pages/EditorView'));
const GithubView = lazy(() => import('./pages/GithubView'));
const HistoryView = lazy(() => import('./pages/HistoryView'));
const RepoScanView = lazy(() => import('./pages/RepoScanView'));

function PageFallback() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '60vh' }}>
      <div style={{ width: 20, height: 20, borderRadius: '50%', border: '2px solid rgba(127,90,240,0.3)', borderTopColor: '#7f5af0', animation: 'spin 0.7s linear infinite' }} />
    </div>
  );
}

function Dashboard() {
  const [active, setActive] = useState('editor');
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [runTrigger, setRunTrigger] = useState(0);
  const [clearTrigger, setClearTrigger] = useState(0);
  // Restore from history → editor
  const [restoreItem, setRestoreItem] = useState(null);

  const handleAction = (action) => {
    if (action.startsWith('nav:')) { setActive(action.slice(4)); return; }
    if (action === 'run') { setRunTrigger((n) => n + 1); return; }
    if (action === 'clear') { setClearTrigger((n) => n + 1); return; }
    if (action === 'link:docs') { window.open('https://github.com', '_blank'); return; }
    if (action === 'link:github') { window.open('https://github.com', '_blank'); return; }
  };

  const handleRestoreToEditor = (item) => {
    setRestoreItem(item);
    setActive('editor');
  };

  return (
    <div style={{ display: 'flex', height: '100vh', background: '#0b0b0f', overflow: 'hidden' }}>
      <Sidebar active={active} setActive={setActive} />

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Topbar active={active} onOpenPalette={() => setPaletteOpen(true)} />

        <main style={{ flex: 1, overflowY: 'auto', padding: '16px 20px 24px' }}>
          <Suspense fallback={<PageFallback />}>
            {active === 'editor' && (
              <EditorView
                runTrigger={runTrigger}
                clearTrigger={clearTrigger}
                restoreItem={restoreItem}
                onRestoreConsumed={() => setRestoreItem(null)}
              />
            )}
            {active === 'repo-scan' && <RepoScanView />}
            {active === 'github' && <GithubView />}
            {active === 'history' && <HistoryView onRestoreToEditor={handleRestoreToEditor} />}
          </Suspense>
        </main>
      </div>

      <CommandPalette
        open={paletteOpen}
        onOpenChange={setPaletteOpen}
        onAction={handleAction}
      />
    </div>
  );
}

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Suspense fallback={<PageFallback />}>
          <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/app" element={<Dashboard />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ErrorBoundary>
  );
}
