<template>
  <div class="map-page">
    <header class="map-heading">
      <div>
        <div class="eyebrow">校园站点</div>
        <h1>快递点地图</h1>
        <p>选择站点查看步行或驾车路线，颜色代表当前拥挤程度。</p>
      </div>
      <el-button :loading="loading" circle aria-label="刷新站点" @click="loadStations">刷</el-button>
    </header>

    <div class="map-shell">
      <CampusMap
        ref="mapRef"
        :stations="stations"
        :center="mapCenter"
        :route-start="routeStart"
        :route-end="routeEnd"
        :route-mode="routeMode"
        @station-click="selectStation"
        @route-result="handleRouteResult"
        @route-error="handleRouteError"
      />
    </div>

    <div class="route-toolbar">
      <el-radio-group v-model="routeMode" size="small">
        <el-radio-button value="walking">步行</el-radio-button>
        <el-radio-button value="driving">驾车</el-radio-button>
      </el-radio-group>
      <div class="origin-actions">
        <span>起点：{{ originLabel }}</span>
        <el-button link type="primary" :loading="locating" @click="useMyLocation">
          使用我的位置
        </el-button>
        <el-button v-if="origin" link @click="useCampusCenter">校园中心</el-button>
      </div>
      <div v-if="routeInfo" class="route-info">
        <strong>{{ routeInfo.distance }}</strong>
        <span>约 {{ routeInfo.duration }}</span>
      </div>
      <div v-else-if="routeError" class="route-error">{{ routeError }}</div>
    </div>

    <div class="legend">
      <span><i class="green"></i>空闲</span>
      <span><i class="yellow"></i>一般</span>
      <span><i class="red"></i>拥挤</span>
      <span>默认视角：校园中心</span>
    </div>

    <section class="station-section">
      <div class="section-heading">
        <h2>附近快递点</h2>
        <span>{{ stations.length }} 个站点</span>
      </div>
      <div class="station-chips">
        <button
          v-for="station in stations"
          :key="station.id"
          type="button"
          :class="{ active: selectedStation?.id === station.id }"
          @click="selectStation(station)"
        >
          <span :style="{ background: crowdLevelMeta(station.crowdLevel).color }"></span>
          {{ shortName(station.name) }}
        </button>
      </div>
    </section>

    <section v-if="selectedStation" ref="detailRef" class="station-detail">
      <div class="detail-top">
        <div>
          <span class="detail-label">当前查看</span>
          <h2>{{ selectedStation.name }}</h2>
        </div>
        <el-tag :type="crowdLevelMeta(selectedStation.crowdLevel).type">
          {{ crowdLevelMeta(selectedStation.crowdLevel).label }}
        </el-tag>
      </div>
      <p class="address">{{ selectedStation.address }}</p>
      <div class="detail-meta">
        <div>
          <span>营业时间</span>
          <strong>{{ selectedStation.businessHours || '以现场为准' }}</strong>
        </div>
        <div>
          <span>拥挤指数</span>
          <strong>{{ Number(selectedStation.crowdScore || 0).toFixed(1) }}</strong>
        </div>
      </div>
      <el-button type="primary" class="report-button" @click="goToReport">
        上报该站点拥挤度
      </el-button>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CampusMap from '@/components/CampusMap.vue'
import { getMapStations } from '@/api/map'
import { crowdLevelMeta } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const stations = ref([])
const selectedStation = ref(null)
const loading = ref(false)
const locating = ref(false)
const detailRef = ref()
const mapRef = ref()
const origin = ref(null)
const originLabel = ref('校园中心')
const routeMode = ref('walking')
const routeInfo = ref(null)
const routeError = ref('')
const mapCenter = computed(() => selectedStation.value
  ? { lng: selectedStation.value.lng, lat: selectedStation.value.lat }
  : null)
const campusCenter = computed(() => {
  const validStations = stations.value.filter(item => (
    Number.isFinite(Number(item.lng)) && Number.isFinite(Number(item.lat))
  ))
  if (!validStations.length) {
    return { lng: 113.3973, lat: 22.5349 }
  }
  return {
    lng: validStations.reduce((sum, item) => sum + Number(item.lng), 0) / validStations.length,
    lat: validStations.reduce((sum, item) => sum + Number(item.lat), 0) / validStations.length
  }
})
const routeStart = computed(() => origin.value || campusCenter.value)
const routeEnd = computed(() => selectedStation.value
  ? { lng: selectedStation.value.lng, lat: selectedStation.value.lat }
  : null)

function selectStation(station) {
  selectedStation.value = station
  routeInfo.value = null
  routeError.value = ''
  nextTick(() => {
    detailRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  })
}

