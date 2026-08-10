<script setup lang="ts">
import type { PlayerView } from '@/stores/table'
import PlayingCard from './PlayingCard.vue'

defineProps<{
  player: PlayerView | null
  isMe: boolean
  sitting: boolean
  canSit: boolean
  showingCards: boolean
}>()

const emit = defineEmits<{
  sit: []
}>()
</script>

<template>
  <div class="absolute z-20 w-28 -translate-x-1/2 -translate-y-1/2">
    <div v-if="player">
      <div
        class="mx-auto mb-1 flex h-10 w-10 items-center justify-center rounded-full border-2 text-sm font-bold"
        :class="isMe ? 'border-gold-500 bg-slate-700' : player.folded ? 'border-line bg-slate-800 opacity-50' : 'border-slate-500 bg-slate-700'"
      >
        {{ player.nickname?.charAt(0) || '?' }}
      </div>

      <div
        class="relative rounded-lg border px-2 py-1 text-center shadow-card"
        :class="
          isMe
            ? 'border-gold-500 bg-slate-900/95'
            : player.folded
              ? 'border-line bg-slate-900/80 opacity-50'
              : 'border-slate-600 bg-slate-900/90'
        "
      >
        <div class="absolute -top-2 left-1/2 z-10 flex -translate-x-1/2 gap-0.5">
          <span
            v-if="player.isDealer"
            class="flex h-4 w-4 items-center justify-center rounded-full bg-white text-[10px] font-bold text-slate-900"
            title="庄家"
            >D</span
          >
          <span
            v-if="player.allIn"
            class="rounded bg-red-500 px-1 text-[10px] font-semibold text-white"
            >ALL-IN</span
          >
        </div>

        <div class="truncate text-xs font-medium text-ink">{{ player.nickname }}</div>
        <div class="text-sm font-bold text-gold-400">{{ Number(player.chip).toLocaleString() }}</div>

        <div v-if="player.roundBet && Number(player.roundBet) > 0" class="mt-1 rounded bg-felt px-1 py-0.5 text-[11px] text-emerald-100">
          {{ Number(player.roundBet).toLocaleString() }}
        </div>
      </div>

      <div v-if="sitting && !player.folded" class="mt-1 flex justify-center gap-1">
        <PlayingCard v-for="(c, i) in (player.holeCards || [])" :key="i" :code="showingCards ? c : undefined" :hidden="!showingCards" small />
      </div>
    </div>

    <button
      v-else-if="canSit"
      class="mx-auto mt-6 rounded-full border border-dashed border-line bg-slate-800/70 px-4 py-2 text-sm text-muted transition hover:border-gold-500 hover:text-gold-400"
      @click="emit('sit')"
    >
      + 坐下
    </button>
    <div v-else class="mt-6 text-center text-xs text-muted/50">空座</div>
  </div>
</template>
