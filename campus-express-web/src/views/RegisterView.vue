<template>
  <div class="register-page">
    <div class="register-card">
      <div class="register-heading">
        <span class="eyebrow">学生账号</span>
        <h1>注册校园快递平台</h1>
        <p>注册后即可查询包裹、查看地图和上报站点拥挤度。</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submit"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input
            v-model="form.phone"
            maxlength="11"
            inputmode="numeric"
            placeholder="请输入 11 位手机号"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" maxlength="20" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入 6—32 位密码"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="submit-button" @click="submit">
          注册账号
        </el-button>
      </el-form>

      <div class="login-link">
        已有账号？
        <router-link to="/login">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({
  phone: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 20, message: '姓名不能超过 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度必须为 6—32 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      phone: form.phone,
      realName: form.realName.trim(),
      password: form.password
    })
    ElMessage.success('注册成功，请登录')
    router.replace({ path: '/login', query: { phone: form.phone } })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: grid;
  min-height: calc(100vh - 60px);
  padding: 24px 18px;
  background:
    radial-gradient(circle at 15% 10%, rgb(64 158 255 / 18%), transparent 32%),
    linear-gradient(150deg, #f5f7fa, #eef6ff);
  box-sizing: border-box;
  place-items: center;
}

.register-card {
  width: min(430px, 100%);
  padding: 26px 22px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 18px;
  box-shadow: 0 18px 42px rgb(48 49 51 / 10%);
  box-sizing: border-box;
}

.eyebrow {
  color: #409eff;
  font-size: 12px;
  font-weight: 700;
}

.register-heading h1 {
  margin: 7px 0 0;
  color: #303133;
  font-size: 25px;
  line-height: 1.3;
}

.register-heading p {
  margin: 9px 0 22px;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.register-card :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 9px;
}

.submit-button {
  width: 100%;
  height: 44px;
  border-radius: 9px;
}

.login-link {
  margin-top: 18px;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.login-link a {
  color: #409eff;
  text-decoration: none;
}

@media (max-width: 420px) {
  .register-page {
    padding: 18px 14px;
  }
}
</style>
