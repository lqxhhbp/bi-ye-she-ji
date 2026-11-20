<template>
  <article class="package-card">
    <div class="card-header">
      <div>
        <div class="code-label">取件码</div>
        <div class="pickup-code">{{ packageInfo.pickupCode }}</div>
      </div>
      <el-tag :type="statusMeta.type" effect="light">{{ statusMeta.label }}</el-tag>
    </div>

    <div class="company-line">
      <strong>{{ packageInfo.companyName || packageInfo.companyCode }}</strong>
      <span>{{ packageInfo.stationName || '未知站点' }}</span>
    </div>

    <div class="detail-grid">
      <div>
        <span>货架号</span>
        <strong>{{ packageInfo.shelfNo || '未填写' }}</strong>
      </div>
      <div>
        <span>收件手机</span>
        <strong>{{ packageInfo.receiverPhone }}</strong>
      </div>
      <div>
        <span>上架时间</span>
        <strong>{{ formatDateTime(packageInfo.inboundAt) }}</strong>
      </div>
      <div v-if="packageInfo.takenAt">
        <span>取件时间</span>
        <strong>{{ formatDateTime(packageInfo.takenAt) }}</strong>
      </div>
      <div v-if="crowd">
        <span>站点状态</span>
        <strong :style="{ color: crowdMeta.color }">
          {{ crowd.crowdLevelName }} · {{ Number(crowd.crowdScore || 0).toFixed(1) }}
        </strong>
      </div>
    </div>

    <p v-if="packageInfo.remark" class="remark">{{ packageInfo.remark }}</p>

    <div class="card-actions">
      <el-button v-if="showTimeline" @click="$emit('timeline', packageInfo)">时间线</el-button>
      <el-button @click="$emit('map', packageInfo)">查看地图</el-button>
      <el-button
        v-if="Number(packageInfo.status) === 0"
        type="primary"
        plain
        @click="$emit('report', packageInfo)"
      >
        上报拥挤度
      </el-button>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue'
import {
  crowdLevelMeta,
  formatDateTime,
  packageStatusMeta
} from '@/utils/format'

const props = defineProps({
  packageInfo: {
    type: Object,
    required: true
  },
  crowd: {
    type: Object,
    default: null
  },
  showTimeline: {
    type: Boolean,
    default: true
  }
})

defineEmits(['timeline', 'map', 'report'])

const statusMeta = computed(() => packageStatusMeta(props.packageInfo.status))
const crowdMeta = computed(() => crowdLevelMeta(props.crowd?.crowdLevel))
</script>

<style scoped>
.package-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 14px;
  box-shadow: 0 6px 20px rgb(48 49 51 / 4%);
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.code-label {
  margin-bottom: 3px;
  color: #909399;
  font-size: 12px;
}

.pickup-code {
  color: #303133;
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 2px;
  line-height: 1.2;
}

.company-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  color: #606266;
  font-size: 14px;
}

.company-line span {
  min-width: 0;
  overflow: hidden;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
  padding: 14px;
  background: #f7f9fc;
  border-radius: 10px;
}

.detail-grid div {
  min-width: 0;
}

.detail-grid span,
.detail-grid strong {
  display: block;
}

.detail-grid span {
  margin-bottom: 4px;
  color: #909399;
  font-size: 12px;
}

.detail-grid strong {
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark {
  margin: 12px 0 0;
  padding: 10px 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  background: #fdf6ec;
  border-radius: 8px;
  overflow-wrap: anywhere;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.card-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 420px) {
  .pickup-code {
    font-size: 23px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .card-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
