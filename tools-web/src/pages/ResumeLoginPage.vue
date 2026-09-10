<template>
  <main class="flex min-h-screen items-center justify-center bg-canvas px-4">
    <section class="panel page-enter w-full max-w-md p-8">
      <p class="text-sm font-semibold tracking-wide text-brand-600">简历生成</p>
      <h1 class="mt-3 text-3xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">登录后继续</h1>
      <p class="mt-3 text-sm text-muted">登录后可编辑、保存并管理你的简历。</p>

      <form class="mt-6 space-y-4" @submit.prevent="login">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">用户名</span>
          <input v-model="form.username" class="field" autocomplete="username" required />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">密码</span>
          <input v-model="form.password" class="field" type="password" autocomplete="current-password" required />
        </label>
        <p v-if="error" class="text-sm font-medium text-rose-500">{{ error }}</p>
        <button class="primary-button w-full" type="submit" :disabled="loggingIn">
          {{ loggingIn ? '登录中…' : '登录' }}
        </button>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminLogin } from '@/shared/api/auth'
import { setSessionToken } from '@/shared/auth/session'

const route = useRoute()
const router = useRouter()
const form = reactive({ username: '', password: '' })
const loggingIn = ref(false)
const error = ref('')

async function login() {
  loggingIn.value = true
  error.value = ''
  try {
    const result = await adminLogin(form.username.trim(), form.password)
    setSessionToken(result.token)
    await router.replace(resolveRedirect())
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loggingIn.value = false
  }
}

function resolveRedirect() {
  return route.query.redirect === '/tools/resume' ? '/tools/resume' : '/tools/resume'
}
</script>
