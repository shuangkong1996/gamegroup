<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchStats, type PokerStatsDTO } from '@/shared/api/poker'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const stats = ref<PokerStatsDTO | null>(null)
const loading = ref(true)
const errorMsg = ref('')

onMounted(async () => {
  try {
    stats.value = await fetchStats()
  } catch (err) {
    errorMsg.value = (err as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
})

function percent(rate: string) {
  return (Number(rate) * 100).toFixed(1) + '%'
}

function winText(win: string) {
  const n = Number(win)
  return n >= 0 ? `+${n.toLocaleString()}` : n.toLocaleString()
}
</script>

<template>
  <div class="min-h-screen bg-canvas">
    <header class="flex items-center justify-between border-b border-line bg-slate-900/70 px-6 py-4">
      <button class="text-sm text-muted transition hover:text-ink" @click="router.push('/lobby')">← 返回大厅</button>
      <h1 class="text-lg font-bold text-gold-500">我的战绩</h1>
      <span class="w-16"></span>
    </header>

    <main class="mx-auto max-w-lg px-6 py-8">
      <p v-if="loading" class="py-12 text-center text-sm text-muted">加载中…</p>
      <p v-if="errorMsg" class="py-12 text-center text-sm text-red-400">{{ errorMsg }}</p>

      <template v-if="stats">
        <div class="mb-6 rounded-2xl border border-line bg-slate-900/60 p-5 text-center">
          <div class="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-700 text-2xl font-bold text-gold-400">
            {{ userStore.userInfo?.nickname?.charAt(0) || '?' }}
          </div>
          <div class="mt-2 font-semibold text-ink">{{ userStore.userInfo?.nickname || '玩家' }}</div>
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div class="rounded-2xl border border-line bg-slate-900/60 p-4 text-center">
            <div class="text-2xl font-bold text-ink">{{ stats.handsPlayed }}</div>
            <div class="mt-1 text-xs text-muted">参与手数</div>
          </div>
          <div class="rounded-2xl border border-line bg-slate-900/60 p-4 text-center">
            <div class="text-2xl font-bold text-emerald-400">{{ stats.handsWon }}</div>
            <div class="mt-1 text-xs text-muted">赢下手数</div>
          </div>
          <div class="rounded-2xl border border-line bg-slate-900/60 p-4 text-center">
            <div class="text-2xl font-bold text-ink">{{ percent(stats.winRate) }}</div>
            <div class="mt-1 text-xs text-muted">胜率</div>
          </div>
        </div>

        <div class="mt-3 grid grid-cols-2 gap-3">
          <div class="rounded-2xl border border-line bg-slate-900/60 p-4 text-center">
            <div class="text-xl font-bold" :class="Number(stats.totalWin) >= 0 ? 'text-emerald-400' : 'text-red-400'">
              {{ winText(stats.totalWin) }}
            </div>
            <div class="mt-1 text-xs text-muted">累计赢输</div>
          </div>
          <div class="rounded-2xl border border-line bg-slate-900/60 p-4 text-center">
            <div class="text-xl font-bold text-ink">{{ Number(stats.totalBet).toLocaleString() }}</div>
            <div class="mt-1 text-xs text-muted">累计投入</div>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>
