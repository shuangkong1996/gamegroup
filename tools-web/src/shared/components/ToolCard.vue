<template>
  <RouterLink
    class="group panel block p-5 transition-all duration-300 ease-in-out hover:-translate-y-1 hover:border-brand-500/30 hover:shadow-glow"
    :to="tool.path"
  >
    <div class="mb-5 flex items-start justify-between gap-4">
      <div
        class="flex h-12 w-12 items-center justify-center rounded-2xl transition-all duration-300 ease-in-out group-hover:scale-105"
        :class="accentClass"
      >
        <component :is="tool.icon" class="h-5 w-5" />
      </div>
      <span
        class="rounded-full border border-line/70 px-2.5 py-1 text-xs font-medium text-muted transition-colors duration-300 group-hover:border-brand-500/20 group-hover:text-brand-600"
      >
        {{ modeLabel }}
      </span>
    </div>
    <h3 class="text-base font-semibold tracking-tight text-slate-900 dark:text-zinc-100">{{ tool.title }}</h3>
    <p class="mt-2 line-clamp-2 text-sm leading-6 text-muted">{{ tool.description }}</p>
  </RouterLink>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ToolMeta } from '@/tools/types'

const props = defineProps<{
  tool: ToolMeta
}>()

const modeLabel = computed(() => (props.tool.mode === 'local' ? '本地' : '服务端'))

const accentClass = computed(() => {
  const classes: Record<string, string> = {
    indigo: 'bg-indigo-500/10 text-indigo-600 dark:text-indigo-400',
    emerald: 'bg-emerald-500/10 text-emerald-600 dark:text-emerald-400',
    amber: 'bg-amber-500/10 text-amber-600 dark:text-amber-400',
    sky: 'bg-sky-500/10 text-sky-600 dark:text-sky-400',
    violet: 'bg-violet-500/10 text-violet-600 dark:text-violet-400'
  }
  return classes[props.tool.accent] ?? classes.indigo
})
</script>
