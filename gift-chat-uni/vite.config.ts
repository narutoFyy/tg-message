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
        server.middlewares.use((request, _response, next) => {
          if (request.url === '/install' || request.url === '/install/') {
            request.url = '/static/install/index.html'
          }
          next()
        })
      }
    }
  ],
});
