import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/axios'

interface AuthUser {
  id: number
  username: string
  email: string
  fullName: string
  role: string
  active: boolean
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const user = ref<AuthUser | null>(null)

  async function login(username: string, password: string) {
    const res = await api.post('/auth/login', { username, password })
    const data = res.data.data
    token.value = data.token
    user.value = data.user
    localStorage.setItem('auth_token', data.token)
  }

  async function ensureLogin() {
    if (token.value) return
    // Temporary mock auth for dev.
    await login('ahnyk', 'password')
  }

  return { token, user, login, ensureLogin }
})
