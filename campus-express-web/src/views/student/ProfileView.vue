<template>
  <div class="student-page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">学生中心</div>
        <h1>我的</h1>
      </div>
      <button class="logout-button" type="button" @click="logout">退出登录</button>
    </header>

    <section class="profile-card">
      <div class="avatar">{{ avatarText }}</div>
      <div class="profile-copy">
        <strong>{{ user?.realName || '学生用户' }}</strong>
        <span>{{ user?.phone || '手机号未加载' }}</span>
      </div>
      <el-tag type="success" effect="light">已登录</el-tag>
    </section>

    <section class="quick-grid">
      <router-link class="quick-card primary" to="/student/records">
        <span class="quick-title">我的包裹</span>
        <small>查看待取与历史记录</small>
      </router-link>
      <router-link class="quick-card" to="/student/map">
        <span class="quick-title">站点地图</span>
        <small>查看位置与拥挤度</small>
      </router-link>
    </section>

    <section class="section-card">
      <div class="section-heading">
        <div>
          <h2>账号安全</h2>
          <p>定期修改密码，不要将密码告知他人。</p>
        </div>
        <el-button type="primary" plain @click="passwordVisible = true">修改密码</el-button>
      </div>
    </section>

    <section class="section-card">
      <div class="section-heading">
        <div>
          <h2>申请成为站长</h2>
          <p>绑定一个校园快递站点，提交后等待超级管理员审核。</p>
        </div>
      </div>
      <el-form label-position="top" class="apply-form" @submit.prevent="submitApply">
        <el-form-item label="申请站点">
          <el-select v-model="applyForm.stationId" placeholder="请选择站点" class="full-width">
            <el-option
              v-for="station in stations"
              :key="station.id"
              :label="station.name"
              :value="station.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input
            v-model="applyForm.applyReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="说明你与该站点的关系或值守安排"
          />
        </el-form-item>
        <el-button type="primary" :loading="applying" class="full-width" @click="submitApply">
          提交入驻申请
        </el-button>
      </el-form>
    </section>

    <el-dialog
      v-model="passwordVisible"
      title="修改密码"
      width="min(92vw, 460px)"
      destroy-on-close
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-position="top"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="submitPassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyStation, changePassword, getCurrentUser } from '@/api/auth'
import { getStations } from '@/api/stations'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const avatarText = computed(() => user.value?.realName?.slice(0, 1) || '学')
const stations = ref([])
const applying = ref(false)
const passwordVisible = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()
const applyForm = reactive({
  stationId: null,
  applyReason: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度必须为 6—32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function loadUser() {
  const response = await getCurrentUser()
  userStore.setUser(response.data)
}

async function loadStations() {
  const response = await getStations({ page: 1, pageSize: 100 })
  stations.value = response.data?.records || []
}

async function submitApply() {
  if (!applyForm.stationId) {
    ElMessage.warning('请选择申请站点')
    return
  }
  if (!applyForm.applyReason.trim()) {
    ElMessage.warning('请填写申请理由')
    return
  }
  applying.value = true
  try {
    await applyStation({
      stationId: applyForm.stationId,
      applyReason: applyForm.applyReason.trim()
    })
    applyForm.stationId = null
    applyForm.applyReason = ''
    ElMessage.success('申请已提交，请等待审核')
  } finally {
    applying.value = false
  }
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  changingPassword.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordVisible.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码修改成功')
  } finally {
    changingPassword.value = false
  }
}

async function logout() {
  await ElMessageBox.confirm('确认退出当前账号？', '退出登录', { type: 'warning' })
  userStore.clearAuth()
  router.replace('/login')
}

onMounted(async () => {
  await Promise.all([loadUser(), loadStations()])
})
</script>

<style scoped>
.student-page {
  width: min(760px, 100%);
  min-height: calc(100vh - 60px);
  margin: 0 auto;
  padding: 22px 18px 28px;
  box-sizing: border-box;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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

.logout-button {
  padding: 8px 0;
  color: #f56c6c;
  font: inherit;
  font-size: 13px;
  background: transparent;
  border: 0;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  padding: 20px;
  color: #fff;
  background: linear-gradient(135deg, #337ecc, #66b1ff);
  border-radius: 16px;
  box-shadow: 0 12px 26px rgb(64 158 255 / 22%);
}

.avatar {
  display: grid;
  flex: none;
  width: 52px;
  height: 52px;
  color: #337ecc;
  font-size: 22px;
  font-weight: 800;
  background: #fff;
  border-radius: 16px;
  place-items: center;
}

.profile-copy {
  display: grid;
  flex: 1;
  gap: 5px;
}

.profile-copy strong {
  font-size: 18px;
}

.profile-copy span {
  color: rgb(255 255 255 / 78%);
  font-size: 13px;
}

.profile-card :deep(.el-tag) {
  color: #337ecc;
  background: #fff;
  border-color: #fff;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.quick-card {
  display: grid;
  gap: 5px;
  padding: 17px;
  color: #303133;
  text-decoration: none;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 14px;
}

.quick-card.primary {
  background: #ecf5ff;
  border-color: #d9ecff;
}

.quick-title {
  font-weight: 700;
}

.quick-card small {
  color: #909399;
  font-size: 12px;
}

.section-card {
  margin-top: 14px;
  padding: 18px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 14px;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.section-heading h2 {
  margin: 0;
  color: #303133;
  font-size: 17px;
}

.section-heading p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.apply-form {
  margin-top: 16px;
}

.full-width {
  width: 100%;
}

@media (max-width: 420px) {
  .student-page {
    padding: 18px 14px 24px;
  }

  .quick-grid {
    grid-template-columns: 1fr;
  }

  .section-heading {
    align-items: flex-start;
  }
}
</style>
