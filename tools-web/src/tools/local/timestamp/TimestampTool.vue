<template>
  <ToolWorkspace eyebrow="开发工具" title="时间戳转换" description="秒、毫秒时间戳与本地时间双向转换。">
    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">时间戳</h2>
        <button class="secondary-button" type="button" @click="useNow">当前时间</button>
      </div>
      <div class="space-y-4">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">时间戳数值</span>
          <input v-model="timestamp" class="field" inputmode="numeric" />
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">单位</span>
          <select v-model="unit" class="field">
            <option value="seconds">秒</option>
            <option value="milliseconds">毫秒</option>
          </select>
        </label>
        <button class="primary-button w-full" type="button" @click="convertFromTimestamp">转换</button>
      </div>
    </section>

    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">本地时间</h2>
        <CopyButton :value="dateText" />
      </div>
      <input v-model="dateText" class="field" type="datetime-local" step="1" />
      <button class="secondary-button mt-4 w-full" type="button" @click="convertFromDate">反向生成时间戳</button>
      <div class="mt-6 grid gap-4 sm:grid-cols-2">
        <MetricCard label="秒" :value="seconds" />
        <MetricCard label="毫秒" :value="milliseconds" />
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import MetricCard from '@/shared/components/MetricCard.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const timestamp = ref('')
const unit = ref<'seconds' | 'milliseconds'>('milliseconds')
const dateText = ref('')
const seconds = ref('')
const milliseconds = ref('')

function toLocalInputValue(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function sync(date: Date) {
  dateText.value = toLocalInputValue(date)
  milliseconds.value = String(date.getTime())
  seconds.value = String(Math.floor(date.getTime() / 1000))
  timestamp.value = unit.value === 'seconds' ? seconds.value : milliseconds.value
}

function useNow() {
  sync(new Date())
}

function convertFromTimestamp() {
  const value = Number(timestamp.value)
  if (!Number.isFinite(value)) {
    return
  }
  sync(new Date(unit.value === 'seconds' ? value * 1000 : value))
}

function convertFromDate() {
  const date = new Date(dateText.value)
  if (!Number.isNaN(date.getTime())) {
    sync(date)
  }
}

useNow()
</script>
