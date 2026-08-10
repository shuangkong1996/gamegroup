<template>
  <ToolWorkspace eyebrow="开发工具" title="图片转 Base64" description="本地图片转数据地址，并支持粘贴数据地址预览。">
    <template #actions>
      <CopyButton :value="dataUrl" />
    </template>

    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">选择图片</h2>
        <button class="secondary-button" type="button" @click="clear">清空</button>
      </div>
      <label
        class="flex min-h-[220px] cursor-pointer flex-col items-center justify-center rounded-2xl border border-dashed border-line/80 bg-white/60 p-6 text-center transition-all duration-300 ease-in-out hover:border-brand-500/30 hover:bg-indigo-50/40 dark:bg-zinc-950/40 dark:hover:bg-indigo-950/20"
      >
        <input class="hidden" type="file" accept="image/*" @change="handleFileChange" />
        <span class="text-sm font-medium text-slate-900 dark:text-zinc-100">点击选择本地图片</span>
        <span class="mt-2 text-xs text-muted">文件只在浏览器读取，不会上传</span>
      </label>

      <div class="mt-5 grid gap-4 sm:grid-cols-2">
        <MetricCard label="文件名" :value="fileName || '—'" />
        <MetricCard label="大小" :value="fileSize" />
      </div>
      <p v-if="error" class="mt-4 text-sm font-medium text-rose-500">{{ error }}</p>
    </section>

    <section class="panel p-5">
      <h2 class="mb-5 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">数据地址 / 预览</h2>
      <textarea
        v-model="dataUrl"
        class="code-field min-h-[180px]"
        spellcheck="false"
        placeholder="也可以粘贴 data:image/...;base64,..."
      />
      <div
        class="mt-5 flex min-h-[220px] items-center justify-center overflow-hidden rounded-2xl border border-line/70 bg-slate-100/70 dark:bg-zinc-950/50"
      >
        <img v-if="dataUrl" :src="dataUrl" class="max-h-[360px] max-w-full object-contain" alt="图片预览" />
        <span v-else class="text-sm text-muted">等待图片</span>
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const dataUrl = ref('')
const fileName = ref('')
const rawSize = ref(0)
const error = ref('')

const fileSize = computed(() => {
  if (!rawSize.value) {
    return '—'
  }
  if (rawSize.value < 1024 * 1024) {
    return `${(rawSize.value / 1024).toFixed(2)} KB`
  }
  return `${(rawSize.value / 1024 / 1024).toFixed(2)} MB`
})

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  if (!file.type.startsWith('image/')) {
    error.value = '请选择图片文件'
    return
  }
  error.value = ''
  fileName.value = file.name
  rawSize.value = file.size
  const reader = new FileReader()
  reader.onload = () => {
    dataUrl.value = String(reader.result || '')
  }
  reader.onerror = () => {
    error.value = '图片读取失败'
  }
  reader.readAsDataURL(file)
}

function clear() {
  dataUrl.value = ''
  fileName.value = ''
  rawSize.value = 0
  error.value = ''
}
</script>
