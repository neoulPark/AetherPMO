import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// GitHub Pages 프로젝트 사이트는 /<repo>/ 하위 경로로 서비스됨.
// 배포 워크플로에서 BASE_PATH=/AetherPMO/ 를 주입하고, 로컬 개발은 '/' 사용.
export default defineConfig({
  base: process.env.BASE_PATH || '/',
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') }
  },
  server: { port: 5173 }
})
