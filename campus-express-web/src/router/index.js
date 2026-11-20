import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页', public: true }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册', public: true }
  },
  {
    path: '/student/query',
    name: 'student-query',
    component: () => import('@/views/student/QueryView.vue'),
    meta: { title: '查件结果', roles: [0] }
  },
  {
    path: '/student/map',
    name: 'student-map',
    component: () => import('@/views/student/StudentMapView.vue'),
    meta: { title: '地图导航', roles: [0] }
  },
  {
    path: '/student/report',
    name: 'student-report',
    component: () => import('@/views/student/ReportView.vue'),
    meta: { title: '上报空闲度', roles: [0] }
  },
  {
    path: '/student/records',
    name: 'student-records',
    component: () => import('@/views/student/RecordsView.vue'),
    meta: { title: '我的包裹', roles: [0] }
  },
  {
    path: '/student/profile',
    name: 'student-profile',
    component: () => import('@/views/student/ProfileView.vue'),
    meta: { title: '个人中心', roles: [0] }
  },
  {
    path: '/station',
    name: 'station-dashboard',
    component: () => import('@/views/station/StationWorkbenchView.vue'),
    meta: { title: '站点工作台', roles: [1] }
  },
  {
    path: '/station/packages/new',
    name: 'station-package-create',
    component: () => import('@/views/station/PackageCreateView.vue'),
    meta: { title: '包裹录入', roles: [1] }
  },
  {
    path: '/station/packages/import',
    name: 'station-package-import',
    component: () => import('@/views/station/PackageImportView.vue'),
    meta: { title: '批量导入', roles: [1] }
  },
  {
    path: '/station/packages',
    name: 'station-packages',
    component: () => import('@/views/station/PackageListView.vue'),
    meta: { title: '包裹列表', roles: [1] }
  },
  {
    path: '/station/verify',
    name: 'station-verify',
    component: () => import('@/views/station/VerifyView.vue'),
    meta: { title: '取件核销', roles: [1] }
  },
  {
    path: '/station/info',
    name: 'station-info',
    component: () => import('@/views/station/StationInfoView.vue'),
    meta: { title: '站点信息', roles: [1] }
  },
  {
    path: '/admin/stations',
    name: 'admin-stations',
    component: () => import('@/views/admin/StationManagementView.vue'),
    meta: { title: '站点管理', roles: [2] }
  },
  {
    path: '/admin/audits',
    name: 'admin-audits',
    component: () => import('@/views/admin/AuditsView.vue'),
    meta: { title: '账号审核', roles: [2] }
  },
  {
    path: '/admin/dashboard',
    name: 'admin-dashboard',
    component: () => import('@/views/admin/DashboardView.vue'),
    meta: { title: '统计看板', roles: [2] }
  },
  {
    path: '/admin/alerts',
    name: 'admin-alerts',
    component: () => import('@/views/admin/AlertsView.vue'),
    meta: { title: '告警中心', roles: [2] }
  },
  {
    path: '/admin/logs',
    name: 'admin-logs',
    component: () => import('@/views/admin/LogsView.vue'),
    meta: { title: '操作日志', roles: [2] }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(to => {
  const userStore = useUserStore()

  if (to.meta.public) {
    return true
  }

  if (!userStore.token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  const requiredRoles = to.meta.roles
  if (requiredRoles && !requiredRoles.includes(userStore.role)) {
    return '/'
  }

  return true
})

export default router
