<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">异常监控</div>
        <h1>告警中心</h1>
        <p>集中处理超时未取、异常上报和站点停用告警。</p>
      </div>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </header>

    <el-card shadow="never">
      <div class="filters">
        <el-select v-model="filters.status" clearable placeholder="处理状态" @change="handleFilterChange">
          <el-option label="未处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
        <el-select v-model="filters.type" clearable placeholder="告警类型" @change="handleFilterChange">
          <el-option label="超时未取" :value="0" />
          <el-option label="异常上报" :value="1" />
          <el-option label="站点停用" :value="2" />
        </el-select>
        <el-button @click="resetFilters">重置筛选</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无告警记录">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="alertTagType(row.type)">{{ alertTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="告警内容" min-width="360" show-overflow-tooltip />
        <el-table-column label="站点 ID" width="100">
          <template #default="{ row }">{{ row.stationId || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt, true) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 0 ? 'warning' : 'success'">
              {{ Number(row.status) === 0 ? '未处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理信息" width="190">
          <template #default="{ row }">
            <el-button v-if="Number(row.status) === 0" link type="primary" @click="handle(row)">
              标记已处理
            </el-button>
            <span v-else class="handled-time">{{ formatDateTime(row.handledAt, true) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="sizes, prev, pager, next"
          background
          @current-change="loadData"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAlerts, handleAlert } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const rows = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filters = reactive({
  status: 0,
  type: null
})

async function loadData() {
  loading.value = true
  try {
    const response = await getAlerts({
      page: page.value,
      pageSize: pageSize.value,
      status: filters.status,
      type: filters.type
    })
    rows.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleFilterChange() {
  page.value = 1
  loadData()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.status = null
  filters.type = null
  page.value = 1
  loadData()
}

async function handle(row) {
  try {
    await ElMessageBox.confirm(`确认告警“${row.content}”已经处理？`, '处理告警', {
      type: 'warning'
    })
  } catch {
    return
  }
  await handleAlert(row.id)
  ElMessage.success('告警已处理')
  await loadData()
}

function alertTypeName(value) {
  return ['超时未取', '异常上报', '站点停用'][Number(value)] || '未知'
}

function alertTagType(value) {
  return Number(value) === 0 ? 'danger' : Number(value) === 1 ? 'warning' : 'info'
}

onMounted(loadData)
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

.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.filters :deep(.el-select) {
  width: 150px;
}

.filters :deep(.el-button + .el-button) {
  margin-left: 0;
}

.handled-time {
  color: #909399;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 18px;
}

.pagination-row > span {
  color: #909399;
  font-size: 13px;
}

@media (max-width: 760px) {
  .page {
    padding: 18px 14px;
  }

  .filters {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }

  .filters :deep(.el-select) {
    width: 100%;
  }

  .filters :deep(.el-button) {
    grid-column: 1 / -1;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
    overflow-x: auto;
  }
}
</style>
