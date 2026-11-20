<template>
  <div class="page" v-loading="loading">
    <header class="page-heading">
      <div>
        <div class="eyebrow">站点运营</div>
        <h1>站点工作台</h1>
        <p>汇总今日包裹、近 7 天趋势和当前拥挤度。</p>
      </div>
      <el-button type="primary" @click="$router.push('/station/verify')">开始核销</el-button>
    </header>

    <div class="stat-grid">
      <el-card v-for="item in statCards" :key="item.key" shadow="never">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ stats[item.key] ?? 0 }}</div>
      </el-card>
    </div>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title-row">
            <span>近 7 天包裹趋势</span>
            <el-button link type="primary" :loading="loading" @click="loadAll">刷新</el-button>
          </div>
        </template>
        <div ref="trendChartRef" class="chart"></div>
      </el-card>

      <div class="side-column">
        <el-card shadow="never">
          <template #header>当前拥挤度</template>
          <div class="crowd-summary">
            <el-tag :type="currentCrowdMeta.type" size="large">
              {{ currentCrowdMeta.label }}
            </el-tag>
            <strong>{{ Number(crowd?.crowdScore || 0).toFixed(1) }}</strong>
            <span>近 10 分钟 {{ crowd?.recentReportCount || 0 }} 次上报</span>
          </div>
          <div class="calibrate-buttons">
            <el-button type="success" plain :loading="calibrating" @click="calibrate(0)">
              空闲
            </el-button>
            <el-button type="warning" plain :loading="calibrating" @click="calibrate(1)">
              一般
            </el-button>
            <el-button type="danger" plain :loading="calibrating" @click="calibrate(2)">
              拥挤
            </el-button>
          </div>
          <p class="calibrate-tip">站长校准同样受 10 分钟频率限制。</p>
        </el-card>

        <el-card shadow="never">
          <template #header>快捷入口</template>
          <div class="quick-links">
            <el-button @click="$router.push('/station/packages/new')">包裹录入</el-button>
            <el-button @click="$router.push('/station/packages/import')">批量导入</el-button>
            <el-button @click="$router.push('/station/packages')">包裹列表</el-button>
            <el-button @click="$router.push('/station/verify')">取件核销</el-button>
            <el-button @click="$router.push('/station/info')">站点信息</el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { calibrateCrowd, getCrowdSummary } from '@/api/idle'
import { getTodayStats } from '@/api/packages'
import { getStationDaily } from '@/api/stats'
import { useUserStore } from '@/stores/user'
import { crowdLevelMeta } from '@/utils/format'

echarts.use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const userStore = useUserStore()
const loading = ref(false)
const calibrating = ref(false)
const stats = reactive({})
const daily = ref([])
const crowd = ref(null)
const trendChartRef = ref()
let trendChart
const statCards = [
  { key: 'inbound', label: '今日上架' },
  { key: 'pending', label: '当前待取' },
  { key: 'taken', label: '今日已取' },
  { key: 'revoked', label: '今日撤销' }
]
const currentCrowdMeta = computed(() => crowdLevelMeta(crowd.value?.crowdLevel))

async function loadAll() {
  loading.value = true
  try {
    const [todayResponse, dailyResponse, crowdResponse] = await Promise.all([
      getTodayStats(),
      getStationDaily(7),
      getCrowdSummary()
    ])
    Object.assign(stats, todayResponse.data || {})
    daily.value = dailyResponse.data || []
    crowd.value = (crowdResponse.data || []).find(
      item => item.stationId === userStore.user?.stationId
    ) || null
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!trendChartRef.value) return
  trendChart ||= echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['上架', '已取', '撤销'], top: 0 },
    grid: { left: 42, right: 22, top: 42, bottom: 38 },
    xAxis: {
      type: 'category',
      data: daily.value.map(item => String(item.date).slice(5))
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '上架',
        type: 'bar',
        data: daily.value.map(item => item.inboundCount),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '已取',
        type: 'line',
        smooth: true,
        data: daily.value.map(item => item.takenCount),
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '撤销',
        type: 'line',
        data: daily.value.map(item => item.revokedCount),
        itemStyle: { color: '#f56c6c' }
      }
    ]
  }, true)
}

async function calibrate(level) {
  calibrating.value = true
  try {
    await calibrateCrowd({ level })
    ElMessage.success('拥挤度校准成功')
    await loadAll()
  } finally {
    calibrating.value = false
  }
}

function resizeChart() {
  trendChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  trendChart?.dispose()
})
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
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
  font-size: 28px;
}

.page-heading p {
  margin: 7px 0 0;
  color: #909399;
  font-size: 13px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin-top: 9px;
  color: #303133;
  font-size: 32px;
  font-weight: 800;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(460px, 1.5fr) minmax(300px, 0.8fr);
  gap: 18px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #303133;
  font-weight: 700;
}

.chart {
  height: 340px;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 16px;
}

.crowd-summary {
  display: grid;
  justify-items: start;
  gap: 8px;
}

.crowd-summary strong {
  color: #303133;
  font-size: 38px;
  line-height: 1;
}

.crowd-summary span {
  color: #909399;
  font-size: 12px;
}

.calibrate-buttons {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
  margin-top: 16px;
}

.calibrate-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.calibrate-tip {
  margin: 10px 0 0;
  color: #909399;
  font-size: 11px;
}

.quick-links {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.quick-links :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 980px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .page {
    padding: 18px 14px;
  }

  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .page-heading {
    align-items: center;
  }

  .chart {
    height: 280px;
  }
}
</style>
