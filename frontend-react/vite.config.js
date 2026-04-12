import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],

  server: {
    port: 3000,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },

  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        // Rolldown (Vite 8) requires manualChunks as a function, not an object
        manualChunks(id) {
          if (id.includes('@monaco-editor') || id.includes('monaco-editor'))
            return 'monaco';
          if (id.includes('react-diff-viewer'))
            return 'diff-viewer';
          if (id.includes('node_modules/react/') || id.includes('node_modules/react-dom/') || id.includes('react-router'))
            return 'react-vendor';
        },
      },
    },
  },
})
