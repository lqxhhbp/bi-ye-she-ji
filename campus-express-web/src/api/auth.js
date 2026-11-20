import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function logout() {
  return request.post('/auth/logout')
}

export function getCurrentUser() {
  return request.get('/auth/me')
}

export function changePassword(data) {
  return request.put('/auth/password', data)
}

export function applyStation(data) {
  return request.post('/auth/station/apply', data)
}
