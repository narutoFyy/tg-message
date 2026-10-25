import { createSSRApp } from "vue";
import App from "./App.vue";
import { watch } from 'vue';
import { privacyAccepted, ensurePrivacyRoute } from '@/utils/privacy';

// #ifdef H5
if (import.meta.env.PROD && "serviceWorker" in navigator) {
  const registerWorker = () => {
    navigator.serviceWorker
      .register("/static/pwa/service-worker.js", { scope: "/" })
      .catch((error) => {
        console.warn("Service worker registration failed", error);
      });
  };
  let workerStarted = false;
  const startWorker = () => {
    if (!privacyAccepted.value || workerStarted) return;
    workerStarted = true;
    if (document.readyState === 'complete') registerWorker();
    else window.addEventListener('load', registerWorker, { once: true });
  };
  startWorker();
  watch(privacyAccepted, startWorker);
}
// #endif

export function createApp() {
  const app = createSSRApp(App);
  app.mixin({
    onReady() { ensurePrivacyRoute(); },
    onShow() { setTimeout(ensurePrivacyRoute, 0); },
  });
  return {
    app,
  };
}
