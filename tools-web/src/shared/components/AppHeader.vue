<template>
  <header class="glass-header">
    <div class="flex h-16 items-center gap-3 px-4 sm:gap-4 sm:px-6 lg:px-8">
      <button class="icon-button lg:hidden" type="button" aria-label="打开导航" @click="ui.openSidebar">
        <Menu class="h-5 w-5" />
      </button>

      <div class="hidden min-w-0 flex-1 lg:block">
        <p class="truncate text-sm font-medium text-muted">GameGroup 工具箱</p>
      </div>

      <label
        class="flex h-11 w-full max-w-xl items-center gap-3 rounded-full border border-line/70 bg-white/80 px-4 text-sm shadow-sm transition-all duration-300 ease-in-out focus-within:border-brand-500/40 focus-within:ring-2 focus-within:ring-brand-500/10 dark:bg-zinc-900/80"
      >
        <Search class="h-4 w-4 shrink-0 text-muted" />
        <input
          ref="searchInput"
          v-model="ui.searchKeyword"
          class="min-w-0 flex-1 bg-transparent text-ink placeholder:text-muted"
          type="search"
          placeholder="搜索工具名称、标签…"
        />
        <span
          class="hidden shrink-0 rounded-full border border-line/70 bg-slate-100 px-2 py-0.5 text-xs font-medium text-muted dark:bg-zinc-800 sm:inline-flex"
        >
          {{ shortcutLabel }}
        </span>
      </label>

      <button
        class="icon-button"
        type="button"
        :aria-label="ui.darkMode ? '切换浅色模式' : '切换深色模式'"
        @click="ui.toggleTheme"
      >
        <Sun v-if="ui.darkMode" class="h-5 w-5" />
        <Moon v-else class="h-5 w-5" />
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Menu, Moon, Search, Sun } from '@lucide/vue'
import { useUiStore } from '@/app/stores/ui'

const ui = useUiStore()
const searchInput = ref<HTMLInputElement>()

const shortcutLabel = computed(() => (navigator.platform.toLowerCase().includes('mac') ? '⌘ K' : 'Ctrl K'))

function handleShortcut(event: KeyboardEvent) {
  const isMac = navigator.platform.toLowerCase().includes('mac')
  const matched = isMac ? event.metaKey && event.key.toLowerCase() === 'k' : event.ctrlKey && event.key.toLowerCase() === 'k'
  if (matched) {
    event.preventDefault()
    searchInput.value?.focus()
  }
}

onMounted(() => window.addEventListener('keydown', handleShortcut))
onBeforeUnmount(() => window.removeEventListener('keydown', handleShortcut))
</script>
