<template>
  <ToolWorkspace eyebrow="开发工具" title="网址参数解析" description="解析查询参数，支持从参数表反向构造查询字符串。">
    <template #actions>
      <CopyButton :value="queryString" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入网址</h2>
        <button class="secondary-button" type="button" @click="clear">清空</button>
      </div>
      <textarea v-model="urlText" class="code-field" spellcheck="false" placeholder="https://example.com?a=1&b=2" />
      <button class="primary-button mt-4 w-full" type="button" @click="parse">解析参数</button>
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>
    </section>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">参数列表</h2>
        <button class="secondary-button" type="button" @click="addRow">新增</button>
      </div>
      <div class="space-y-3">
        <div v-for="(row, index) in rows" :key="index" class="grid grid-cols-[1fr_1fr_auto] gap-2">
          <input v-model="row.key" class="field" placeholder="参数名" />
          <input v-model="row.value" class="field" placeholder="参数值" />
          <button class="secondary-button px-3" type="button" @click="rows.splice(index, 1)">删除</button>
        </div>
      </div>
      <label class="mt-5 block">
        <span class="mb-2 block text-sm font-medium text-muted">查询字符串</span>
        <textarea v-model="queryString" class="code-field min-h-[140px]" spellcheck="false" readonly />
      </label>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

interface ParamRow {
  key: string
  value: string
}

const urlText = ref('https://example.com/search?q=工具箱&page=1')
const rows = ref<ParamRow[]>([])
const error = ref('')

const queryString = computed(() => {
  const params = new URLSearchParams()
  rows.value.forEach((row) => {
    if (row.key) {
      params.append(row.key, row.value)
    }
  })
  return params.toString()
})

function addRow() {
  rows.value.push({ key: '', value: '' })
}

function parse() {
  try {
    error.value = ''
    const value = urlText.value.trim()
    const query = value.includes('?') ? value.slice(value.indexOf('?') + 1) : value
    const params = new URLSearchParams(query)
    rows.value = Array.from(params.entries()).map(([key, value]) => ({ key, value }))
  } catch {
    error.value = '网址参数解析失败'
  }
}

function clear() {
  urlText.value = ''
  rows.value = []
  error.value = ''
}

parse()
</script>
