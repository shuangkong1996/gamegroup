<script setup lang="ts">
import PlayingCard from './PlayingCard.vue'

defineProps<{
  board: string[]
  pot: string
}>()
</script>

<template>
  <div class="flex flex-col items-center gap-2">
    <div class="flex gap-1.5">
      <PlayingCard v-for="(c, i) in Array.from({ length: 5 }, (_, i) => board[i])" :key="i" :code="c" :small="true" />
    </div>
    <div class="rounded-full bg-black/40 px-4 py-1 text-sm font-semibold text-gold-300">
      底池 {{ Number(pot).toLocaleString() }}
    </div>
  </div>
</template>
