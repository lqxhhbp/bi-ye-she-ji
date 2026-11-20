<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">包裹入库</div>
        <h1>单个包裹上架</h1>
        <p>录入取件码和货架号后，学生即可通过手机号或取件码查询。</p>
      </div>
      <el-button @click="$router.push('/station/packages')">返回包裹列表</el-button>
    </header>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">包裹信息</div>
        </template>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="取件码" prop="pickupCode">
            <el-input
              v-model="form.pickupCode"
              maxlength="8"
              clearable
              placeholder="6—8 位数字或字母"
            />
          </el-form-item>
          <el-form-item label="收件人手机号" prop="receiverPhone">
            <el-input
              v-model="form.receiverPhone"
              maxlength="11"
              inputmode="numeric"
              clearable
              placeholder="请输入 11 位手机号"
            />
          </el-form-item>
          <el-form-item label="快递公司" prop="companyCode">
            <el-select v-model="form.companyCode" placeholder="请选择本站代理公司" class="full-width">
              <el-option
                v-for="company in station?.companies || []"
                :key="company.companyCode"
                :label="company.companyName"
                :value="company.companyCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="货架号" prop="shelfNo">
            <el-input v-model="form.shelfNo" maxlength="32" placeholder="例如：A-01" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
              placeholder="选填，例如：大件、易碎"
            />
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" :loading="submitting" @click="submit">
              确认上架
            </el-button>
            <el-button @click="resetForm">清空</el-button>
          </div>
        </el-form>
      </el-card>

      <div class="side-column">
        <el-card shadow="never">
          <template #header>当前站点</template>
          <div v-loading="loadingStation" class="station-summary">
            <strong>{{ station?.name || '站点信息未加载' }}</strong>
            <span>{{ station?.address }}</span>
            <span>营业时间：{{ station?.businessHours || '以现场为准' }}</span>
            <div class="company-tags">
              <el-tag
                v-for="company in station?.companies || []"
                :key="company.companyCode"
                effect="plain"
              >
                {{ company.companyName }}
              </el-tag>
            </div>
          </div>
        </el-card>

        <el-card v-if="lastCreated" class="success-card" shadow="never">
          <template #header>最近上架成功</template>
          <div class="success-package">
            <span>取件码</span>
            <strong>{{ lastCreated.pickupCode }}</strong>
            <small>{{ lastCreated.stationName }} · {{ lastCreated.shelfNo }}</small>
          </div>
        </el-card>

        <el-alert
          type="info"
          :closable="false"
          title="同一取件码全局唯一；包裹上架 30 分钟后不能撤销。"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createPackage } from '@/api/packages'
import { getStation } from '@/api/stations'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const station = ref(null)
const loadingStation = ref(false)
const submitting = ref(false)
const lastCreated = ref(null)
const stationId = computed(() => userStore.user?.stationId)
const form = reactive({
  pickupCode: '',
  receiverPhone: '',
  companyCode: '',
  shelfNo: '',
  remark: ''
})
const rules = {
  pickupCode: [
    { required: true, message: '请输入取件码', trigger: 'blur' },
    { pattern: /^[0-9A-Za-z]{6,8}$/, message: '取件码必须为 6—8 位数字或字母', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入收件人手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  companyCode: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  shelfNo: [{ required: true, message: '请输入货架号', trigger: 'blur' }]
}

async function loadStation() {
  if (!stationId.value) {
    ElMessage.error('当前账号未绑定站点')
    return
  }
  loadingStation.value = true
  try {
    const response = await getStation(stationId.value)
    station.value = response.data
    if (!form.companyCode) {
      form.companyCode = station.value?.companies?.[0]?.companyCode || ''
    }
  } finally {
    loadingStation.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    const response = await createPackage({
      pickupCode: form.pickupCode.trim().toUpperCase(),
      receiverPhone: form.receiverPhone.trim(),
      companyCode: form.companyCode,
      shelfNo: form.shelfNo.trim(),
      remark: form.remark.trim() || null
    })
    lastCreated.value = response.data
    ElMessage.success('包裹上架成功')
    form.pickupCode = ''
    form.receiverPhone = ''
    form.shelfNo = ''
    form.remark = ''
    formRef.value.clearValidate()
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.pickupCode = ''
  form.receiverPhone = ''
  form.shelfNo = ''
  form.remark = ''
  formRef.value?.clearValidate()
}

onMounted(loadStation)
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

.content-grid {
  display: grid;
  grid-template-columns: minmax(360px, 680px) minmax(280px, 1fr);
  gap: 18px;
}

.card-title {
  color: #303133;
  font-weight: 700;
}

.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  gap: 10px;
}

.form-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 16px;
}

.station-summary {
  display: grid;
  gap: 8px;
}

.station-summary strong {
  color: #303133;
  font-size: 16px;
  line-height: 1.55;
}

.station-summary span {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.company-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 4px;
}

.success-card {
  border-color: #b3e19d;
}

.success-package {
  display: grid;
  gap: 5px;
}

.success-package span,
.success-package small {
  color: #909399;
  font-size: 12px;
}

.success-package strong {
  color: #529b2e;
  font-size: 28px;
  letter-spacing: 2px;
}

@media (max-width: 900px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .page {
    padding: 18px 14px;
  }

  .page-heading {
    display: block;
  }

  .page-heading :deep(.el-button) {
    margin-top: 12px;
  }
}
</style>
