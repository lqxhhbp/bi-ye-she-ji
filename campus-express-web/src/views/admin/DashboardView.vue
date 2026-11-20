<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h2>统计看板</h2>
        <p>站点吞吐、操作员工作量和实时拥挤度分布。</p>
      </div>
      <el-button :loading="loading" @click="loadAll">刷新数据</el-button>
    </div>

    <div class="stat-grid">
      <el-card v-for="item in summaryCards" :key="item.label" shadow="never">
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-value">{{ item.value }}</div>
      </el-card>
    </div>

    <div class="chart-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title-row">
            <span>近 {{ throughputDays }} 天站点吞吐量</span>
            <el-select v-model="throughputDays" size="small" @change="loadThroughput">
              <el-option label="近 7 天" :value="7" />
              <el-option label="近 14 天" :value="14" />
              <el-option label="近 30 天" :value="30" />
              <el-option label="近 90 天" :value="90" />
            </el-select>
          </div>
        </template>
        <div ref="throughputChartRef" class="chart throughput-chart"></div>
      </el-card>
      <el-card shadow="never">
        <template #header>当前拥挤度分布</template>
        <div ref="crowdChartRef" class="chart"></div>
      </el-card>
    </div>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-title-row">
          <span>近 {{ workloadDays }} 天操作员工作量</span>
          <el-select v-model="workloadDays" size="small" @change="loadWorkload">
            <el-option label="近 7 天" :value="7" />
            <el-option label="近 30 天" :value="30" />
            <el-option label="近 90 天" :value="90" />
          </el-select>
        </div>
      </template>
      <el-table :data="operatorRows" stripe>
        <el-table-column prop="realName" label="操作员" />
        <el-table-column prop="stationName" label="站点" />
        <el-table-column prop="inboundCount" label="上架数量" width="120" />
        <el-table-column prop="verifiedCount" label="核销数量" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  getCrowdDistribution,
  getOperatorWorkload,
  getStationThroughput
} from '@/api/admin'

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const loading = ref(false)
const throughput = ref([])
const crowdDistribution = ref([])
const operatorRows = ref([])
const throughputChartRef = ref()
const crowdChartRef = ref()
const throughputDays = ref(7)
const workloadDays = ref(30)
let throughputChart
let crowdChart

const summaryCards = computed(() => {
  const inbound = throughput.value.reduce((sum, item) => sum + Number(item.inboundCount || 0), 0)
  const taken = throughput.value.reduce((sum, item) => sum + Number(item.takenCount || 0), 0)
  const overdue = throughput.value.reduce((sum, item) => sum + Number(item.overdueCount || 0), 0)
  return [
    { label: `近 ${throughputDays.value} 天上架`, value: inbound },
    { label: `近 ${throughputDays.value} 天取件`, value: taken },
    { label: '当前超时未取', value: overdue }
  ]
})

async function loadAll() {
  loading.value = true
  try {
    const [throughputResponse, crowdResponse, operatorResponse] = await Promise.all([
      getStationThroughput(throughputDays.value),
      getCrowdDistribution(),
      getOperatorWorkload(workloadDays.value)
    ])
    throughput.value = throughputResponse.data || []
    crowdDistribution.value = crowdResponse.data || []
    operatorRows.value = operatorResponse.data || []
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

async function loadThroughput() {
  loading.value = true
  try {
    const response = await getStationThroughput(throughputDays.value)
    throughput.value = response.data || []
    await nextTick()
    renderThroughputChart()
  } finally {
    loading.value = false
  }
}

async function loadWorkload() {
  loading.value = true
  try {
    const response = await getOperatorWorkload(workloadDays.value)
    operatorRows.value = response.data || []
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  throughputChart ||= echarts.init(throughputChartRef.value)
  crowdChart ||= echarts.init(crowdChartRef.value)
  renderThroughputChart()

  crowdChart.setOption({
    tooltip: { trigger: 'item' },
    color: ['#67c23a', '#e6a23c', '#f56c6c'],
    series: [{
      type: 'pie',
      radius: ['42%', '70%'],
      data: crowdDistribution.value.map(item => ({
        name: item.crowdLevelName,
        value: item.stationCount
      }))
    }]
  }, true)
}

function renderThroughputChart() {
  if (!throughputChart) return
  throughputChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['上架', '已取'],
      left: 'center',
      bottom: 10,
      itemGap: 20
    },
    grid: { left: 52, right: 24, top: 68, bottom: 112 },
    xAxis: {
      type: 'category',
      data: throughput.value.map(item => shortName(item.stationName)),
      axisLabel: {
        interval: 0,
        rotate: 20,
        margin: 14
      }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '上架',
        type: 'bar',
        data: throughput.value.map(item => item.inboundCount),
        itemStyle: { color: '#409eff' }
      },
      {
        name: '已取',
        type: 'bar',
        data: throughput.value.map(item => item.takenCount),
        itemStyle: { color: '#67c23a' }
      }
    ]
  }, true)
}

function shortName(value) {
  if (!value) return ''
  return value.length > 10 ? `${value.slice(0, 8)}…` : value
}

function resizeCharts() {
  throughputChart?.resize()
  crowdChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  throughputChart?.dispose()
  crowdChart?.dispose()
})
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

h2 {
  margin: 0 0 6px;
}

p {
  margin: 0;
  color: #909399;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.stat-label {
  color: #909399;
}

.stat-value {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 700;
}

.chart-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 1fr);
  gap: 18px;
}

.chart {
  height: 330px;
}

.throughput-chart {
  height: 400px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title-row > span {
  white-space: nowrap;
}

.card-title-row :deep(.el-select) {
  width: 120px;
}

.table-card {
  margin-top: 18px;
}

@media (max-width: 900px) {
  .stat-grid,
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
