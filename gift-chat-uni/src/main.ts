import { createSSRApp } from "vue";
import App from "./App.vue";

// #ifdef H5
if (import.meta.env.PROD && "serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker
      .register("/static/pwa/service-worker.js", { scope: "/" })
      .catch((error) => {
        console.warn("Service worker registration failed", error);
      });
  });
}
// #endif

export function createApp() {
  const app = createSSRApp(App);
  return {
    app,
  };
}
