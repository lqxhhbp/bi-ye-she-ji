<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h2>取件核销</h2>
        <p>输入取件码确认包裹后完成核销，重复核销会被拒绝。</p>
      </div>
    </div>

    <el-card class="verify-card" shadow="never">
      <div class="search-row">
        <el-input
          v-model="pickupCode"
          maxlength="8"
          clearable
          placeholder="请输入 6—8 位取件码"
          @keyup.enter="searchPackage"
        />
        <el-button type="primary" :loading="searching" @click="searchPackage">查询包裹</el-button>
      </div>

      <el-descriptions v-if="currentPackage" :column="2" border class="package-detail">
        <el-descriptions-item label="取件码">{{ currentPackage.pickupCode }}</el-descriptions-item>
        <el-descriptions-item label="收件人">{{ currentPackage.receiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="快递公司">{{ currentPackage.companyName }}</el-descriptions-item>
        <el-descriptions-item label="货架号">{{ currentPackage.shelfNo }}</el-descriptions-item>
        <el-descriptions-item label="上架时间">{{ formatTime(currentPackage.inboundAt) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ currentPackage.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-button
        v-if="currentPackage"
        class="verify-button"
        type="success"
        :loading="verifying"
        @click="confirmVerify"
      >
        确认核销
      </el-button>
    </el-card>

    <el-card v-loading="loading" shadow="never" class="list-card">
      <el-tabs v-model="activeTab" @tab-change="loadActiveTab">
        <el-tab-pane label="已取包裹" name="taken">
          <el-table :data="taken.records" stripe>
            <el-table-column prop="pickupCode" label="取件码" width="130" />
            <el-table-column prop="receiverPhone" label="收件人" width="140" />
            <el-table-column prop="companyName" label="快递公司" />
            <el-table-column prop="shelfNo" label="货架号" width="100" />
            <el-table-column label="取件时间" width="180">
              <template #default="{ row }">{{ formatTime(row.takenAt) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="taken.total > pageSize"
            v-model:current-page="takenPage"
            :page-size="pageSize"
            :total="taken.total"
            layout="prev, pager, next"
            background
            class="pagination"
            @current-change="loadTaken"
          />
        </el-tab-pane>
        <el-tab-pane label="超时未取" name="overdue">
          <el-table :data="overdue.records" stripe>
            <el-table-column prop="pickupCode" label="取件码" width="130" />
            <el-table-column prop="receiverPhone" label="收件人" width="140" />
            <el-table-column prop="companyName" label="快递公司" />
            <el-table-column prop="shelfNo" label="货架号" width="100" />
            <el-table-column label="上架时间" width="180">
              <template #default="{ row }">{{ formatTime(row.inboundAt) }}</template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="overdue.total > pageSize"
            v-model:current-page="overduePage"
            :page-size="pageSize"
            :total="overdue.total"
            layout="prev, pager, next"
            background
            class="pagination"
            @current-change="loadOverdue"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOverduePackages,
  getTakenPackages,
  queryVerifiablePackage,
  verifyPackage
} from '@/api/packages'

const pickupCode = ref('')
const currentPackage = ref(null)
const searching = ref(false)
const verifying = ref(false)
const loading = ref(false)
const activeTab = ref('taken')
const taken = ref({ total: 0, records: [] })
const overdue = ref({ total: 0, records: [] })
const takenPage = ref(1)
const overduePage = ref(1)
const pageSize = 20

async function searchPackage() {
  if (!/^[0-9A-Za-z]{6,8}$/.test(pickupCode.value.trim())) {
    ElMessage.warning('请输入 6—8 位取件码')
    return
  }
  searching.value = true
  try {
    const response = await queryVerifiablePackage(pickupCode.value.trim().toUpperCase())
    currentPackage.value = response.data
  } catch {
    currentPackage.value = null
  } finally {
    searching.value = false
  }
}

async function confirmVerify() {
  try {
    await ElMessageBox.confirm('确认该包裹已经交付给收件人？', '核销确认', { type: 'warning' })
  } catch {
    return
  }
  verifying.value = true
  try {
    await verifyPackage(currentPackage.value.pickupCode)
    ElMessage.success('核销成功')
    currentPackage.value = null
    pickupCode.value = ''
    takenPage.value = 1
    overduePage.value = 1
    await loadLists()
  } finally {
    verifying.value = false
  }
}

async function loadLists() {
  loading.value = true
  try {
    await Promise.all([loadTaken(), loadOverdue()])
  } finally {
    loading.value = false
  }
}

function loadActiveTab() {
  loadLists()
}

async function loadTaken() {
  const response = await getTakenPackages(takenPage.value, pageSize)
  taken.value = response.data || { total: 0, records: [] }
}

async function loadOverdue() {
  const response = await getOverduePackages(overduePage.value, pageSize)
  overdue.value = response.data || { total: 0, records: [] }
}

function formatTime(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : ''
}

onMounted(loadLists)
</script>

<style scoped>
.page {
  padding: 24px;
}

.page-heading {
  margin-bottom: 18px;
}

h2 {
  margin: 0 0 6px;
}

p {
  margin: 0;
  color: #909399;
}

.search-row {
  display: grid;
  grid-template-columns: minmax(240px, 480px) auto;
  gap: 10px;
}

.package-detail {
  margin-top: 20px;
}

.verify-button {
  width: 100%;
  margin-top: 16px;
}

.list-card {
  margin-top: 18px;
}

.pagination {
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
