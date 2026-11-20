<template>
  <div class="placeholder-page">
    <el-page-header :content="title" @back="$router.back()" />
    <el-result
      icon="info"
      title="页面骨架已就绪"
      :sub-title="`${title}将在对应业务阶段实现`"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => route.meta.title || '功能页面')
</script>

<style scoped>
.placeholder-page {
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
  background: #f5f7fa;
}
</style>
