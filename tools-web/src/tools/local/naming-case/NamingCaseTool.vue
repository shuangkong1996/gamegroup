<template>
  <ToolWorkspace eyebrow="文本工具" title="命名风格转换" description="把短语或变量名转换为常见代码命名风格。">
    <template #actions>
      <CopyButton :value="selectedOutput" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="input = ''">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder="user profile name" />
    </section>

    <section class="panel p-5">
      <h2 class="mb-4 font-semibold tracking-tight text-slate-900 dark:text-zinc-100">转换结果</h2>
      <div class="space-y-3">
        <div
          v-for="item in outputs"
          :key="item.label"
          class="rounded-xl border border-line/70 bg-white/70 p-4 transition-all duration-300 ease-in-out hover:border-brand-500/20 dark:bg-zinc-950/50"
          :class="selectedOutput === item.value ? 'border-brand-500/30 ring-2 ring-brand-500/10' : ''"
        >
          <div class="mb-2 flex items-center justify-between gap-3">
            <p class="text-xs font-medium text-muted">{{ item.label }}</p>
            <CopyButton :value="item.value" />
          </div>
          <button
            class="w-full truncate text-left font-mono text-sm text-slate-900 dark:text-zinc-100"
            type="button"
            @click="selectedOutput = item.value"
          >
            {{ item.value || '—' }}
          </button>
        </div>
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, ref, watchEffect } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('user profile name')
const selectedOutput = ref('')

const words = computed(() =>
  input.value
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .split(/[^A-Za-z0-9]+/)
    .map((word) => word.trim())
    .filter(Boolean)
    .map((word) => word.toLowerCase())
)

function capitalize(value: string) {
  return value ? value[0].toUpperCase() + value.slice(1) : ''
}

const outputs = computed(() => {
  const list = words.value
  const pascal = list.map(capitalize).join('')
  return [
    { label: '小驼峰（camelCase）', value: list[0] ? list[0] + list.slice(1).map(capitalize).join('') : '' },
    { label: '大驼峰（PascalCase）', value: pascal },
    { label: '下划线（snake_case）', value: list.join('_') },
    { label: '短横线（kebab-case）', value: list.join('-') },
    { label: '全大写常量（CONSTANT_CASE）', value: list.join('_').toUpperCase() }
  ]
})

watchEffect(() => {
  selectedOutput.value = outputs.value[0]?.value ?? ''
})
</script>
