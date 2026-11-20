<template>
  <div class="campus-map">
    <div ref="container" class="map-container"></div>
    <div v-if="statusMessage" class="map-status">
      <span>{{ statusMessage }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'

const props = defineProps({
  stations: {
    type: Array,
    default: () => []
  },
  center: {
    type: Object,
    default: null
  },
  zoom: {
    type: Number,
    default: 17
  },
  routeStart: {
    type: Object,
    default: null
  },
  routeEnd: {
    type: Object,
    default: null
  },
  routeMode: {
    type: String,
    default: 'walking'
  },
  pickMode: {
    type: Boolean,
    default: false
  },
  city: {
    type: String,
    default: '中山市'
  }
})

const emit = defineEmits(['station-click', 'route-result', 'route-error', 'location-pick'])

const container = ref(null)
const statusMessage = ref('')
const ak = import.meta.env.VITE_BMAP_AK?.trim()
let map = null
let sdkPromise = null
let routeService = null

const effectiveCenter = computed(() => {
  if (isValidCoordinate(props.center)) {
    return {
      lng: Number(props.center.lng),
      lat: Number(props.center.lat)
    }
  }

  const station = props.stations.find(isValidCoordinate)
  return station
    ? { lng: Number(station.lng), lat: Number(station.lat) }
    : null
})

function isValidCoordinate(point) {
  return point
    && Number.isFinite(Number(point.lng))
    && Number.isFinite(Number(point.lat))
}

function loadBMapGL() {
  if (window.BMapGL?.Map) {
    return Promise.resolve(window.BMapGL)
  }
  if (sdkPromise) {
    return sdkPromise
  }
  if (!ak) {
    return Promise.reject(new Error('未配置 VITE_BMAP_AK'))
  }

  sdkPromise = new Promise((resolve, reject) => {
    const callbackName = `__campusBMapReady_${Date.now()}`
    const script = document.createElement('script')
    const timeoutId = window.setTimeout(() => {
      fail('百度地图 SDK 加载超时')
    }, 15000)

    function cleanup() {
      window.clearTimeout(timeoutId)
      delete window[callbackName]
    }

    window[callbackName] = () => {
      cleanup()
      if (window.BMapGL?.Map) {
        resolve(window.BMapGL)
      } else {
        sdkPromise = null
        reject(new Error('百度地图 SDK 初始化失败'))
      }
    }

    script.src = `https://api.map.baidu.com/api?v=1.0&type=webgl&ak=${encodeURIComponent(ak)}&callback=${callbackName}`
    script.async = true
    script.onerror = () => {
      fail('百度地图 SDK 网络加载失败')
    }

    function fail(message) {
      cleanup()
      sdkPromise = null
      reject(new Error(message))
    }

    document.head.appendChild(script)
  })

  return sdkPromise
}

async function renderMap() {
  if (!container.value) {
    return
  }
  if (!ak) {
    statusMessage.value = '未配置百度地图 AK'
    return
  }
  if (!effectiveCenter.value) {
    statusMessage.value = '暂无可显示的快递站点'
    return
  }

  try {
    const BMapGL = await loadBMapGL()
    const center = new BMapGL.Point(effectiveCenter.value.lng, effectiveCenter.value.lat)

    if (!map) {
      map = new BMapGL.Map(container.value)
      map.enableScrollWheelZoom(true)
      map.addEventListener('click', handleMapClick)
    }

    map.centerAndZoom(center, props.zoom)
    clearRoute()
    map.clearOverlays()

    props.stations.filter(isValidCoordinate).forEach(station => {
      const point = new BMapGL.Point(Number(station.lng), Number(station.lat))
      const crowd = crowdStyle(station.crowdLevel)
      const circle = new BMapGL.Circle(point, 35, {
        strokeColor: crowd.color,
        strokeWeight: 2,
        strokeOpacity: 0.9,
        fillColor: crowd.color,
        fillOpacity: 0.22
      })
      map.addOverlay(circle)
      const marker = new BMapGL.Marker(point)
      map.addOverlay(marker)
      marker.addEventListener('click', () => {
        const title = escapeHtml(station.name || '快递站点')
        const address = escapeHtml(station.address || '')
        const score = Number(station.crowdScore || 0).toFixed(1)
        const content = `<div style="padding:8px;line-height:1.6">
          <strong>${title}</strong><br>
          ${address}<br>
          当前状态：<span style="color:${crowd.color};font-weight:600">${crowd.label}</span>
          <span style="margin-left:8px">指数 ${score}</span>
        </div>`
        map.openInfoWindow(new BMapGL.InfoWindow(content), point)
        emit('station-click', station)
      })
    })

    if (isValidCoordinate(props.routeStart) && isValidCoordinate(props.routeEnd)) {
      searchRoute(BMapGL)
    }

    statusMessage.value = ''
  } catch (error) {
    statusMessage.value = error.message || '地图加载失败'
  }
}

function searchRoute(BMapGL) {
  const RouteType = props.routeMode === 'driving'
    ? BMapGL.DrivingRoute
    : BMapGL.WalkingRoute
  if (!RouteType) {
    emit('route-error', '当前百度地图版本不支持路线规划')
    return
  }

  try {
    routeService = new RouteType(map, {
      renderOptions: {
        map,
        autoViewport: true
      },
      onSearchComplete: () => {
        if (!routeService || routeService.getStatus() !== 0) {
          emit('route-error', '未找到可用路线')
          return
        }
        const plan = routeService.getResults()?.getPlan(0)
        if (!plan) {
          emit('route-error', '未找到可用路线')
          return
        }
        emit('route-result', {
          mode: props.routeMode,
          distance: plan.getDistance?.(true) || '',
          duration: plan.getDuration?.(true) || ''
        })
      }
    })
    routeService.search(
      new BMapGL.Point(Number(props.routeStart.lng), Number(props.routeStart.lat)),
      new BMapGL.Point(Number(props.routeEnd.lng), Number(props.routeEnd.lat))
    )
  } catch {
    emit('route-error', '路线规划失败')
  }
}

function clearRoute() {
  if (routeService?.clearResults) {
    routeService.clearResults()
  }
  routeService = null
}

async function geocodeAddress(address) {
  const value = String(address || '').trim()
  if (!value) {
    throw new Error('请先填写地址')
  }
  const BMapGL = await loadBMapGL()
  const Geocoder = BMapGL.Geocoder
  if (!Geocoder) {
    throw new Error('当前百度地图版本不支持地址解析')
  }

  return new Promise((resolve, reject) => {
    const geocoder = new Geocoder()
    geocoder.getPoint(value, point => {
      if (!point) {
        reject(new Error('未能解析该地址，请检查地址是否完整'))
        return
      }
      resolve({
        lng: Number(point.lng),
        lat: Number(point.lat)
      })
    }, props.city)
  })
}

function handleMapClick(event) {
  if (!props.pickMode || !map) {
    return
  }
  const point = event?.point || event?.latLng
  if (!point) {
    return
  }
  const baseLocation = {
    lng: Number(point.lng),
    lat: Number(point.lat)
  }
  emit('location-pick', baseLocation)

  const Geocoder = window.BMapGL?.Geocoder
  if (!Geocoder) {
    return
  }
  const geocoder = new Geocoder()
  geocoder.getLocation(point, result => {
    if (result?.address) {
      emit('location-pick', {
        ...baseLocation,
        address: result.address
      })
    }
  }, props.city)
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function crowdStyle(level) {
  const value = Number(level)
  if (value === 2) {
    return { color: '#f56c6c', label: '拥挤' }
  }
  if (value === 1) {
    return { color: '#e6a23c', label: '一般' }
  }
  return { color: '#67c23a', label: '空闲' }
}

watch(
  [
    () => props.center,
    () => props.stations,
    () => props.routeStart,
    () => props.routeEnd,
    () => props.routeMode
  ],
  () => renderMap(),
  { deep: true }
)

defineExpose({
  geocodeAddress
})

onMounted(async () => {
  await nextTick()
  renderMap()
})
</script>

<style scoped>
.campus-map {
  position: relative;
  min-height: 520px;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #f5f7fa;
}

.map-container {
  width: 100%;
  height: 520px;
}

.map-status {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #606266;
  text-align: center;
  background: rgb(245 247 250 / 92%);
  pointer-events: none;
}
</style>
