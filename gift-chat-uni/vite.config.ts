import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig({
  server: {
    host: '127.0.0.1',
    port: 5174,
    proxy: {
      '^/install/xcard\\.mobileios$': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      },
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:8081',
        ws: true,
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      }
    }
  },
  plugins: [
    uni(),
    {
      name: 'xcard-install-page',
      configureServer(server) {
        server.middlewares.use((request, response, next) => {
          if (request.url?.split('?')[0] === '/static/pwa/service-worker.js') {
            response.setHeader('Service-Worker-Allowed', '/')
            response.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate')
          }
          if (request.url === '/install' || request.url === '/install/') {
            request.url = '/static/install/index.html'
          }
          next()
        })
      }
    }
  ],
});
