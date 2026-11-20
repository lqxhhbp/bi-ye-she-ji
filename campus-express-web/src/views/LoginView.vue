<template>
  <div class="login-page">
    <el-card class="login-card" shadow="always">
      <template #header>
        <div>
          <div class="login-badge">学生移动端</div>
          <div class="login-title">校园快递平台登录</div>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" class="login-button" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <div class="register-entry">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </div>
      <div class="demo-accounts">
        <div class="demo-title">演示账号，密码均为 123456</div>
        <div>超管：13800000000</div>
        <div>顺丰站长：13800001001</div>
        <div class="student-demo">
          <span>学生：13800002001</span>
          <el-button link type="primary" @click="fillStudentDemo">一键填入</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  phone: typeof route.query.phone === 'string' ? route.query.phone : '',
  password: ''
})
const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const response = await login(form)
    userStore.setAuth(response.data.token, response.data.user)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
    router.replace(redirect || homeForRole(response.data.user.role))
  } finally {
    loading.value = false
  }
}

function homeForRole(role) {
  if (Number(role) === 1) return '/station'
  if (Number(role) === 2) return '/admin/dashboard'
  return '/student/query'
}

function fillStudentDemo() {
  form.phone = '13800002001'
  form.password = '123456'
}
</script>

<style scoped>
.login-page {
  display: grid;
  min-height: calc(100vh - 61px);
  place-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 20% 20%, rgb(64 158 255 / 18%), transparent 34%),
    linear-gradient(145deg, #f5f7fa, #eaf2ff);
  box-sizing: border-box;
}

.login-card {
  width: min(420px, 100%);
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.login-badge {
  margin-bottom: 5px;
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.login-button {
  width: 100%;
  height: 44px;
  border-radius: 9px;
}

.login-card :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 9px;
}

.register-entry {
  margin-top: 14px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.register-entry a {
  color: #409eff;
  text-decoration: none;
}

.demo-accounts {
  margin-top: 22px;
  padding: 12px 14px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
  background: #f5f7fa;
  border-radius: 8px;
}

.demo-title {
  color: #303133;
  font-weight: 600;
}

.student-demo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 420px) {
  .login-page {
    align-items: end;
    padding: 16px 14px 28px;
  }

  .login-card {
    border-radius: 18px;
  }
}
</style>
