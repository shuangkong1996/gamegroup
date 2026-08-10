<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTableStore } from '@/stores/table'
import { useUserStore } from '@/stores/user'
import type { PokerRoomDTO } from '@/shared/api/poker-types'
import PokerTable from '@/components/table/PokerTable.vue'
import ActionBar from '@/components/table/ActionBar.vue'
import HandResultModal from '@/components/table/HandResultModal.vue'

const route = useRoute()
const router = useRouter()
const store = useTableStore()
const userStore = useUserStore()

const roomId = route.params.roomId as string
const roomInfo = ref<PokerRoomDTO | null>(null)

const sitTarget = ref<number | null>(null)
const buyIn = ref(0)
const buyInError = ref('')

const snapshot = computed(() => store.snapshot)
const showActionBar = computed(
  () =>
    snapshot.value?.status === 'PLAYING' &&
    snapshot.value.currentActor != null &&
    snapshot.value.currentActor === snapshot.value.mySeat
)

onMounted(async () => {
  await store.enterRoom(roomId)
  roomInfo.value = await store.fetchRoomInfo()
})

onBeforeUnmount(() => {
  store.leaveRoom()
})

function openSit(seat: number) {
  if (!roomInfo.value) return
  sitTarget.value = seat
  buyIn.value = Number(roomInfo.value.buyInMin)
  buyInError.value = ''
}

function confirmSit() {
  if (!roomInfo.value || sitTarget.value == null) return
  const min = Number(roomInfo.value.buyInMin)
  const max = Number(roomInfo.value.buyInMax)
  if (buyIn.value < min || buyIn.value > max) {
    buyInError.value = `买入须在 ${min} ~ ${max} 之间`
    return
  }
  store.sitDown(sitTarget.value, buyIn.value)
  sitTarget.value = null
}

function onStandUp() {
  if (window.confirm('确定离座吗？')) {
    store.standUp()
  }
}

async function onDissolve() {
  if (window.confirm('确定解散房间吗？所有玩家将被移出。')) {
    await store.dissolveRoom()
    router.replace('/lobby')
  }
}

function onLeave() {
  store.leaveRoom()
  router.replace('/lobby')
}
</script>

<template>
  <div class="flex min-h-screen flex-col bg-canvas">
    <!-- 顶部栏 -->
    <header class="flex flex-wrap items-center justify-between gap-x-3 gap-y-2 border-b border-line bg-slate-900/70 px-3 py-2.5 sm:px-4">
      <div class="flex min-w-0 items-center gap-2">
        <button class="shrink-0 text-sm text-muted transition hover:text-ink" @click="onLeave">← 退出</button>
        <div class="min-w-0">
          <span class="text-base font-bold text-ink sm:text-lg">{{ snapshot?.name || roomInfo?.name || '牌桌' }}</span>
          <span class="ml-1.5 rounded bg-slate-700 px-1.5 py-0.5 font-mono text-xs text-muted">
            {{ snapshot?.roomNo || roomInfo?.roomNo }}
          </span>
        </div>
      </div>

      <div class="flex flex-wrap items-center gap-2 text-xs text-muted">
        <span v-if="roomInfo" class="hidden sm:inline">
          盲注 {{ roomInfo.smallBlind }}/{{ roomInfo.bigBlind }} ·
          座位 {{ snapshot?.players.filter((p) => !p.folded).length ?? roomInfo.currentPlayers }}/{{ roomInfo.tableSize }}
        </span>
        <span v-if="roomInfo" class="sm:hidden">盲注 {{ roomInfo.smallBlind }}/{{ roomInfo.bigBlind }}</span>
        <button
          v-if="store.snapshot?.isOwner && store.snapshot.status === 'WAITING'"
          class="rounded-lg bg-gold-500 px-3 py-1.5 text-sm font-semibold text-slate-900 transition hover:bg-gold-400"
          @click="store.startGame()"
        >
          开始游戏
        </button>
        <button
          v-if="store.snapshot?.isOwner"
          class="rounded-lg border border-line px-3 py-1.5 text-sm transition hover:border-red-500 hover:text-red-400"
          @click="onDissolve"
        >
          解散
        </button>
        <button
          v-if="store.mySeat != null && store.snapshot?.status === 'WAITING'"
          class="rounded-lg border border-line px-3 py-1.5 text-sm transition hover:text-ink"
          @click="onStandUp"
        >
          离座
        </button>
      </div>
    </header>

    <!-- 等待提示 -->
    <div
      v-if="snapshot?.status === 'WAITING' && !snapshot.isOwner"
      class="bg-felt/40 py-2 text-center text-sm text-emerald-100/80"
    >
      等待房主开始游戏…
    </div>

    <!-- 牌桌 -->
    <main class="flex flex-1 flex-col items-center py-4 sm:justify-center sm:py-6">
      <PokerTable @sit="openSit" />
      <div class="mt-6 w-full max-w-lg px-4">
        <ActionBar v-if="showActionBar" />
      </div>
    </main>

    <!-- 错误提示 -->
    <div
      v-if="store.error"
      class="fixed bottom-4 left-1/2 z-50 -translate-x-1/2 rounded-lg border border-red-500/50 bg-slate-900 px-4 py-2 text-sm text-red-400 shadow-card"
      @click="store.clearError"
    >
      {{ store.error }}
    </div>

    <!-- 坐下弹窗 -->
    <div
      v-if="sitTarget != null"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      @click.self="sitTarget = null"
    >
      <div class="w-full max-w-xs rounded-2xl border border-line bg-slate-900 p-6 shadow-card">
        <h3 class="mb-4 text-center font-bold text-ink">坐到 {{ sitTarget }} 号位</h3>
        <p v-if="roomInfo" class="mb-2 text-center text-xs text-muted">
          买入范围：{{ roomInfo.buyInMin }} ~ {{ roomInfo.buyInMax }}
        </p>
        <input
          v-model.number="buyIn"
          type="number"
          class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-center text-lg font-bold text-ink outline-none focus:border-gold-500"
        />
        <p v-if="buyInError" class="mt-2 text-center text-xs text-red-400">{{ buyInError }}</p>
        <div class="mt-4 flex gap-2">
          <button
            class="flex-1 rounded-lg border border-line py-2 text-sm text-muted transition hover:text-ink"
            @click="sitTarget = null"
          >
            取消
          </button>
          <button
            class="flex-1 rounded-lg bg-gold-500 py-2 text-sm font-semibold text-slate-900 transition hover:bg-gold-400"
            @click="confirmSit"
          >
            确认入座
          </button>
        </div>
      </div>
    </div>

    <HandResultModal />
  </div>
</template>
