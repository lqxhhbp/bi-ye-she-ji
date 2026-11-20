<template>
  <div class="page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">批量入库</div>
        <h1>Excel 批量导入</h1>
        <p>先下载模板填写包裹信息，再上传 Excel 完成批量上架。</p>
      </div>
      <el-button @click="$router.push('/station/packages')">返回包裹列表</el-button>
    </header>

    <div class="content-grid">
      <el-card shadow="never">
        <template #header>
          <div class="card-title-row">
            <span>导入文件</span>
            <el-button link type="primary" :loading="downloading" @click="downloadTemplate">
              下载 Excel 模板
            </el-button>
          </div>
        </template>

        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleFileChange"
          :on-remove="handleFileRemove"
          :on-exceed="handleExceed"
        >
          <div class="upload-title">将 Excel 文件拖到这里</div>
          <div class="upload-tip">或点击选择文件，仅支持 .xlsx / .xls</div>
        </el-upload>

        <div class="import-actions">
          <el-button
            type="primary"
            :disabled="!selectedFile"
            :loading="importing"
            @click="startImport"
          >
            开始导入
          </el-button>
          <el-button :disabled="!selectedFile || importing" @click="clearFile">清空文件</el-button>
        </div>

        <el-alert
          v-if="result"
          class="result-alert"
          :type="result.failed ? 'warning' : 'success'"
          :closable="false"
          :title="`共 ${result.total} 行，成功 ${result.success} 行，失败 ${result.failed} 行`"
        />

        <el-table
          v-if="result?.errors?.length"
          :data="result.errors"
          border
          class="error-table"
        >
          <el-table-column prop="rowNumber" label="Excel 行号" width="120" />
          <el-table-column prop="message" label="错误原因" min-width="280" />
        </el-table>
      </el-card>

      <div class="side-column">
        <el-card shadow="never">
          <template #header>模板字段说明</template>
          <div class="field-list">
            <div>
              <strong>取件码</strong>
              <span>6—8 位数字或字母，全平台唯一</span>
            </div>
            <div>
              <strong>收件人手机号</strong>
              <span>11 位中国大陆手机号</span>
            </div>
            <div>
              <strong>快递公司编码</strong>
              <span>必须是本站点代理公司编码</span>
            </div>
            <div>
              <strong>货架号</strong>
              <span>例如 A-01，不能为空</span>
            </div>
            <div>
              <strong>备注</strong>
              <span>选填，最多 255 个字符</span>
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <template #header>本站支持的公司编码</template>
          <div class="company-list">
            <el-tag
              v-for="company in station?.companies || []"
              :key="company.companyCode"
              effect="plain"
            >
              {{ company.companyCode }} · {{ company.companyName }}
            </el-tag>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  downloadImportTemplate,
  importPackages
} from '@/api/packages'
import { getStation } from '@/api/stations'
import { useUserStore } from '@/stores/user'
import { downloadBlob } from '@/utils/download'

const userStore = useUserStore()
const uploadRef = ref()
const selectedFile = ref(null)
const station = ref(null)
const downloading = ref(false)
const importing = ref(false)
const result = ref(null)

function handleFileChange(uploadFile) {
  if (!/\.(xlsx|xls)$/i.test(uploadFile.name)) {
    ElMessage.warning('请选择 Excel 文件')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  selectedFile.value = uploadFile.raw
  result.value = null
}

function handleFileRemove() {
  selectedFile.value = null
}

function handleExceed() {
  ElMessage.warning('一次只能导入一个文件，请先清空当前文件')
}

function clearFile() {
  uploadRef.value?.clearFiles()
  selectedFile.value = null
  result.value = null
}

async function downloadTemplate() {
  downloading.value = true
  try {
    const blob = await downloadImportTemplate()
    downloadBlob(blob, '包裹导入模板.xlsx')
  } finally {
    downloading.value = false
  }
}

async function startImport() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择 Excel 文件')
    return
  }
  importing.value = true
  try {
    const response = await importPackages(selectedFile.value)
    result.value = response.data
    if (response.data?.failed) {
      ElMessage.warning('部分数据未导入，请查看错误明细')
    } else {
      ElMessage.success('全部导入成功')
    }
  } finally {
    importing.value = false
  }
}

async function loadStation() {
  const stationId = userStore.user?.stationId
  if (!stationId) return
  const response = await getStation(stationId)
  station.value = response.data
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
  grid-template-columns: minmax(440px, 1.3fr) minmax(300px, 0.7fr);
  gap: 18px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #303133;
  font-weight: 700;
}

.upload-title {
  color: #303133;
  font-size: 16px;
  font-weight: 700;
}

.upload-tip {
  margin-top: 7px;
  color: #909399;
  font-size: 12px;
}

.import-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.import-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.result-alert {
  margin-top: 16px;
}

.error-table {
  margin-top: 14px;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 16px;
}

.field-list {
  display: grid;
  gap: 13px;
}

.field-list div {
  display: grid;
  gap: 4px;
}

.field-list strong {
  color: #303133;
  font-size: 13px;
}

.field-list span {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.company-list {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
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
