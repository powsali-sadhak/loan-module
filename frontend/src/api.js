import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('loan_jwt')
  if (token) config.headers.Authorization = `Bearer ${token}`

  // Local demo identity. Backend endpoints that use the authenticated
  // principal ignore this header; demo-mode endpoints resolve the selected
  // database user from it because the JWT filter is intentionally disabled.
  try {
    const demoUser = JSON.parse(localStorage.getItem('loan_demo_user') || 'null')
    if (demoUser?.id) config.headers['X-Demo-User-Id'] = String(demoUser.id)
  } catch {
    // Ignore malformed demo identity state; the backend will return a clear error.
  }

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
