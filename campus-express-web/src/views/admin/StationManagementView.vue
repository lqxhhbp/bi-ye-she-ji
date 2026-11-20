<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">平台资源</div>
        <h1>站点管理</h1>
        <p>维护物理快递站点、地图坐标和代理快递公司。</p>
      </div>
      <el-button type="primary" @click="openCreate">新增站点</el-button>
    </header>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column prop="name" label="站点名称" min-width="240">
          <template #default="{ row }">
            <strong>{{ row.name }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="address" label="地址" min-width="260" show-overflow-tooltip />
        <el-table-column prop="businessHours" label="营业时间" width="140" />
        <el-table-column label="代理公司" min-width="220">
          <template #default="{ row }">
            <div class="company-tags">
              <el-tag
                v-for="company in row.companies"
                :key="company.companyCode"
                size="small"
                effect="plain"
              >
                {{ company.companyName }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="拥挤度" width="110">
          <template #default="{ row }">
            <el-tag :type="crowdLevelMeta(row.crowdLevel).type">
              {{ crowdLevelMeta(row.crowdLevel).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openCompanies(row)">代理公司</el-button>
            <el-button link type="danger" @click="disable(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ total }} 个启用站点</span>
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

    <el-dialog
      v-model="stationDialogVisible"
      :title="editingId ? '编辑站点' : '新增站点'"
      width="min(92vw, 780px)"
      destroy-on-close
    >
      <el-form ref="stationFormRef" :model="stationForm" :rules="stationRules" label-position="top">
        <el-form-item label="站点名称" prop="name">
          <el-input v-model="stationForm.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="stationForm.address" maxlength="255" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="营业时间" prop="businessHours">
            <el-input v-model="stationForm.businessHours" maxlength="100" />
          </el-form-item>
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="stationForm.contactPhone" maxlength="20" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="经度（BD-09）" prop="lng">
            <el-input-number
              v-model="stationForm.lng"
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
              v-model="stationForm.lat"
              :precision="7"
              :step="0.0001"
              :min="-90"
              :max="90"
              controls-position="right"
              class="full-width"
            />
          </el-form-item>
        </div>
        <div class="map-picker">
          <div class="map-picker-heading">
            <div>
              <strong>地图位置与选点</strong>
              <span>可解析地址，也可直接点击地图调整坐标。</span>
            </div>
            <el-button
              type="primary"
              plain
              :loading="geocoding"
              @click="geocodeAddress"
            >
              地址解析坐标
            </el-button>
          </div>
          <CampusMap
            ref="stationMapRef"
            :stations="dialogMapStations"
            :center="dialogMapCenter"
            :zoom="18"
            pick-mode
            @location-pick="handleLocationPick"
          />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="stationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveStation">
          {{ editingId ? '保存修改' : '创建站点' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="companiesDialogVisible"
      :title="`维护代理公司 · ${currentStation?.name || ''}`"
      width="min(92vw, 620px)"
      destroy-on-close
    >
      <p class="dialog-tip">勾选该物理站点实际代理的快递公司。</p>
      <el-checkbox-group v-model="selectedCompanyCodes" class="company-checkboxes">
        <el-checkbox
          v-for="company in allCompanies"
          :key="company.companyCode"
          :value="company.companyCode"
        >
          <span>{{ company.companyName }}</span>
          <small>{{ company.companyCode }}</small>
        </el-checkbox>
      </el-checkbox-group>
      <el-empty v-if="!allCompanies.length" description="暂无可选快递公司" />
      <template #footer>
        <el-button @click="companiesDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingCompanies" @click="saveCompanies">
          保存代理公司
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CampusMap from '@/components/CampusMap.vue'
import {
  createStation,
  disableStation,
  getCompanies,
  getStations,
  updateStation,
  updateStationCompanies
} from '@/api/stations'
import { crowdLevelMeta } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const savingCompanies = ref(false)
const geocoding = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const stationDialogVisible = ref(false)
const companiesDialogVisible = ref(false)
const editingId = ref(null)
const currentStation = ref(null)
const stationFormRef = ref()
const stationMapRef = ref()
const allCompanies = ref([])
const selectedCompanyCodes = ref([])
const stationForm = reactive({
  name: '',
  address: '',
  lng: null,
  lat: null,
  businessHours: '',
  contactPhone: ''
})
const selectedCompanies = computed(() => selectedCompanyCodes.value
  .map(code => allCompanies.value.find(item => item.companyCode === code))
  .filter(Boolean))
const dialogMapCenter = computed(() => {
  if (Number.isFinite(Number(stationForm.lng)) && Number.isFinite(Number(stationForm.lat))) {
    return { lng: Number(stationForm.lng), lat: Number(stationForm.lat) }
  }
  return { lng: 113.3973, lat: 22.5349 }
})
const dialogMapStations = computed(() => {
  if (!Number.isFinite(Number(stationForm.lng)) || !Number.isFinite(Number(stationForm.lat))) {
    return []
  }
  return [{
    id: editingId.value || 'new',
    name: stationForm.name || '新站点',
    lng: Number(stationForm.lng),
    lat: Number(stationForm.lat),
    crowdLevel: 0
  }]
})
const stationRules = {
  name: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  businessHours: [{ required: true, message: '请输入营业时间', trigger: 'blur' }],
  contactPhone: [
    { pattern: /^$|^[0-9+\-]{5,20}$/, message: '联系电话格式不正确', trigger: 'blur' }
  ],
  lng: [{ required: true, message: '请输入经度', trigger: 'change' }],
  lat: [{ required: true, message: '请输入纬度', trigger: 'change' }]
}

async function loadData() {
  loading.value = true
  try {
    const response = await getStations({ page: page.value, pageSize: pageSize.value })
    rows.value = response.data?.records || []
    total.value = response.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadAllCompanies() {
  const response = await getCompanies()
  allCompanies.value = response.data || []
}

function resetStationForm() {
  Object.assign(stationForm, {
    name: '',
    address: '',
    lng: null,
    lat: null,
    businessHours: '',
    contactPhone: ''
  })
  stationFormRef.value?.clearValidate()
}

function openCreate() {
  editingId.value = null
  resetStationForm()
  stationDialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(stationForm, {
    name: row.name,
    address: row.address,
    lng: Number(row.lng),
    lat: Number(row.lat),
    businessHours: row.businessHours || '',
    contactPhone: row.contactPhone || ''
  })
  stationDialogVisible.value = true
}

async function geocodeAddress() {
  if (!stationForm.address.trim()) {
    ElMessage.warning('请先填写详细地址')
    return
  }
  geocoding.value = true
  try {
    const point = await stationMapRef.value.geocodeAddress(stationForm.address)
    stationForm.lng = point.lng
    stationForm.lat = point.lat
    ElMessage.success('地址解析成功，坐标已回填')
  } finally {
    geocoding.value = false
  }
}

function handleLocationPick(location) {
  stationForm.lng = location.lng
  stationForm.lat = location.lat
  if (location.address) {
    stationForm.address = location.address
  }
}

async function saveStation() {
  await stationFormRef.value.validate()
  saving.value = true
  const payload = {
    name: stationForm.name.trim(),
    address: stationForm.address.trim(),
    lng: stationForm.lng,
    lat: stationForm.lat,
    businessHours: stationForm.businessHours.trim(),
    contactPhone: stationForm.contactPhone.trim(),
    status: 1
  }
  try {
    if (editingId.value) {
      await updateStation(editingId.value, payload)
      ElMessage.success('站点信息已更新')
    } else {
      await createStation(payload)
      ElMessage.success('站点已创建，可继续维护代理公司')
    }
    stationDialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function disable(row) {
  try {
    await ElMessageBox.confirm(
      `确认停用“${row.name}”？停用后该站点将从启用列表移除。`,
      '停用站点',
      { type: 'warning' }
    )
  } catch {
    return
  }
  await disableStation(row.id)
  ElMessage.success('站点已停用')
  if (rows.value.length === 1 && page.value > 1) {
    page.value--
  }
  await loadData()
}

async function openCompanies(row) {
  currentStation.value = row
  await loadAllCompanies()
  selectedCompanyCodes.value = (row.companies || []).map(item => item.companyCode)
  companiesDialogVisible.value = true
}

async function saveCompanies() {
  savingCompanies.value = true
  try {
    await updateStationCompanies(currentStation.value.id, selectedCompanies.value)
    ElMessage.success('代理公司已更新')
    companiesDialogVisible.value = false
    await loadData()
  } finally {
    savingCompanies.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  loadData()
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

.company-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.full-width {
  width: 100%;
}

.map-picker {
  margin-top: 4px;
}

.map-picker-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.map-picker-heading div {
  display: grid;
  gap: 4px;
}

.map-picker-heading strong {
  color: #303133;
  font-size: 14px;
}

.map-picker-heading span {
  color: #909399;
  font-size: 12px;
}

.map-picker :deep(.campus-map),
.map-picker :deep(.map-container) {
  min-height: 260px;
  height: 260px;
}

.dialog-tip {
  margin: 0 0 14px;
  color: #909399;
  font-size: 13px;
}

.company-checkboxes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.company-checkboxes :deep(.el-checkbox) {
  height: auto;
  margin-right: 0;
  padding: 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.company-checkboxes small {
  margin-left: 8px;
  color: #909399;
}

@media (max-width: 760px) {
  .page {
    padding: 18px 14px;
  }

  .page-heading {
    align-items: center;
  }

  .form-grid,
  .company-checkboxes {
    grid-template-columns: 1fr;
  }

  .pagination-row {
    flex-direction: column;
    align-items: flex-start;
    overflow-x: auto;
  }
}
</style>
