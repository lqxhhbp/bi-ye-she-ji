<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">账号权限</div>
        <h1>站长入驻审核</h1>
        <p>审核通过后账号自动转为站长并绑定申请站点。</p>
      </div>
      <el-radio-group v-model="status" @change="handleFilterChange">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button :value="0">待审核</el-radio-button>
        <el-radio-button :value="1">已通过</el-radio-button>
        <el-radio-button :value="2">已驳回</el-radio-button>
      </el-radio-group>
    </header>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe empty-text="暂无审核记录">
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="applicantPhone" label="手机号" width="140" />
        <el-table-column prop="stationName" label="申请站点" min-width="220" />
        <el-table-column prop="applyReason" label="申请理由" min-width="260" show-overflow-tooltip />
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt, true) }}</template>
        </el-table-column>
        <el-table-column label="状态/操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="Number(row.status) === 0">
              <el-button link type="success" @click="review(row, 1)">通过</el-button>
              <el-button link type="danger" @click="review(row, 2)">驳回</el-button>
            </template>
            <el-tag v-else :type="Number(row.status) === 1 ? 'success' : 'danger'">
              {{ Number(row.status) === 1 ? '已通过' : '已驳回' }}
            </el-tag>
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
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAudits, reviewAudit } from '@/api/admin'
import { formatDateTime } from '@/utils/format'

const status = ref(0)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    const response = await getAudits({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value
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

async function review(row, nextStatus) {
  const actionName = nextStatus === 1 ? '通过' : '驳回'
  try {
    await ElMessageBox.confirm(
      `确认${actionName}“${row.applicantName}”的入驻申请？`,
      '审核确认',
      { type: nextStatus === 1 ? 'success' : 'warning' }
    )
  } catch {
    return
  }
  await reviewAudit(row.id, nextStatus)
  ElMessage.success(`已${actionName}`)
  await loadData()
}

onMounted(loadData)
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-heading {
  display: flex;
  align-items: center;
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
    align-items: flex-start;
    flex-direction: column;
  }

  .page-heading :deep(.el-radio-group) {
    overflow-x: auto;
  }

  .pagination-row {
    align-items: flex-start;
    flex-direction: column;
    overflow-x: auto;
  }
}
</style>
