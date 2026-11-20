<template>
  <div class="student-page">
    <header class="page-heading">
      <button class="back-button" type="button" @click="$router.back()">返回</button>
      <div>
        <div class="eyebrow">个人包裹</div>
        <h1>我的包裹</h1>
      </div>
      <span class="total-badge">{{ total }} 件</span>
    </header>

    <el-tabs v-model="activeStatus" class="status-tabs" @tab-change="handleStatusChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待取" name="0" />
      <el-tab-pane label="已取" name="1" />
      <el-tab-pane label="已撤销" name="2" />
    </el-tabs>

    <div v-loading="loading" class="records-body">
      <div v-if="records.length" class="package-list">
        <PackageCard
          v-for="item in records"
          :key="item.id"
          :package-info="item"
          :crowd="crowdByStation[item.stationId]"
          @timeline="openTimeline"
          @map="goToMap"
          @report="goToReport"
        />
      </div>
      <el-empty v-else-if="!loading" description="这里还没有包裹记录" />

      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        background
        class="pagination"
        @current-change="loadRecords"
      />
    </div>

    <PackageTimelineDialog
      v-model="timelineVisible"
      :package-id="timelinePackageId"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PackageCard from '@/components/PackageCard.vue'
import PackageTimelineDialog from '@/components/PackageTimelineDialog.vue'
import { getCrowdSummary } from '@/api/idle'
import { getMyPackages } from '@/api/packages'

const router = useRouter()
const activeStatus = ref('all')
const loading = ref(false)
const records = ref([])
const crowdByStation = ref({})
const total = ref(0)
const page = ref(1)
const pageSize = 8
const timelineVisible = ref(false)
const timelinePackageId = ref(null)

async function loadCrowdSummary() {
  try {
    const response = await getCrowdSummary()
    crowdByStation.value = Object.fromEntries(
      (response.data || []).map(item => [item.stationId, item])
    )
  } catch {
    crowdByStation.value = {}
  }
}

async function loadRecords() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize }
    if (activeStatus.value !== 'all') {
      params.status = Number(activeStatus.value)
    }
    const response = await getMyPackages(params)
    records.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  page.value = 1
  loadRecords()
}

function openTimeline(packageInfo) {
  timelinePackageId.value = packageInfo.id
  timelineVisible.value = true
}

function goToMap(packageInfo) {
  router.push({ path: '/student/map', query: { stationId: packageInfo.stationId } })
}

function goToReport(packageInfo) {
  router.push({ path: '/student/report', query: { stationId: packageInfo.stationId } })
}

onMounted(async () => {
  await Promise.all([loadCrowdSummary(), loadRecords()])
})
</script>

<style scoped>
.student-page {
  width: min(760px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 22px 18px 28px;
  box-sizing: border-box;
}

.page-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-heading > div {
  flex: 1;
}

.back-button {
  padding: 8px 0;
  color: #606266;
  font: inherit;
  background: transparent;
  border: 0;
}

.eyebrow {
  margin-bottom: 4px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

h1 {
  margin: 0;
  color: #303133;
  font-size: 25px;
}

.total-badge {
  padding: 6px 10px;
  color: #337ecc;
  font-size: 12px;
  background: #ecf5ff;
  border-radius: 999px;
}

.status-tabs {
  margin-top: 14px;
}

.status-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.records-body {
  min-height: 280px;
}

.package-list {
  display: grid;
  gap: 14px;
}

.pagination {
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 420px) {
  .student-page {
    padding: 18px 14px 24px;
  }
}
</style>
