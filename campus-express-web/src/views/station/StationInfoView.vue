<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">站点资料</div>
        <h1>站点信息</h1>
        <p>维护地址、营业时间、联系电话和地图坐标。</p>
      </div>
      <el-button @click="loadStation">重新加载</el-button>
    </header>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title">基础信息</div>
        </template>
        <el-form ref="formRef" v-loading="loading" :model="form" :rules="rules" label-position="top">
          <el-form-item label="站点名称">
            <el-input v-model="form.name" disabled />
          </el-form-item>
          <el-form-item label="地址" prop="address">
            <el-input v-model="form.address" maxlength="255" />
            <el-button
              class="geocode-button"
              link
              type="primary"
              :loading="geocoding"
              @click="geocodeAddress"
            >
              根据地址解析坐标
            </el-button>
          </el-form-item>
          <el-form-item label="营业时间" prop="businessHours">
            <el-input v-model="form.businessHours" maxlength="100" placeholder="例如：08:00-22:30" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" maxlength="20" placeholder="选填" />
          </el-form-item>
          <div class="coordinate-grid">
            <el-form-item label="经度（BD-09）" prop="lng">
              <el-input-number
                v-model="form.lng"
                :precision="7"
                :step="0.0001"
                :min="-180"
                :max="180"
                controls-position="right"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="纬度（BD-09）" prop="lat">
              <el-input-number
                v-model="form.lat"
                :precision="7"
                :step="0.0001"
                :min="-90"
                :max="90"
                controls-position="right"
                class="full-width"
              />
            </el-form-item>
          </div>
          <el-button type="primary" :loading="saving" @click="save">保存修改</el-button>
        </el-form>
      </el-card>

      <div class="side-column">
        <el-card shadow="never">
          <template #header>
            <div class="map-card-title">
              <span>地图位置</span>
              <small>可点击地图调整坐标</small>
            </div>
          </template>
          <CampusMap
            ref="mapRef"
            :stations="previewStations"
            :center="mapCenter"
            :zoom="18"
            pick-mode
            @location-pick="handleLocationPick"
          />
        </el-card>

        <el-card shadow="never">
          <template #header>代理快递公司</template>
          <div class="company-tags">
            <el-tag
              v-for="company in station?.companies || []"
              :key="company.companyCode"
              effect="plain"
            >
              {{ company.companyName }}
            </el-tag>
          </div>
          <p class="company-tip">代理公司由超级管理员维护，站长端只读。</p>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CampusMap from '@/components/CampusMap.vue'
import { getStation, updateStation } from '@/api/stations'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const formRef = ref()
const station = ref(null)
const loading = ref(false)
const saving = ref(false)
const geocoding = ref(false)
const mapRef = ref()
const form = reactive({
  name: '',
  address: '',
  lng: null,
  lat: null,
  businessHours: '',
  contactPhone: ''
})
const previewStations = computed(() => {
  if (!Number.isFinite(Number(form.lng)) || !Number.isFinite(Number(form.lat))) {
    return []
  }
  return [{
    id: station.value?.id || 'current',
    name: station.value?.name || form.name || '当前站点',
    lng: Number(form.lng),
    lat: Number(form.lat),
    crowdLevel: station.value?.crowdLevel || 0
  }]
})
const mapCenter = computed(() => previewStations.value[0]
  ? { lng: previewStations.value[0].lng, lat: previewStations.value[0].lat }
  : { lng: 113.3973, lat: 22.5349 })
const rules = {
  address: [{ required: true, message: '请输入站点地址', trigger: 'blur' }],
  businessHours: [{ required: true, message: '请输入营业时间', trigger: 'blur' }],
  contactPhone: [
    { pattern: /^$|^[0-9+\-]{5,20}$/, message: '联系电话格式不正确', trigger: 'blur' }
  ],
  lng: [{ required: true, message: '请输入经度', trigger: 'change' }],
  lat: [{ required: true, message: '请输入纬度', trigger: 'change' }]
}

async function loadStation() {
  const stationId = userStore.user?.stationId
  if (!stationId) {
    ElMessage.error('当前账号未绑定站点')
    return
  }
  loading.value = true
  try {
    const response = await getStation(stationId)
    station.value = response.data
    Object.assign(form, {
      name: response.data.name,
      address: response.data.address,
      lng: Number(response.data.lng),
      lat: Number(response.data.lat),
      businessHours: response.data.businessHours,
      contactPhone: response.data.contactPhone || ''
    })
  } finally {
    loading.value = false
  }
}

async function geocodeAddress() {
  if (!form.address.trim()) {
    ElMessage.warning('请先填写站点地址')
    return
  }
  geocoding.value = true
  try {
    const point = await mapRef.value.geocodeAddress(form.address)
    form.lng = point.lng
    form.lat = point.lat
    ElMessage.success('地址解析成功，坐标已回填')
  } finally {
    geocoding.value = false
  }
}

function handleLocationPick(location) {
  form.lng = location.lng
  form.lat = location.lat
  if (location.address) {
    form.address = location.address
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateStation(userStore.user.stationId, {
      name: form.name,
      address: form.address.trim(),
      lng: form.lng,
      lat: form.lat,
      businessHours: form.businessHours.trim(),
      contactPhone: form.contactPhone.trim(),
      status: station.value.status
    })
    ElMessage.success('站点信息已保存')
    await loadStation()
  } finally {
    saving.value = false
  }
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
  grid-template-columns: minmax(380px, 680px) minmax(320px, 1fr);
  gap: 18px;
}

.card-title {
  color: #303133;
  font-weight: 700;
}

.full-width {
  width: 100%;
}

.coordinate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.geocode-button {
  margin: 6px 0 0 auto;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 16px;
}

.side-column :deep(.campus-map),
.side-column :deep(.map-container) {
  min-height: 300px;
  height: 300px;
}

.map-card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.map-card-title small {
  color: #909399;
  font-size: 11px;
  font-weight: 400;
}

.company-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.company-tip {
  margin: 12px 0 0;
  color: #909399;
  font-size: 12px;
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

  .coordinate-grid {
    grid-template-columns: 1fr;
  }
}
</style>
