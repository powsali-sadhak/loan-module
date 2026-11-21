import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('loan_jwt')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export function setToken(token) {
  if (token) localStorage.setItem('loan_jwt', token)
  else localStorage.removeItem('loan_jwt')
}

export function friendlyError(error) {
  return error?.response?.data?.message || error?.response?.data?.error ||
    error?.message || 'Something went wrong. Please try again.'
}

export default api