import request from './request'

export function getStations(params) {
  return request.get('/stations', { params })
}

export function getStation(id) {
  return request.get(`/stations/${id}`)
}

export function createStation(data) {
  return request.post('/stations', data)
}

export function updateStation(id, data) {
  return request.put(`/stations/${id}`, data)
}

export function disableStation(id) {
  return request.delete(`/stations/${id}`)
}

export function getCompanies() {
  return request.get('/companies')
}

export function updateStationCompanies(id, companies) {
  return request.put(`/stations/${id}/companies`, companies)
}
