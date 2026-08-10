<template>
  <div
    class="rounded-2xl border border-line/70 bg-white/70 p-4 shadow-sm transition-all duration-300 ease-in-out hover:border-brand-500/20 dark:bg-zinc-950/40"
  >
    <p class="text-xs font-medium tracking-wide text-muted">{{ label }}</p>
    <p class="mt-2 break-all text-2xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">{{ value }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
}>()
</script>
