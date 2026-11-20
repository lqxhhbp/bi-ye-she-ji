import request from './request'

export function getAudits(params) {
  return request.get('/audits', { params })
}

export function reviewAudit(id, status) {
  return request.put(`/audits/${id}`, { status })
}

export function getAlerts(params) {
  return request.get('/alerts', { params })
}

export function handleAlert(id) {
  return request.put(`/alerts/${id}/handle`)
}

export function getStationThroughput(days = 7) {
  return request.get('/stats/station-throughput', { params: { days } })
}

export function getOperatorWorkload(days = 30) {
  return request.get('/stats/operator-workload', { params: { days } })
}

export function getCrowdDistribution() {
  return request.get('/stats/crowd-distribution')
}

export function getOperationLogs(params) {
  return request.get('/logs', { params })
}
