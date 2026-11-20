<template>
  <div class="home-page">
    <section class="hero">
      <div class="hero-copy">
        <span class="hero-eyebrow">校园快递 · 少走弯路</span>
        <h1>找到快递点，<br />也避开排队。</h1>
        <p>一个入口完成查件、地图导航和站点拥挤度查看，让取快递更从容。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="startQuery">开始查件</el-button>
          <el-button size="large" @click="$router.push('/student/map')">查看站点地图</el-button>
        </div>
      </div>
      <div class="hero-panel">
        <span>校园快递站点</span>
        <strong>{{ stations.length }}</strong>
        <small>个校园及周边快递点已接入</small>
      </div>
    </section>

    <section class="feature-grid">
      <article>
        <span class="feature-number">01</span>
        <h2>快速查件</h2>
        <p>手机号或取件码查询待取包裹和货架位置。</p>
      </article>
      <article>
        <span class="feature-number">02</span>
        <h2>站点地图</h2>
        <p>查看五个真实快递点，用颜色判断当前拥挤度。</p>
      </article>
      <article>
        <span class="feature-number">03</span>
        <h2>实时反馈</h2>
        <p>到站后上报现场情况，帮助下一位同学错峰取件。</p>
      </article>
    </section>

    <section class="stations-section">
      <div class="section-heading">
        <div>
          <span>站点概览</span>
          <h2>校园及周边快递点</h2>
        </div>
        <el-button v-if="!userStore.token" link type="primary" @click="$router.push('/login')">
          登录查看
        </el-button>
      </div>

      <div v-if="stations.length" class="station-grid">
        <article v-for="station in stations" :key="station.id" class="station-card">
          <div class="station-top">
            <el-tag :type="crowdLevelMeta(station.crowdLevel).type" effect="light">
              {{ crowdLevelMeta(station.crowdLevel).label }}
            </el-tag>
            <span>{{ Number(station.crowdScore || 0).toFixed(1) }}</span>
          </div>
          <h3>{{ station.name }}</h3>
          <p>{{ station.address }}</p>
          <div class="station-meta">
            <span>{{ station.businessHours || '营业时间以现场为准' }}</span>
          </div>
        </article>
      </div>
      <el-empty v-else description="站点信息暂未加载" />
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMapStations } from '@/api/map'
import { useUserStore } from '@/stores/user'
import { crowdLevelMeta } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()
const stations = ref([])

function startQuery() {
  if (!userStore.token) {
    router.push({ path: '/login', query: { redirect: '/student/query' } })
    return
  }
  router.push(userStore.role === 0 ? '/student/query' : '/')
}

async function loadStations() {
  try {
    const response = await getMapStations()
    stations.value = response.data || []
  } catch {
    stations.value = []
  }
}

onMounted(loadStations)
</script>

<style scoped>
.home-page {
  width: min(1120px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 30px 22px 42px;
  box-sizing: border-box;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(220px, 0.7fr);
  gap: 20px;
  padding: clamp(26px, 6vw, 56px);
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 85% 20%, rgb(255 255 255 / 20%), transparent 26%),
    linear-gradient(135deg, #1f5fa8, #409eff 58%, #79bbff);
  border-radius: 24px;
  box-shadow: 0 20px 44px rgb(64 158 255 / 22%);
}

.hero-eyebrow {
  display: inline-block;
  margin-bottom: 14px;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 700;
  background: rgb(255 255 255 / 18%);
  border-radius: 999px;
}

.hero h1 {
  margin: 0;
  font-size: clamp(36px, 6vw, 64px);
  line-height: 1.1;
  letter-spacing: -2px;
}

.hero-copy p {
  max-width: 600px;
  margin: 20px 0 0;
  color: rgb(255 255 255 / 84%);
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 26px;
}

.hero-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.hero-panel {
  align-self: end;
  display: grid;
  gap: 8px;
  padding: 22px;
  background: rgb(255 255 255 / 14%);
  border: 1px solid rgb(255 255 255 / 24%);
  border-radius: 18px;
  backdrop-filter: blur(10px);
}

.hero-panel span,
.hero-panel small {
  color: rgb(255 255 255 / 78%);
  font-size: 12px;
}

.hero-panel strong {
  font-size: 54px;
  line-height: 1;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.feature-grid article {
  padding: 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 16px;
}

.feature-number {
  color: #a0cfff;
  font-size: 12px;
  font-weight: 800;
}

.feature-grid h2 {
  margin: 10px 0 0;
  color: #303133;
  font-size: 18px;
}

.feature-grid p {
  margin: 8px 0 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.7;
}

.stations-section {
  margin-top: 34px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.section-heading span {
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.section-heading h2 {
  margin: 5px 0 0;
  color: #303133;
  font-size: 24px;
}

.station-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.station-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 15px;
}

.station-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.station-top > span {
  color: #909399;
  font-size: 12px;
}

.station-card h3 {
  margin: 14px 0 0;
  color: #303133;
  font-size: 16px;
  line-height: 1.5;
}

.station-card p {
  min-height: 42px;
  margin: 7px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.7;
}

.station-meta {
  margin-top: 14px;
  color: #606266;
  font-size: 12px;
}

@media (max-width: 760px) {
  .home-page {
    padding: 16px 14px 28px;
  }

  .hero {
    grid-template-columns: 1fr;
    padding: 28px 22px;
    border-radius: 20px;
  }

  .hero h1 {
    font-size: 40px;
    letter-spacing: -1px;
  }

  .hero-copy p {
    margin-top: 16px;
  }

  .hero-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
  }

  .hero-panel {
    grid-template-columns: 1fr auto;
    align-items: center;
    padding: 16px;
  }

  .hero-panel span,
  .hero-panel small {
    grid-column: 1;
  }

  .hero-panel strong {
    grid-row: 1 / span 2;
    grid-column: 2;
    font-size: 42px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .feature-grid article {
    display: grid;
    grid-template-columns: auto 1fr;
    column-gap: 12px;
  }

  .feature-number {
    grid-row: 1 / span 2;
    padding-top: 4px;
  }

  .feature-grid h2 {
    margin-top: 0;
  }

  .station-grid {
    grid-template-columns: 1fr;
  }
}
</style>
