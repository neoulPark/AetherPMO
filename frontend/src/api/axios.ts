import axios from 'axios'

// 로컬: '/api/v1' (vite 프록시 → localhost:8080)
// 배포(Vercel 등): VITE_API_BASE_URL 에 백엔드 주소 지정
//   예) https://aetherpmo-backend.onrender.com/api/v1
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token')
    }
    return Promise.reject(error)
  }
)

export default api
