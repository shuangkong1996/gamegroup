<template>
  <section class="page-enter space-y-8">
    <div class="panel relative overflow-hidden p-6 sm:p-8">
      <div
        class="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-gradient-to-br from-brand-500/10 to-emerald-500/10 blur-2xl"
      />
      <div class="relative max-w-3xl">
        <p class="text-sm font-semibold tracking-wide text-brand-600">在线工具工作台</p>
        <h1 class="mt-3 text-4xl font-bold tracking-tight text-slate-950 dark:text-zinc-50 sm:text-5xl">
          高效、克制、可扩展的工具集合
        </h1>
        <p class="mt-5 max-w-2xl text-base leading-8 text-muted">
          高频工具在本地即时完成；复杂计算接入 GameGroup 后端服务，保持同一套极简体验。
        </p>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-3">
      <RouterLink
        v-for="category in categories"
        :key="category.key"
        class="panel flex items-center gap-4 p-5 transition-all duration-300 ease-in-out hover:-translate-y-1 hover:border-brand-500/30 hover:shadow-glow"
        :to="`/category/${category.key}`"
      >
        <div class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-indigo-500/10 text-brand-600">
          <component :is="category.icon" class="h-5 w-5" />
        </div>
        <div class="min-w-0">
          <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">{{ category.title }}</h2>
          <p class="mt-1 truncate text-xs text-muted">{{ category.description }}</p>
        </div>
      </RouterLink>
    </div>

    <div>
      <div class="mb-4 flex items-center justify-between gap-3">
        <h2 class="text-xl font-semibold tracking-tight text-slate-950 dark:text-zinc-50">全部工具</h2>
        <p class="text-sm text-muted">{{ visibleTools.length }} 个</p>
      </div>
      <div v-if="visibleTools.length" class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
        <ToolCard v-for="tool in visibleTools" :key="tool.key" :tool="tool" />
      </div>
      <div v-else class="empty-state">没有匹配的工具，试试换个关键词</div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ToolCard from '@/shared/components/ToolCard.vue'
import { useUiStore } from '@/app/stores/ui'
import { categories, searchTools, tools } from '@/tools/registry'

const ui = useUiStore()
const visibleTools = computed(() => searchTools(ui.searchKeyword, tools))
</script>
