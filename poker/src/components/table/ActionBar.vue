<script setup lang="ts">
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useTableStore } from '@/stores/table'

const store = useTableStore()

const raiseTo = ref(0)
const countdown = ref(0)
let timer: number | null = null

const snapshot = computed(() => store.snapshot)
const myChip = computed(() => Number(store.me?.chip ?? 0))
const callAmount = computed(() => Number(snapshot.value?.callAmount ?? 0))
const minRaise = computed(() => Number(snapshot.value?.minRaise ?? 0))
const maxRaise = computed(() => myChip.value + Number(store.me?.totalBet ?? 0))

const canCheck = computed(() => callAmount.value === 0)
const canCall = computed(() => callAmount.value > 0 && callAmount.value < maxRaise.value)

const canBet = computed(() => Number(snapshot.value?.currentBet ?? 0) === 0 && myChip.value > 0)

const minBetValue = computed(() => {
  const base = callAmount.value > 0 ? minRaise.value : Number(snapshot.value?.bigBlind ?? 0)
  return Math.min(base, maxRaise.value)
})

watch(
  () => store.snapshot?.phase,
  () => {
    raiseTo.value = minBetValue.value
  },
  { immediate: true }
)

function updateCountdown() {
  const deadline = snapshot.value?.actionDeadline
  if (deadline) {
    countdown.value = Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
  } else {
    countdown.value = 0
  }
}

watch(() => store.snapshot?.actionDeadline, updateCountdown, { immediate: true })

onMounted(() => {
  timer = window.setInterval(updateCountdown, 500)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

function doAction(action: 'FOLD' | 'CHECK' | 'CALL' | 'BET' | 'RAISE' | 'ALL_IN', amount?: number) {
  store.act(action, amount)
}

function betOrRaise() {
  const target = Math.min(Math.max(raiseTo.value, minBetValue.value), maxRaise.value)
  if (target >= maxRaise.value) {
    doAction('ALL_IN')
  } else if (callAmount.value === 0) {
    doAction('BET', target)
  } else {
    doAction('RAISE', target)
  }
}
</script>

<template>
  <div class="mx-auto w-full max-w-lg">
    <div class="rounded-2xl border border-line bg-slate-900/80 p-4 shadow-card">
      <div class="mb-3 flex items-center justify-between text-sm">
        <span class="text-muted">我的筹码</span>
        <span class="font-bold text-gold-400">{{ myChip.toLocaleString() }}</span>
      </div>
      <div v-if="countdown > 0" class="mb-3 flex justify-center">
        <span
          class="rounded-full px-3 py-1 text-sm font-bold"
          :class="countdown <= 5 ? 'bg-red-500/20 text-red-300' : 'bg-slate-700 text-muted'"
        >
          剩余 {{ countdown }} 秒
        </span>
      </div>

      <div class="flex items-center gap-2">
        <button
          class="flex-1 rounded-lg bg-slate-700 py-2.5 text-sm font-semibold transition hover:bg-slate-600 disabled:opacity-40"
          @click="doAction('FOLD')"
        >
          弃牌
        </button>

        <button
          v-if="canCheck"
          class="flex-1 rounded-lg bg-emerald-600 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-500"
          @click="doAction('CHECK')"
        >
          过牌
        </button>
        <button
          v-else
          class="flex-1 rounded-lg bg-emerald-600 py-2.5 text-sm font-semibold text-white transition hover:bg-emerald-500 disabled:opacity-40"
          :disabled="!canCall"
          @click="doAction('CALL', callAmount)"
        >
          跟注 {{ callAmount.toLocaleString() }}
        </button>

        <button
          v-if="canBet"
          class="flex-1 rounded-lg bg-gold-500 py-2.5 text-sm font-semibold text-slate-900 transition hover:bg-gold-400"
          @click="betOrRaise"
        >
          下注
        </button>
        <button
          v-else
          class="flex-1 rounded-lg bg-gold-500 py-2.5 text-sm font-semibold text-slate-900 transition hover:bg-gold-400 disabled:opacity-40"
          :disabled="callAmount <= 0"
          @click="betOrRaise"
        >
          加注
        </button>
      </div>

      <div class="mt-3">
        <div class="mb-1 flex justify-between text-xs text-muted">
          <span>最小 {{ minBetValue.toLocaleString() }}</span>
          <span>全下 {{ maxRaise.toLocaleString() }}</span>
        </div>
        <input
          v-model.number="raiseTo"
          type="range"
          :min="minBetValue"
          :max="maxRaise"
          :step="1"
          class="w-full accent-gold-500"
        />
        <div class="mt-1 text-center text-sm font-semibold text-ink">
          加注到 {{ Math.min(Math.max(raiseTo, minBetValue), maxRaise).toLocaleString() }}
        </div>
      </div>

      <button
        class="mt-3 w-full rounded-lg bg-red-600 py-2 text-sm font-bold text-white transition hover:bg-red-500 disabled:opacity-40"
        :disabled="myChip <= 0"
        @click="doAction('ALL_IN')"
      >
        全下（ALL-IN）
      </button>
    </div>
  </div>
</template>
