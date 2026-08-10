<template>
  <aside
    class="fixed inset-y-0 left-0 z-50 flex w-[280px] flex-col border-r border-line/70 bg-white/90 p-5 backdrop-blur-md transition-transform duration-300 ease-in-out dark:bg-zinc-950/90 lg:translate-x-0"
    :class="ui.sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'"
  >
    <RouterLink class="mb-8 flex items-center gap-3" to="/" @click="ui.closeSidebar">
      <div
        class="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-600 to-emerald-500 text-white shadow-glow"
      >
        <Sparkles class="h-5 w-5" />
      </div>
      <div>
        <p class="bg-gradient-to-r from-brand-600 to-emerald-500 bg-clip-text text-lg font-bold tracking-tight text-transparent">
          工具箱
        </p>
        <p class="text-xs font-medium text-muted">现代效率工具台</p>
      </div>
    </RouterLink>

    <nav class="flex-1 space-y-1.5 overflow-y-auto pr-1">
      <RouterLink
        class="group relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-muted transition-all duration-300 ease-in-out hover:bg-gradient-to-r hover:from-indigo-50 hover:to-transparent hover:text-brand-600 dark:hover:from-indigo-950/30"
        :class="route.name === 'home' ? 'bg-indigo-50 text-brand-600 dark:bg-indigo-950/30' : ''"
        to="/"
        @click="ui.closeSidebar"
      >
        <span
          v-if="route.name === 'home'"
          class="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-full bg-brand-600"
        />
        <LayoutGrid class="h-5 w-5 shrink-0" />
        全部工具
      </RouterLink>

      <RouterLink
        v-for="category in categories"
        :key="category.key"
        class="group relative flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-muted transition-all duration-300 ease-in-out hover:bg-gradient-to-r hover:from-indigo-50 hover:to-transparent hover:text-brand-600 dark:hover:from-indigo-950/30"
        :class="isActive(category.key) ? 'bg-indigo-50 text-brand-600 dark:bg-indigo-950/30' : ''"
        :to="`/category/${category.key}`"
        @click="ui.closeSidebar"
      >
        <span
          v-if="isActive(category.key)"
          class="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-full bg-brand-600"
        />
        <component :is="category.icon" class="h-5 w-5 shrink-0" />
        {{ category.title }}
      </RouterLink>
    </nav>

    <p class="mt-6 border-t border-line/60 pt-4 text-xs leading-5 text-muted">
      本地工具在浏览器内完成，无需登录。
    </p>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { LayoutGrid, Sparkles } from '@lucide/vue'
import { categories } from '@/tools/registry'
import { useUiStore } from '@/app/stores/ui'

const route = useRoute()
const ui = useUiStore()

function isActive(categoryKey: string) {
  return route.name === 'category' && route.params.categoryKey === categoryKey
}
</script>
