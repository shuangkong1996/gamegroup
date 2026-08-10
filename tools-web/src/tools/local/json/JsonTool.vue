<template>
  <ToolWorkspace eyebrow="开发工具" title="JSON 格式化" description="格式化、压缩与校验 JSON 文本。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="clear">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder='{"名称":"工具箱"}' />
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>
    </section>

    <section class="panel p-5">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输出</h2>
        <div class="flex gap-2">
          <button class="secondary-button" type="button" @click="compact">压缩</button>
          <button class="primary-button" type="button" @click="format">格式化</button>
        </div>
      </div>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('{\n  "名称": "工具箱",\n  "类型": "效率工具"\n}')
const output = ref('')
const error = ref('')

function parseJson() {
  try {
    error.value = ''
    return JSON.parse(input.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'JSON 解析失败'
    return null
  }
}

function format() {
  const value = parseJson()
  if (value !== null) {
    output.value = JSON.stringify(value, null, 2)
  }
}

function compact() {
  const value = parseJson()
  if (value !== null) {
    output.value = JSON.stringify(value)
  }
}

function clear() {
  input.value = ''
  output.value = ''
  error.value = ''
}

format()
</script>
