<template>
  <ToolWorkspace eyebrow="开发工具" title="哈希摘要" description="使用浏览器内置能力计算 SHA 系列摘要。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="input = ''">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder="输入要计算摘要的文本" />
      <label class="mt-4 block">
        <span class="mb-2 block text-sm font-medium text-muted">算法</span>
        <select v-model="algorithm" class="field">
          <option value="SHA-1">SHA-1</option>
          <option value="SHA-256">SHA-256</option>
          <option value="SHA-384">SHA-384</option>
          <option value="SHA-512">SHA-512</option>
        </select>
      </label>
      <button class="primary-button mt-4 w-full" type="button" @click="digest">计算摘要</button>
    </section>

    <section class="panel p-5">
      <h2 class="mb-4 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输出</h2>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('GameGroup 工具箱')
const algorithm = ref('SHA-256')
const output = ref('')

function toHex(buffer: ArrayBuffer) {
  return Array.from(new Uint8Array(buffer))
    .map((byte) => byte.toString(16).padStart(2, '0'))
    .join('')
}

async function digest() {
  const bytes = new TextEncoder().encode(input.value)
  output.value = toHex(await crypto.subtle.digest(algorithm.value, bytes))
}

void digest()
</script>
