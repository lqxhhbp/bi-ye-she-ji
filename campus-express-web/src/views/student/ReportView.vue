<template>
  <div class="student-page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">现场反馈</div>
        <h1>上报拥挤度</h1>
        <p>用一次点击告诉同学现场排队情况。</p>
      </div>
    </header>

    <div class="report-layout">
      <el-card class="report-card" shadow="never">
        <template #header>
          <div class="card-title">选择站点与现场情况</div>
        </template>
        <el-form label-position="top">
          <el-form-item label="快递站点">
            <el-select v-model="form.stationId" placeholder="请选择站点" class="full-width">
              <el-option
                v-for="station in stations"
                :key="station.id"
                :label="station.name"
                :value="station.id"
              />
            </el-select>
          </el-form-item>

          <div v-if="selectedStation" class="station-summary">
            <div>
              <span>当前显示</span>
              <strong :style="{ color: selectedCrowdMeta.color }">
                {{ selectedCrowdMeta.label }}
              </strong>
            </div>
            <div>
              <span>拥挤指数</span>
              <strong>{{ Number(selectedCrowd?.crowdScore || 0).toFixed(1) }}</strong>
            </div>
            <div>
              <span>近 10 分钟</span>
              <strong>{{ selectedCrowd?.recentReportCount || 0 }} 次上报</strong>
            </div>
          </div>

          <el-form-item label="现场情况">
            <el-radio-group v-model="form.level" class="level-group">
              <el-radio-button :value="0">
                <span class="level-dot empty"></span>空闲
              </el-radio-button>
              <el-radio-button :value="1">
                <span class="level-dot medium"></span>一般
              </el-radio-button>
              <el-radio-button :value="2">
                <span class="level-dot crowded"></span>拥挤
              </el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-alert
            type="info"
            :closable="false"
            title="同一站点 10 分钟内只能上报一次，每日最多 5 次。"
          />
          <el-button class="submit-button" type="primary" :loading="submitting" @click="submit">
            提交现场情况
          </el-button>
        </el-form>
      </el-card>

      <el-card shadow="never">
        <template #header>
          <div class="card-title">最近上报</div>
        </template>
        <el-timeline v-if="recentReports.length">
          <el-timeline-item
            v-for="item in recentReports"
            :key="item.id"
            :timestamp="formatDateTime(item.createdAt)"
            :color="crowdLevelMeta(item.level).color"
          >
            {{ item.levelName }} · {{ item.source === 1 ? '站长校准' : '学生上报' }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="该站点暂无上报记录" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCrowdSummary, getRecentReports, reportCrowd } from '@/api/idle'
import { getStations } from '@/api/stations'
import { crowdLevelMeta, formatDateTime } from '@/utils/format'

const route = useRoute()
const stations = ref([])
const crowdByStation = ref({})
const recentReports = ref([])
const submitting = ref(false)
const form = reactive({
  stationId: null,
  level: 0
})

const selectedStation = computed(() => (
  stations.value.find(item => item.id === form.stationId) || null
))
const selectedCrowd = computed(() => crowdByStation.value[form.stationId])
const selectedCrowdMeta = computed(() => crowdLevelMeta(selectedCrowd.value?.crowdLevel))

async function loadStations() {
  const [stationResponse, crowdResponse] = await Promise.all([
    getStations({ page: 1, pageSize: 100 }),
    getCrowdSummary()
  ])
  stations.value = stationResponse.data?.records || []
  crowdByStation.value = Object.fromEntries(
    (crowdResponse.data || []).map(item => [item.stationId, item])
  )
  const queryStationId = Number(route.query.stationId)
  form.stationId = stations.value.some(item => item.id === queryStationId)
    ? queryStationId
    : stations.value[0]?.id || null
}

async function loadRecent() {
  if (!form.stationId) {
    recentReports.value = []
    return
  }
  const response = await getRecentReports(form.stationId)
  recentReports.value = response.data || []
}

async function refreshCrowd() {
  const response = await getCrowdSummary()
  crowdByStation.value = Object.fromEntries(
    (response.data || []).map(item => [item.stationId, item])
  )
}

async function submit() {
  if (!form.stationId) {
    ElMessage.warning('请选择站点')
    return
  }
  submitting.value = true
  try {
    await reportCrowd(form)
    ElMessage.success('上报成功，感谢你的反馈')
    await Promise.all([loadRecent(), refreshCrowd()])
  } finally {
    submitting.value = false
  }
}

watch(() => form.stationId, loadRecent)
onMounted(async () => {
  await loadStations()
  await loadRecent()
})
</script>

<style scoped>
.student-page {
  width: min(900px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 20px 18px 28px;
  box-sizing: border-box;
}

.eyebrow {
  margin-bottom: 5px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.page-heading h1 {
  margin: 0;
  color: #303133;
  font-size: 27px;
}

.page-heading p {
  margin: 7px 0 0;
  color: #909399;
  font-size: 13px;
}

.report-layout {
  display: grid;
  grid-template-columns: minmax(340px, 520px) minmax(280px, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.card-title {
  color: #303133;
  font-weight: 700;
}

.full-width {
  width: 100%;
}

.station-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f7f9fc;
  border-radius: 10px;
}

.station-summary div {
  min-width: 0;
}

.station-summary span,
.station-summary strong {
  display: block;
}

.station-summary span {
  margin-bottom: 4px;
  color: #909399;
  font-size: 11px;
}

.station-summary strong {
  overflow: hidden;
  color: #303133;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.level-group {
  display: flex;
  width: 100%;
}

.level-group :deep(.el-radio-button) {
  flex: 1;
}

.level-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.level-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 6px;
  border-radius: 50%;
}

.level-dot.empty {
  background: #67c23a;
}

.level-dot.medium {
  background: #e6a23c;
}

.level-dot.crowded {
  background: #f56c6c;
}

.submit-button {
  width: 100%;
  margin-top: 18px;
}

@media (max-width: 760px) {
  .student-page {
    padding: 16px 14px 24px;
  }

  .report-layout {
    grid-template-columns: 1fr;
  }

  .report-card {
    order: 0;
  }
}

@media (max-width: 420px) {
  .station-summary {
    grid-template-columns: 1fr;
  }
}
</style>
