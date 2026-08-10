<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { useTableStore } from '@/stores/table'
import PlayerSeat from './PlayerSeat.vue'
import CommunityCards from './CommunityCards.vue'

const store = useTableStore()
const emit = defineEmits<{ sit: [seat: number] }>()

const snapshot = computed(() => store.snapshot)
const seats = computed(() => store.playersBySeat)
const tableSize = computed(() => snapshot.value?.tableSize ?? 6)
const phase = computed(() => snapshot.value?.phase ?? 'WAITING')

const wrapRef = ref<HTMLElement | null>(null)
const scale = ref(1)

// 手机端（<768px）使用竖向椭圆布局，便于单手操作与更大元素
const mq = typeof window !== 'undefined' ? window.matchMedia('(max-width: 767px)') : null
const isMobile = ref(mq?.matches ?? false)

const layout = computed(() =>
  isMobile.value
    ? { w: 480, h: 820, rx: 185, ry: 290, feltW: 280, feltH: 500 }
    : { w: 900, h: 560, rx: 315, ry: 175, feltW: 580, feltH: 340 }
)

function updateScale() {
  const el = wrapRef.value
  if (el) {
    scale.value = Math.min(1, el.clientWidth / layout.value.w)
  }
}

function onModeChange() {
  isMobile.value = mq?.matches ?? false
  requestAnimationFrame(updateScale)
}

onMounted(() => {
  const el = wrapRef.value
  if (!el) return
  updateScale()
  const observer = new ResizeObserver(updateScale)
  observer.observe(el)
  mq?.addEventListener('change', onModeChange)
  onBeforeUnmount(() => {
    observer.disconnect()
    mq?.removeEventListener('change', onModeChange)
  })
})

const PHASE_NAMES: Record<string, string> = {
  WAITING: '等待开局',
  PREFLOP: '翻牌前',
  FLOP: '翻牌',
  TURN: '转牌',
  RIVER: '河牌',
  FINISHED: '本手结束'
}

function pos(seat: number, total: number) {
  const angle = (seat / total) * Math.PI * 2 - Math.PI / 2
  const x = layout.value.w / 2 + layout.value.rx * Math.cos(angle)
  const y = layout.value.h / 2 + layout.value.ry * Math.sin(angle)
  return { left: `${x}px`, top: `${y}px` }
}

function canSit(seat: number) {
  const s = snapshot.value
  return !!s && s.status === 'WAITING' && s.mySeat == null
}

function showingCards(seat: number, isMe: boolean) {
  if (isMe) return true
  const last = snapshot.value?.lastHand
  return !!last && !!last.revealedHoleCards[seat]
}
</script>

<template>
  <div
    ref="wrapRef"
    class="relative mx-auto w-full overflow-hidden"
    :style="{ maxWidth: `${layout.w}px`, height: `${layout.h * scale}px` }"
  >
    <div
      class="absolute left-0 top-0"
      :style="{
        width: `${layout.w}px`,
        height: `${layout.h}px`,
        transform: `scale(${scale})`,
        transformOrigin: 'top left'
      }"
    >
      <!-- 桌面 -->
      <div
        class="absolute left-1/2 top-1/2 flex -translate-x-1/2 -translate-y-1/2 flex-col items-center justify-center rounded-[45%] border-8 border-[#5b3a1e] bg-felt shadow-card"
        :style="{ width: `${layout.feltW}px`, height: `${layout.feltH}px` }"
      >
        <div class="mb-2 rounded-full bg-black/30 px-3 py-0.5 text-xs text-emerald-100/80">
          {{ PHASE_NAMES[phase] }} · 第 {{ snapshot?.handNo ?? 0 }} 手
        </div>
        <CommunityCards :board="snapshot?.board ?? []" :pot="snapshot?.pot ?? '0'" />
        <div v-if="snapshot?.dealerSeat != null" class="mt-2 text-xs text-emerald-100/60">
          庄家：{{ snapshot.dealerSeat }} 号位
        </div>
      </div>

      <!-- 座位环 -->
      <template v-for="(player, seat) in seats" :key="seat">
        <div class="absolute" :style="pos(seat, tableSize)">
          <PlayerSeat
            :player="player"
            :is-me="seat === snapshot?.mySeat"
            :sitting="snapshot?.status === 'PLAYING'"
            :can-sit="!player && canSit(seat)"
            :showing-cards="showingCards(seat, seat === snapshot?.mySeat)"
            @sit="emit('sit', seat)"
          />
        </div>
      </template>
    </div>
  </div>
</template>
