import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En desarrollo, /api y /ws se redirigen al gateway (docker compose) en el puerto 8080
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/ws': 'http://localhost:8080'
    }
  }
})
