<script setup lang="ts">
import { computed, ref } from 'vue'
import { useTableStore } from '@/stores/table'
import { HAND_RANK_NAMES } from '@/shared/poker'
import PlayingCard from './PlayingCard.vue'

const store = useTableStore()

const dismissed = ref('')

const lastHand = computed(() => {
  const hand = store.snapshot?.lastHand
  if (!hand) return null
  const sig = JSON.stringify({ w: hand.winners, p: hand.pot })
  if (sig === dismissed.value) return null
  return hand
})

const entries = computed(() => {
  const hand = lastHand.value
  if (!hand) return []
  return hand.winners
    .map((seat) => {
      const player = store.snapshot?.players.find((p) => p.seat === seat)
      return {
        seat,
        nickname: player?.nickname ?? `座位${seat}`,
        delta: hand.chipDelta[seat],
        cards: hand.revealedHoleCards[seat] ?? [],
        rank: hand.bestHands[seat] ? HAND_RANK_NAMES[hand.bestHands[seat]] ?? hand.bestHands[seat] : ''
      }
    })
})

const closed = computed(() => !lastHand.value)

function close() {
  const hand = store.snapshot?.lastHand
  if (hand) {
    dismissed.value = JSON.stringify({ w: hand.winners, p: hand.pot })
  }
}
</script>

<template>
  <div v-if="lastHand" class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
    <div class="w-full max-w-md animate-fade-up rounded-2xl border border-line bg-slate-900 p-6 shadow-soft">
      <h2 class="mb-4 text-center text-xl font-bold text-gold-400">本手结算</h2>

      <div class="mb-4 text-center text-sm text-muted">
        底池 <span class="font-bold text-ink">{{ Number(lastHand.pot).toLocaleString() }}</span>
      </div>

      <div class="space-y-3">
        <div
          v-for="e in entries"
          :key="e.seat"
          class="flex items-center gap-3 rounded-xl border border-gold-500/40 bg-slate-800/70 p-3"
        >
          <div class="flex gap-0.5">
            <PlayingCard v-for="(c, i) in e.cards" :key="i" :code="c" small />
          </div>
          <div class="flex-1">
            <div class="text-sm font-semibold text-ink">{{ e.nickname }}</div>
            <div class="text-xs text-muted">{{ e.rank }}</div>
          </div>
          <div class="text-lg font-bold text-emerald-400">+{{ Number(e.delta).toLocaleString() }}</div>
        </div>
      </div>

      <button
        class="mt-6 w-full rounded-lg bg-gold-500 py-2.5 font-semibold text-slate-900 transition hover:bg-gold-400"
        @click="close"
      >
        {{ closed ? '继续' : '继续游戏' }}
      </button>
    </div>
  </div>
</template>
