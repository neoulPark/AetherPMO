import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import router from './router'
import App from './App.vue'
import { useAuthStore } from './stores/auth'
import './assets/styles/global.css'
import './assets/styles/element-overrides.css'

async function bootstrap() {
  document.documentElement.classList.add('dark')
  const app = createApp(App)
  app.use(createPinia())
  app.use(ElementPlus)
  app.use(router)
  try {
    await useAuthStore().ensureLogin()
  } catch (e) {
    console.error('Auto-login failed', e)
  }
  app.mount('#app')
}

bootstrap()
