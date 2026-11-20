<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">审计追踪</div>
        <h1>操作日志</h1>
        <p>查看关键写操作，日志不包含密码、Token 和完整手机号。</p>
      </div>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </header>

    <el-card shadow="never">
      <div class="filters">
        <el-select v-model="filters.module" clearable placeholder="业务模块" @change="handleFilterChange">
          <el-option v-for="module in modules" :key="module" :label="module" :value="module" />
        </el-select>
        <el-input
          v-model="filters.action"
          clearable
          placeholder="操作名称，例如：上架包裹"
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
        <el-button type="primary" @click="handleFilterChange">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无操作日志">
        <el-table-column prop="userId" label="用户 ID" width="100" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="action" label="操作" width="150" />
        <el-table-column prop="detail" label="请求内容" min-width="320" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP 地址" width="150" />
        <el-table-column label="操作时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt, true) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ total }} 条</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
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
import { getOperationLogs } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const modules = ['站点', '包裹', '空闲度', '审核', '告警']
const rows = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filters = reactive({
  module: '',
  action: ''
})

async function loadData() {
  loading.value = true
  try {
    const response = await getOperationLogs({
      page: page.value,
      pageSize: pageSize.value,
      module: filters.module || undefined,
      action: filters.action.trim() || undefined
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
  filters.module = ''
  filters.action = ''
  handleFilterChange()
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

.filters :deep(.el-input) {
  width: min(300px, 100%);
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

  .filters :deep(.el-select),
  .filters :deep(.el-input) {
    width: 100%;
    grid-column: 1 / -1;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
    overflow-x: auto;
  }
}
</style>
