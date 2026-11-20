import request from './request'

export function getStationDaily(days = 7) {
  return request.get('/stats/station-daily', { params: { days } })
}
