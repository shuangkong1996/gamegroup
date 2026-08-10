<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const errorMsg = ref('')

async function onSubmit() {
  errorMsg.value = ''
  if (!username.value.trim() || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  if (mode.value === 'register' && password.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login(username.value.trim(), password.value)
    } else {
      await userStore.registerAndLogin(username.value.trim(), password.value)
    }
    router.replace('/lobby')
  } catch (err) {
    errorMsg.value = (err as Error).message || '操作失败'
  } finally {
    loading.value = false
  }
}

function switchMode(m: 'login' | 'register') {
  mode.value = m
  errorMsg.value = ''
}
</script>

<template>
  <div class="flex h-full items-center justify-center bg-canvas p-4">
    <div class="w-full max-w-sm">
      <div class="mb-8 text-center">
        <h1 class="text-3xl font-bold text-gold-500">Poker</h1>
        <p class="mt-2 text-sm text-muted">德州扑克 · 娱乐平台</p>
      </div>

      <div class="rounded-2xl border border-line bg-slate-900/60 p-6 shadow-card">
        <div class="mb-6 grid grid-cols-2 rounded-lg bg-slate-800 p-1 text-sm">
          <button
            class="rounded-md py-2 transition"
            :class="mode === 'login' ? 'bg-slate-700 text-ink' : 'text-muted hover:text-ink'"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            class="rounded-md py-2 transition"
            :class="mode === 'register' ? 'bg-slate-700 text-ink' : 'text-muted hover:text-ink'"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <form class="space-y-4" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1 block text-sm text-muted">用户名</label>
            <input
              v-model="username"
              type="text"
              autocomplete="username"
              class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 outline-none transition focus:border-gold-500"
              placeholder="请输入用户名"
            />
          </div>
          <div>
            <label class="mb-1 block text-sm text-muted">密码</label>
            <input
              v-model="password"
              type="password"
              autocomplete="current-password"
              class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 outline-none transition focus:border-gold-500"
              placeholder="请输入密码"
            />
          </div>
          <div v-if="mode === 'register'">
            <label class="mb-1 block text-sm text-muted">确认密码</label>
            <input
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 outline-none transition focus:border-gold-500"
              placeholder="请再次输入密码"
            />
          </div>

          <p v-if="errorMsg" class="text-sm text-red-400">{{ errorMsg }}</p>

          <button
            type="submit"
            :disabled="loading"
            class="w-full rounded-lg bg-gold-500 py-2.5 font-semibold text-slate-900 transition hover:bg-gold-400 disabled:opacity-50"
          >
            {{ loading ? '处理中...' : mode === 'login' ? '登 录' : '注册并登录' }}
          </button>
        </form>
      </div>

      <p class="mt-4 text-center text-xs text-muted">
        娱乐用途，使用虚拟筹码，不涉及真实货币
      </p>
    </div>
  </div>
</template>
