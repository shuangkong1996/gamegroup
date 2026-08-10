<template>
  <section class="page-enter space-y-6">
    <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-sm font-semibold tracking-wide text-brand-600">{{ eyebrow }}</p>
        <h1 class="mt-2 text-3xl font-bold tracking-tight text-slate-950 dark:text-zinc-50">{{ title }}</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-muted">{{ description }}</p>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-2">
        <slot name="actions" />
      </div>
    </div>

    <div class="grid gap-6 lg:grid-cols-2">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  description: string
}>()
</script>
