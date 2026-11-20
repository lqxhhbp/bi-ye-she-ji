import request from './request'

export function getCrowdSummary() {
  return request.get('/idle/summary')
}

export function reportCrowd(data) {
  return request.post('/idle/reports', data)
}

export function calibrateCrowd(data) {
  return request.post('/idle/reports/calibrate', data)
}

export function getRecentReports(stationId) {
  return request.get('/idle/reports/recent', { params: { stationId } })
}

export function getCrowdTrend(stationId, hours = 1) {
  return request.get('/idle/reports/trend', { params: { stationId, hours } })
}
