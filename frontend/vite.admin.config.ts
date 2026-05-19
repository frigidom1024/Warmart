import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    {
      name: 'admin-html-fallback',
      configureServer(server) {
        server.middlewares.use((req, res, next) => {
          if (req.url === '/') {
            req.url = '/admin.html'
          } else if (
            req.url &&
            !req.url.startsWith('/@') &&
            !req.url.startsWith('/src/') &&
            !req.url.startsWith('/node_modules/') &&
            !req.url.startsWith('/favicon') &&
            !req.url.includes('.')
          ) {
            req.url = '/admin.html'
          }
          next()
        })
      }
    }
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src/admin', import.meta.url))
    }
  },
  optimizeDeps: {
    entries: ['./admin.html']
  },
  server: {
    port: 5174,
    open: '/'
  },
  build: {
    outDir: 'dist/admin',
    rollupOptions: {
      input: fileURLToPath(new URL('./admin.html', import.meta.url))
    }
  }
})
