<template>
  <div class="home">
    <el-page-header title="返回" content="校园快递点导航与空闲度查询平台" />

    <el-row :gutter="16" class="cards">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>后端连通性</template>
          <div class="health">
            <el-tag v-if="health" type="success">已连接</el-tag>
            <el-tag v-else type="danger">未连接</el-tag>
            <pre v-if="health">{{ health }}</pre>
            <el-button type="primary" size="small" @click="checkHealth">检测后端</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>地图区域（等待接入百度地图 JS API）</template>
          <div id="map" class="map-placeholder">
            <span>地图容器占位：拿到百度地图 AK 后在此初始化 BMapGL</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import request from '@/api/request'

const health = ref(null)

async function checkHealth() {
  try {
    const res = await request.get('/health')
    health.value = res.data
  } catch (e) {
    health.value = null
  }
}
</script>

<style scoped lang="scss">
.home {
  padding: 20px;
}
.cards {
  margin-top: 16px;
}
.health pre {
  white-space: pre-wrap;
  word-break: break-all;
  background: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}
.map-placeholder {
  height: 520px;
  border: 1px dashed #c0c4cc;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  background: #fafafa;
}
</style>
