<template>
  <section class="page-enter space-y-6">
    <div class="panel p-6">
      <div class="flex items-start gap-4">
        <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-indigo-500/10 text-brand-600">
          <component :is="category?.icon" class="h-5 w-5" />
        </div>
        <div>
          <p class="text-sm font-semibold tracking-wide text-brand-600">工具分类</p>
          <h1 class="mt-1 text-3xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">
            {{ category?.title || '未知分类' }}
          </h1>
          <p class="mt-2 text-sm leading-7 text-muted">{{ category?.description }}</p>
        </div>
      </div>
    </div>

    <div v-if="visibleTools.length" class="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
      <ToolCard v-for="tool in visibleTools" :key="tool.key" :tool="tool" />
    </div>
    <div v-else class="empty-state">没有匹配的工具</div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ToolCard from '@/shared/components/ToolCard.vue'
import { useUiStore } from '@/app/stores/ui'
import { getCategory, getToolsByCategory, searchTools } from '@/tools/registry'

const route = useRoute()
const ui = useUiStore()

const categoryKey = computed(() => String(route.params.categoryKey || ''))
const category = computed(() => getCategory(categoryKey.value))
const categoryTools = computed(() => getToolsByCategory(categoryKey.value))
const visibleTools = computed(() => searchTools(ui.searchKeyword, categoryTools.value))
</script>
