<template>
  <component :is="asyncTool" v-if="tool && asyncTool" />
  <div v-else class="panel page-enter p-8">
    <p class="text-sm font-semibold tracking-wide text-brand-600">未找到</p>
    <h1 class="mt-2 text-2xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">工具不存在</h1>
    <p class="mt-3 text-sm text-muted">请从首页或分类页重新选择工具。</p>
    <RouterLink class="secondary-button mt-6" to="/">返回首页</RouterLink>
  </div>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import { useRoute } from 'vue-router'
import { getTool } from '@/tools/registry'

const route = useRoute()

const tool = computed(() => getTool(String(route.params.toolKey || '')))
const asyncTool = computed(() => {
  if (!tool.value) {
    return null
  }
  return defineAsyncComponent(tool.value.loader)
})
</script>
