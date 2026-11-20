<template>
  <el-config-provider>
    <div :class="shellClasses">
      <header class="app-header">
        <router-link class="brand" :to="brandPath">校园快递点导航</router-link>
        <nav v-if="navLinks.length" class="header-navigation">
          <router-link v-for="link in navLinks" :key="link.to" :to="link.to">
            {{ link.label }}
          </router-link>
        </nav>
        <div class="account-area">
          <template v-if="userStore.token">
            <span>{{ userStore.user?.realName || '已登录' }}</span>
            <el-button link type="danger" @click="logout">退出</el-button>
          </template>
          <router-link v-else to="/login">登录</router-link>
        </div>
      </header>

      <div class="app-body">
        <aside v-if="hasSideNavigation" class="side-navigation">
          <div class="side-navigation-title">{{ navigationTitle }}</div>
          <router-link v-for="link in navLinks" :key="link.to" :to="link.to">
            {{ link.label }}
          </router-link>
        </aside>

        <main class="app-main">
          <router-view />
        </main>
      </div>

      <nav v-if="isStudent" class="student-tab-bar" aria-label="学生端导航">
        <router-link v-for="link in navLinks" :key="link.to" :to="link.to">
          {{ link.label }}
        </router-link>
      </nav>
    </div>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const studentLinks = [
  { to: '/student/query', label: '查件' },
  { to: '/student/map', label: '地图' },
  { to: '/student/report', label: '上报' },
  { to: '/student/profile', label: '我的' }
]

const stationLinks = [
  { to: '/station', label: '工作台' },
  { to: '/station/packages/new', label: '包裹录入' },
  { to: '/station/packages/import', label: '批量导入' },
  { to: '/station/packages', label: '包裹列表' },
  { to: '/station/verify', label: '取件核销' },
  { to: '/station/info', label: '站点信息' }
]

const adminLinks = [
  { to: '/admin/dashboard', label: '统计看板' },
  { to: '/admin/stations', label: '站点管理' },
  { to: '/admin/audits', label: '入驻审核' },
  { to: '/admin/alerts', label: '告警中心' },
  { to: '/admin/logs', label: '操作日志' }
]

const isStudent = computed(() => userStore.token && userStore.role === 0)
const hasSideNavigation = computed(() => userStore.token && [1, 2].includes(userStore.role))
const navLinks = computed(() => {
  if (userStore.role === 0) return studentLinks
  if (userStore.role === 1) return stationLinks
  if (userStore.role === 2) return adminLinks
  return []
})
const navigationTitle = computed(() => userStore.role === 1 ? '站点管理端' : '平台管理端')
const brandPath = computed(() => {
  if (userStore.role === 0) return '/student/query'
  if (userStore.role === 1) return '/station'
  if (userStore.role === 2) return '/admin/dashboard'
  return '/'
})
const shellClasses = computed(() => ({
  'app-shell': true,
  'has-side-navigation': hasSideNavigation.value,
  'student-shell': isStudent.value
}))

function logout() {
  userStore.clearAuth()
  router.replace('/login')
}
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  font-family: 'Microsoft YaHei', 'PingFang SC', Arial, sans-serif;
}

.app-shell {
  min-height: 100%;
  background: #f5f7fa;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 60px;
  padding: 0 24px;
  background: rgb(255 255 255 / 96%);
  border-bottom: 1px solid #e4e7ed;
  box-sizing: border-box;
  backdrop-filter: blur(8px);
}

.brand {
  color: #303133;
  font-size: 17px;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.header-navigation {
  display: flex;
  flex: 1;
  gap: 20px;
}

.header-navigation a,
.account-area a {
  color: #606266;
  font-size: 14px;
  text-decoration: none;
  white-space: nowrap;
}

.header-navigation a.router-link-active {
  color: #409eff;
  font-weight: 600;
}

.account-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  color: #606266;
  font-size: 14px;
}

.app-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  min-height: calc(100vh - 60px);
}

.app-main {
  min-width: 0;
}

.side-navigation {
  display: none;
  padding: 22px 14px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
}

.side-navigation-title {
  padding: 0 12px 16px;
  color: #303133;
  font-size: 16px;
  font-weight: 700;
}

.side-navigation a {
  display: block;
  margin-bottom: 6px;
  padding: 11px 12px;
  color: #606266;
  font-size: 14px;
  text-decoration: none;
  border-radius: 8px;
}

.side-navigation a.router-link-active {
  color: #337ecc;
  font-weight: 600;
  background: #ecf5ff;
}

.student-tab-bar {
  display: none;
}

@media (min-width: 960px) {
  .app-shell.has-side-navigation .app-body {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .app-shell.has-side-navigation .header-navigation {
    display: none;
  }

  .app-shell.has-side-navigation .side-navigation {
    display: block;
  }
}

@media (max-width: 760px) {
  .app-header {
    gap: 12px;
    padding: 0 14px;
  }

  .brand {
    font-size: 15px;
  }

  .header-navigation {
    gap: 14px;
    overflow-x: auto;
  }

  .app-shell.student-shell .header-navigation {
    display: none;
  }

  .account-area > span {
    display: none;
  }

  .student-tab-bar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 30;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    padding: 6px 8px max(6px, env(safe-area-inset-bottom));
    background: rgb(255 255 255 / 98%);
    border-top: 1px solid #e4e7ed;
    box-shadow: 0 -4px 16px rgb(0 0 0 / 5%);
  }

  .student-tab-bar a {
    padding: 7px 2px;
    color: #909399;
    font-size: 12px;
    text-align: center;
    text-decoration: none;
  }

  .student-tab-bar a.router-link-active {
    color: #409eff;
    font-weight: 600;
  }

  .app-shell.student-shell .app-main {
    padding-bottom: 62px;
  }
}
</style>
