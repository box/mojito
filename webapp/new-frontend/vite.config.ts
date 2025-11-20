import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Build into Spring Boot's static resources so it serves at /n/
export default defineConfig({
  base: '/n/',
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Forward API calls to the Spring Boot backend
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    // Output to webapp/target/classes/public/n so Spring Boot serves it
    outDir: '../target/classes/public/n',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
    css: true,
  },
});
