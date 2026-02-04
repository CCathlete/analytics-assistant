import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Proxying API requests to the backend
      '/api': {
        target: 'http://192.168.100.36:8383',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
