import request from './request'

export function getMapStations() {
  return request.get('/map/stations')
}

export function getNearbyStations(lng, lat) {
  return request.get('/map/nearby', { params: { lng, lat } })
}
