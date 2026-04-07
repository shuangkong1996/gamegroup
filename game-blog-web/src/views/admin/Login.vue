<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="header">
          <span>Game Blog Admin</span>
        </div>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="Username" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="Password" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">Login</el-button>
        </el-form-item>
        <div class="register-link">
           <el-link type="info" @click="toggleRegister" :underline="false">Register new admin</el-link>
        </div>
      </el-form>
    </el-card>

    <!-- Simple Register Dialog -->
    <el-dialog v-model="registerVisible" title="Register Admin" width="30%">
      <el-form :model="regForm" label-width="80px">
        <el-form-item label="Username">
          <el-input v-model="regForm.username" />
        </el-form-item>
        <el-form-item label="Password">
          <el-input v-model="regForm.password" type="password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="registerVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleRegister">Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const registerVisible = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const regForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: 'Please input username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please input password', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!form.username || !form.password) return
  
  loading.value = true
  try {
    const res: any = await request.post('/auth/admin/login', form)
    localStorage.setItem('token', res.token)
    localStorage.setItem('user', JSON.stringify({ username: res.nickname, avatar: res.avatarUrl }))
    ElMessage.success('Login successfully')
    router.push('/admin/articles')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleRegister = () => {
  registerVisible.value = true
}

const handleRegister = async () => {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('Please fill all fields')
    return
  }
  try {
    await request.post('/auth/register', regForm)
    ElMessage.success('Register successfully, please login')
    registerVisible.value = false
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  background: radial-gradient(circle at 0% 0%, rgba(37, 99, 235, 0.45) 0%, transparent 42%),
    linear-gradient(160deg, #0f172a 0%, #1e293b 100%);
}

.login-card {
  width: 400px;
  max-width: 100%;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  box-shadow: var(--gg-shadow-float);
}

.header {
  text-align: center;
  font-size: 20px;
  font-weight: bold;
}

.login-btn {
  width: 100%;
}

.register-link {
  text-align: right;
}

@media (max-width: 767px) {
  .login-card {
    width: 100%;
  }
}
</style>
