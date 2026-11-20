<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">本站包裹</div>
        <h1>包裹列表</h1>
        <p>按状态、取件码、手机号或货架号筛选，并导出本站数据。</p>
      </div>
      <div class="heading-actions">
        <el-button :loading="exporting" @click="exportAll">导出 Excel</el-button>
        <el-button type="primary" @click="$router.push('/station/packages/new')">
          上架包裹
        </el-button>
      </div>
    </header>

    <el-card shadow="never">
      <div class="filters">
        <el-select v-model="filters.status" clearable placeholder="包裹状态" @change="search">
          <el-option label="待取" :value="0" />
          <el-option label="已取" :value="1" />
          <el-option label="已撤销" :value="2" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="取件码 / 手机号 / 货架号"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" stripe class="package-table">
        <el-table-column prop="pickupCode" label="取件码" min-width="120">
          <template #default="{ row }">
            <strong>{{ row.pickupCode }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="receiverPhone" label="收件手机" width="130" />
        <el-table-column prop="companyName" label="快递公司" min-width="110" />
        <el-table-column prop="shelfNo" label="货架号" width="100" />
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="packageStatusMeta(row.status).type">
              {{ packageStatusMeta(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.inboundAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetails(row)">详情</el-button>
            <el-button
              v-if="canRevoke(row)"
              link
              type="danger"
              @click="revoke(row)"
            >
              撤销
            </el-button>
          </template>
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

    <el-dialog v-model="detailsVisible" title="包裹详情" width="min(92vw, 580px)">
      <el-descriptions v-if="currentPackage" :column="2" border>
        <el-descriptions-item label="取件码">{{ currentPackage.pickupCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="packageStatusMeta(currentPackage.status).type">
            {{ packageStatusMeta(currentPackage.status).label }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收件手机">{{ currentPackage.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ currentPackage.companyName }}</el-descriptions-item>
        <el-descriptions-item label="货架号">{{ currentPackage.shelfNo }}</el-descriptions-item>
        <el-descriptions-item label="上架时间">
          {{ formatDateTime(currentPackage.inboundAt, true) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="currentPackage.takenAt" label="取件时间">
          {{ formatDateTime(currentPackage.takenAt, true) }}
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">
          {{ currentPackage.remark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  exportPackages,
  getPackages,
  revokePackage
} from '@/api/packages'
import { downloadBlob } from '@/utils/download'
import {
  formatDateTime,
  packageStatusMeta
} from '@/utils/format'

const loading = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const filters = reactive({
  status: null,
  keyword: ''
})
const detailsVisible = ref(false)
const currentPackage = ref(null)

async function loadData() {
  loading.value = true
  try {
    const response = await getPackages({
      page: page.value,
      pageSize: pageSize.value,
      status: filters.status,
      keyword: filters.keyword.trim() || undefined
    })
    rows.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  loadData()
}

function resetFilters() {
  filters.status = null
  filters.keyword = ''
  search()
}

function handleSizeChange() {
  page.value = 1
  loadData()
}

function canRevoke(row) {
  if (Number(row.status) !== 0 || !row.inboundAt) return false
  return Date.now() - new Date(row.inboundAt).getTime() <= 30 * 60 * 1000
}

async function revoke(row) {
  try {
    await ElMessageBox.confirm(
      `确认撤销包裹 ${row.pickupCode}？撤销后学生将无法查询到该包裹。`,
      '撤销上架',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await revokePackage(row.id)
  ElMessage.success('包裹已撤销')
  await loadData()
}

function openDetails(row) {
  currentPackage.value = row
  detailsVisible.value = true
}

async function exportAll() {
  exporting.value = true
  try {
    const blob = await exportPackages()
    downloadBlob(blob, `本站包裹-${new Date().toISOString().slice(0, 10)}.xlsx`)
  } finally {
    exporting.value = false
  }
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

.heading-actions,
.filters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.heading-actions :deep(.el-button + .el-button),
.filters :deep(.el-button + .el-button) {
  margin-left: 0;
}

.filters {
  margin-bottom: 16px;
}

.filters :deep(.el-select) {
  width: 150px;
}

.filters :deep(.el-input) {
  width: min(320px, 100%);
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

  .page-heading {
    display: block;
  }

  .heading-actions {
    margin-top: 12px;
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
    flex-direction: column;
    align-items: flex-start;
    overflow-x: auto;
  }
}
</style>
