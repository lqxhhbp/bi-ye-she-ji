export const PACKAGE_STATUS = {
  0: { label: '待取', type: 'warning' },
  1: { label: '已取', type: 'success' },
  2: { label: '已撤销', type: 'info' }
}

export const CROWD_LEVEL = {
  0: { label: '空闲', type: 'success', color: '#67c23a' },
  1: { label: '一般', type: 'warning', color: '#e6a23c' },
  2: { label: '拥挤', type: 'danger', color: '#f56c6c' }
}

export function formatDateTime(value, withSeconds = false) {
  if (!value) {
    return ''
  }
  const text = String(value).replace('T', ' ')
  return withSeconds ? text.slice(0, 19) : text.slice(0, 16)
}

export function formatDate(value) {
  if (!value) {
    return ''
  }
  return String(value).slice(0, 10)
}

export function packageStatusMeta(status) {
  return PACKAGE_STATUS[Number(status)] || { label: '未知', type: 'info' }
}

export function crowdLevelMeta(level) {
  return CROWD_LEVEL[Number(level)] || { label: '未知', type: 'info', color: '#909399' }
}
