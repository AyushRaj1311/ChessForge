import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
  },
  server: {
    port: 3000,
    proxy: {
      '/api': 'http://localhost:1111',
      '/ws': {
        target: 'http://localhost:1111',
        ws: true,
      },
    },
  },
});
