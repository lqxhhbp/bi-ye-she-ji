<template>
  <div class="student-page">
    <header class="mobile-heading">
      <div>
        <div class="eyebrow">学生查件</div>
        <h1>包裹到了吗？</h1>
        <p>输入手机号或取件码，快速找到待取包裹。</p>
      </div>
      <router-link class="records-link" to="/student/records">我的包裹</router-link>
    </header>

    <section class="search-panel">
      <el-radio-group v-model="mode" class="mode-switch">
        <el-radio-button value="phone">按手机号</el-radio-button>
        <el-radio-button value="code">按取件码</el-radio-button>
      </el-radio-group>

      <el-form class="search-form" @submit.prevent="search">
        <el-form-item v-if="mode === 'phone'">
          <el-input
            v-model="keyword"
            maxlength="11"
            inputmode="numeric"
            clearable
            placeholder="请输入收件手机号"
            @keyup.enter="search"
          >
            <template #prefix>手机</template>
          </el-input>
        </el-form-item>
        <el-form-item v-else>
          <el-input
            v-model="keyword"
            maxlength="8"
            clearable
            placeholder="请输入 6—8 位取件码"
            @keyup.enter="search"
          >
            <template #prefix>取件</template>
          </el-input>
        </el-form-item>
        <el-button type="primary" :loading="loading" class="search-button" @click="search">
          查询包裹
        </el-button>
      </el-form>
      <p class="query-tip">手机号查询只显示当前待取包裹，历史记录请到“我的包裹”。</p>
    </section>

    <section v-if="searched" class="result-section">
      <div class="result-heading">
        <h2>查询结果</h2>
        <span>{{ packages.length }} 个包裹</span>
      </div>

      <div v-if="packages.length" class="package-list">
        <PackageCard
          v-for="item in packages"
          :key="item.id"
          :package-info="item"
          :crowd="crowdByStation[item.stationId]"
          :show-timeline="item.receiverPhone === userStore.user?.phone"
          @timeline="openTimeline"
          @map="goToMap"
          @report="goToReport"
        />
      </div>
      <el-empty v-else description="没有找到待取包裹" />
    </section>

    <section v-else class="guide-card">
      <div class="guide-icon">查</div>
      <div>
        <strong>两种查询方式</strong>
        <p>手机号可以查看该号码名下所有待取包裹；取件码适合到站后快速确认货架。</p>
      </div>
    </section>

    <PackageTimelineDialog
      v-model="timelineVisible"
      :package-id="timelinePackageId"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PackageCard from '@/components/PackageCard.vue'
import PackageTimelineDialog from '@/components/PackageTimelineDialog.vue'
import { getCrowdSummary } from '@/api/idle'
import { queryPackageByPickupCode, queryPackagesByPhone } from '@/api/packages'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const mode = ref('phone')
const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const packages = ref([])
const crowdByStation = ref({})
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

async function search() {
  const value = keyword.value.trim()
  if (mode.value === 'phone' && !/^1\d{10}$/.test(value)) {
    ElMessage.warning('请输入正确的 11 位手机号')
    return
  }
  if (mode.value === 'code' && !/^[0-9A-Za-z]{6,8}$/.test(value)) {
    ElMessage.warning('请输入 6—8 位取件码')
    return
  }

  loading.value = true
  try {
    if (mode.value === 'phone') {
      const response = await queryPackagesByPhone(value)
      packages.value = response.data || []
    } else {
      const response = await queryPackageByPickupCode(value.toUpperCase())
      packages.value = response.data ? [response.data] : []
    }
    searched.value = true
  } finally {
    loading.value = false
  }
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
  await loadCrowdSummary()
  const phone = typeof route.query.phone === 'string' ? route.query.phone : ''
  const pickupCode = typeof route.query.pickupCode === 'string' ? route.query.pickupCode : ''
  if (phone) {
    mode.value = 'phone'
    keyword.value = phone
    await search()
  } else if (pickupCode) {
    mode.value = 'code'
    keyword.value = pickupCode
    await search()
  }
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

.mobile-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin-bottom: 6px;
  color: #409eff;
  font-size: 13px;
  font-weight: 700;
}

h1 {
  margin: 0;
  color: #303133;
  font-size: clamp(26px, 7vw, 36px);
  line-height: 1.2;
}

.mobile-heading p {
  margin: 9px 0 0;
  color: #909399;
  font-size: 14px;
  line-height: 1.6;
}

.records-link {
  flex: none;
  padding: 9px 12px;
  color: #337ecc;
  font-size: 13px;
  text-decoration: none;
  background: #ecf5ff;
  border-radius: 999px;
}

.search-panel {
  margin-top: 20px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgb(48 49 51 / 5%);
}

.mode-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
}

.mode-switch :deep(.el-radio-button) {
  flex: 1;
}

.mode-switch :deep(.el-radio-button__inner) {
  width: 100%;
}

.search-form {
  margin-top: 16px;
}

.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.search-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 10px;
}

.search-button {
  width: 100%;
  height: 46px;
  border-radius: 10px;
}

.query-tip {
  margin: 12px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.result-section {
  margin-top: 22px;
}

.result-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.result-heading h2 {
  margin: 0;
  color: #303133;
  font-size: 18px;
}

.result-heading span {
  color: #909399;
  font-size: 13px;
}

.package-list {
  display: grid;
  gap: 14px;
}

.guide-card {
  display: flex;
  gap: 14px;
  margin-top: 18px;
  padding: 18px;
  background: linear-gradient(135deg, #ecf5ff, #f5f9ff);
  border-radius: 14px;
}

.guide-icon {
  display: grid;
  flex: none;
  width: 42px;
  height: 42px;
  color: #fff;
  font-weight: 800;
  background: #409eff;
  border-radius: 12px;
  place-items: center;
}

.guide-card strong {
  color: #303133;
}

.guide-card p {
  margin: 6px 0 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 420px) {
  .student-page {
    padding: 18px 14px 24px;
  }

  .search-panel {
    padding: 15px;
  }
}
</style>
