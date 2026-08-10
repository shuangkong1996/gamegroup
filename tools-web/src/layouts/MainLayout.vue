<template>
  <div class="min-h-screen bg-canvas text-ink">
    <AppSidebar />

    <div
      v-if="ui.sidebarOpen"
      class="fixed inset-0 z-40 bg-slate-950/30 backdrop-blur-sm transition-opacity duration-300 lg:hidden"
      @click="ui.closeSidebar"
    />

    <div class="lg:pl-[280px]">
      <AppHeader />
      <main class="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppHeader from '@/shared/components/AppHeader.vue'
import AppSidebar from '@/shared/components/AppSidebar.vue'
import { useUiStore } from '@/app/stores/ui'

const ui = useUiStore()

onMounted(() => {
  ui.initTheme()
})
</script>
