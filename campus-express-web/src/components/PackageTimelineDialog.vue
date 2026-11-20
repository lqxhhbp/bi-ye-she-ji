<template>
  <el-dialog
    :model-value="modelValue"
    title="包裹时间线"
    width="min(92vw, 520px)"
    destroy-on-close
    @close="$emit('update:modelValue', false)"
  >
    <div v-loading="loading" class="timeline-body">
      <el-timeline v-if="items.length">
        <el-timeline-item
          v-for="item in items"
          :key="`${item.action}-${item.createdAt}`"
          :timestamp="formatDateTime(item.createdAt, true)"
          :type="timelineType(item.action)"
        >
          {{ item.actionName }}
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else-if="!loading" description="暂无操作记录" />
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getPackageTimeline } from '@/api/packages'
import { formatDateTime } from '@/utils/format'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  packageId: {
    type: Number,
    default: null
  }
})

defineEmits(['update:modelValue'])

const loading = ref(false)
const items = ref([])

async function loadTimeline() {
  if (!props.packageId) {
    items.value = []
    return
  }
  loading.value = true
  try {
    const response = await getPackageTimeline(props.packageId)
    items.value = response.data || []
  } finally {
    loading.value = false
  }
}

function timelineType(action) {
  if (Number(action) === 2) return 'success'
  if (Number(action) === 1) return 'info'
  return 'primary'
}

watch(
  () => props.modelValue,
  visible => {
    if (visible) loadTimeline()
  }
)
</script>

<style scoped>
.timeline-body {
  min-height: 180px;
}
</style>
