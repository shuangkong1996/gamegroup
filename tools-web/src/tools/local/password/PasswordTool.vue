<template>
  <ToolWorkspace eyebrow="开发工具" title="随机密码生成" description="基于浏览器随机数生成密码，全程不经过服务端。">
    <template #actions>
      <CopyButton :value="passwords.join('\n')" label="复制全部" />
    </template>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">生成规则</h2>
      <div class="grid gap-4 sm:grid-cols-2">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">长度</span>
          <input v-model.number="length" class="field" type="number" min="6" max="128" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">数量</span>
          <input v-model.number="count" class="field" type="number" min="1" max="20" />
        </label>
      </div>
      <div class="mt-5 grid gap-3 sm:grid-cols-2">
        <label class="check-label"><input v-model="useUpper" type="checkbox" />大写字母</label>
        <label class="check-label"><input v-model="useLower" type="checkbox" />小写字母</label>
        <label class="check-label"><input v-model="useNumber" type="checkbox" />数字</label>
        <label class="check-label"><input v-model="useSymbol" type="checkbox" />符号</label>
      </div>
      <p v-if="error" class="mt-4 text-sm font-medium text-rose-500">{{ error }}</p>
      <button class="primary-button mt-5 w-full" type="button" @click="generate">生成密码</button>
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">生成结果</h2>
      <div class="space-y-3">
        <div
          v-for="password in passwords"
          :key="password"
          class="flex items-center justify-between gap-3 rounded-xl border border-line/70 bg-white/70 px-4 py-3 font-mono text-sm dark:bg-zinc-950/50"
        >
          <span class="min-w-0 break-all">{{ password }}</span>
          <CopyButton :value="password" />
        </div>
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const upper = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
const lower = 'abcdefghijklmnopqrstuvwxyz'
const numbers = '0123456789'
const symbols = '!@#$%^&*()-_=+[]{};:,.?'

const length = ref(18)
const count = ref(5)
const useUpper = ref(true)
const useLower = ref(true)
const useNumber = ref(true)
const useSymbol = ref(true)
const passwords = ref<string[]>([])
const error = ref('')

function randomIndex(max: number) {
  const array = new Uint32Array(1)
  crypto.getRandomValues(array)
  return array[0] % max
}

function buildCharset() {
  return [
    useUpper.value ? upper : '',
    useLower.value ? lower : '',
    useNumber.value ? numbers : '',
    useSymbol.value ? symbols : ''
  ].join('')
}

function generateOne(charset: string, safeLength: number) {
  return Array.from({ length: safeLength }, () => charset[randomIndex(charset.length)]).join('')
}

function generate() {
  const charset = buildCharset()
  if (!charset) {
    error.value = '请至少选择一种字符集'
    passwords.value = []
    return
  }
  error.value = ''
  const safeLength = Math.min(Math.max(Number(length.value) || 12, 6), 128)
  const safeCount = Math.min(Math.max(Number(count.value) || 1, 1), 20)
  length.value = safeLength
  count.value = safeCount
  passwords.value = Array.from({ length: safeCount }, () => generateOne(charset, safeLength))
}

generate()
</script>
