import axios from 'axios'
import { ElMessage } from 'element-plus'

function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (window.location.pathname !== '/login') {
    const redirect = encodeURIComponent(window.location.pathname + window.location.search)
    window.location.assign(`/login?redirect=${redirect}`)
  }
}

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => {
    const body = response.data
    if (body && body.code !== undefined && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        clearAuthAndRedirect()
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body
  },
  error => {
    ElMessage.error(error.message || '网络异常')
    if (error.response?.status === 401) {
      clearAuthAndRedirect()
    }
    return Promise.reject(error)
  }
)

export default request
