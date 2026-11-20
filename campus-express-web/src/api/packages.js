import request from './request'

export function createPackage(data) {
  return request.post('/packages', data)
}

export function importPackages(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/packages/import', formData)
}

export function downloadImportTemplate() {
  return request.get('/packages/import/template', { responseType: 'blob' })
}

export function getPackages(params) {
  return request.get('/packages', { params })
}

export function getPackage(id) {
  return request.get(`/packages/${id}`)
}

export function revokePackage(id) {
  return request.put(`/packages/${id}/revoke`)
}

export function exportPackages() {
  return request.get('/packages/export', { responseType: 'blob' })
}

export function queryPackagesByPhone(phone) {
  return request.get('/packages/query', { params: { phone } })
}

export function queryPackageByPickupCode(pickupCode) {
  return request.get(`/packages/query/${encodeURIComponent(pickupCode)}`)
}

export function getMyPackages(params) {
  return request.get('/packages/mine', { params })
}

export function getPackageTimeline(id) {
  return request.get(`/packages/${id}/timeline`)
}

export function queryVerifiablePackage(pickupCode) {
  return request.get(`/packages/verify/${encodeURIComponent(pickupCode)}`)
}

export function verifyPackage(pickupCode) {
  return request.post('/packages/verify', { pickupCode })
}

export function getTakenPackages(page = 1, pageSize = 20) {
  return request.get('/packages/taken', { params: { page, pageSize } })
}

export function getOverduePackages(page = 1, pageSize = 20) {
  return request.get('/packages/overdue', { params: { page, pageSize } })
}

export function getTodayStats() {
  return request.get('/packages/stats/today')
}