function useCampusCenter() {
  origin.value = null
  originLabel.value = '校园中心'
  routeInfo.value = null
  routeError.value = ''
}

function useMyLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位，已使用校园中心')
    useCampusCenter()
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    position => {
      locating.value = false
      origin.value = {
        lng: Number(position.coords.longitude),
        lat: Number(position.coords.latitude)
      }
      originLabel.value = '我的位置'
      routeInfo.value = null
      routeError.value = ''
    },
    () => {
      locating.value = false
      ElMessage.warning('无法获取当前位置，已使用校园中心')
      useCampusCenter()
    },
    {
      enableHighAccuracy: true,
      timeout: 8000,
      maximumAge: 60000
    }
  )
}

function handleRouteResult(result) {
  routeInfo.value = result
  routeError.value = ''
}

function handleRouteError(message) {
  routeInfo.value = null
  routeError.value = message
}

function shortName(name) {
  if (!name) return ''
  if (name.length <= 12) return name
  return `${name.slice(0, 10)}…`
}

function goToReport() {
  router.push({
    path: '/student/report',
    query: { stationId: selectedStation.value.id }
  })
}

async function loadStations() {
  loading.value = true
  try {
    const response = await getMapStations()
    stations.value = response.data || []
    const stationId = Number(route.query.stationId)
    selectedStation.value = stations.value.find(item => item.id === stationId)
      || stations.value[0]
      || null
  } finally {
    loading.value = false
  }
}

onMounted(loadStations)
</script>

<style scoped>
.map-page {
  width: min(900px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 20px 18px 28px;
  box-sizing: border-box;
}

.map-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 15px;
}

.eyebrow {
  margin-bottom: 5px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

h1 {
  margin: 0;
  color: #303133;
  font-size: 27px;
}

.map-heading p {
  margin: 7px 0 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.map-shell {
  overflow: hidden;
  border-radius: 16px;
}

.map-shell :deep(.campus-map) {
  min-height: 440px;
}

.map-shell :deep(.map-container) {
  height: 440px;
}

.route-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 2px 4px;
}

.origin-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  color: #606266;
  font-size: 12px;
}

.origin-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.route-info {
  display: flex;
  align-items: baseline;
  gap: 7px;
  margin-left: auto;
  color: #67c23a;
}

.route-info span {
  color: #909399;
  font-size: 12px;
}

.route-error {
  margin-left: auto;
  color: #e6a23c;
  font-size: 12px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 2px 4px;
  color: #909399;
  font-size: 12px;
}

.legend span {
  display: flex;
  align-items: center;
  gap: 5px;
}

.legend i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.green {
  background: #67c23a;
}

.yellow {
  background: #e6a23c;
}

.red {
  background: #f56c6c;
}

.station-section {
  margin-top: 17px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-heading h2 {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.section-heading span {
  color: #909399;
  font-size: 12px;
}

.station-chips {
  display: flex;
  gap: 8px;
  margin-top: 11px;
  padding-bottom: 4px;
  overflow-x: auto;
  scrollbar-width: none;
}

.station-chips::-webkit-scrollbar {
  display: none;
}

.station-chips button {
  display: flex;
  flex: none;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 8px 13px;
  color: #606266;
  font: inherit;
  font-size: 13px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 999px;
}

.station-chips button.active {
  color: #337ecc;
  border-color: #409eff;
  background: #ecf5ff;
}

.station-chips button span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.station-detail {
  margin-top: 16px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 15px;
}

.detail-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-label {
  color: #909399;
  font-size: 11px;
}

.detail-top h2 {
  margin: 4px 0 0;
  color: #303133;
  font-size: 18px;
  line-height: 1.45;
}

.address {
  margin: 10px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 15px;
  padding: 13px;
  background: #f7f9fc;
  border-radius: 10px;
}

.detail-meta span,
.detail-meta strong {
  display: block;
}

.detail-meta span {
  margin-bottom: 4px;
  color: #909399;
  font-size: 11px;
}

.detail-meta strong {
  color: #303133;
  font-size: 13px;
}

.report-button {
  width: 100%;
  margin-top: 15px;
}

@media (max-width: 760px) {
  .map-page {
    padding: 16px 14px 24px;
  }

  .map-shell :deep(.campus-map),
  .map-shell :deep(.map-container) {
    min-height: 360px;
    height: 360px;
  }

  .route-info,
  .route-error {
    width: 100%;
    margin-left: 0;
  }
}

@media (max-width: 420px) {
  .detail-meta {
    grid-template-columns: 1fr;
  }
}
</style>
