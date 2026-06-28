import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import { useAuthStore } from './stores/auth'
import './assets/styles/global.css'

async function bootstrap() {
  const app = createApp(App)
  app.use(createPinia())
  app.use(router)
  try {
    await useAuthStore().ensureLogin()
  } catch (e) {
    console.error('Auto-login failed', e)
  }
  app.mount('#app')
}

bootstrap()
