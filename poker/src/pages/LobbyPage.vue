<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createRoom, joinRoom, listRooms } from '@/shared/api/poker'
import type { PokerRoomDTO } from '@/shared/api/poker-types'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const rooms = ref<PokerRoomDTO[]>([])
const loading = ref(false)
const errorMsg = ref('')

const showCreate = ref(false)
const creating = ref(false)
const joinNo = ref('')

const form = ref({
  name: '',
  tableSize: 6,
  smallBlind: 1,
  bigBlind: 2,
  buyInMin: 100,
  buyInMax: 500
})

const STATUS_NAMES: Record<string, string> = {
  WAITING: '等待中',
  PLAYING: '对局中'
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    rooms.value = await listRooms()
  } catch (err) {
    errorMsg.value = (err as Error).message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (route.query.msg === 'room-closed') {
    errorMsg.value = '房间已被房主解散'
  }
  load()
})

async function doCreate() {
  creating.value = true
  errorMsg.value = ''
  try {
    const room = await createRoom(form.value)
    showCreate.value = false
    router.push(`/room/${room.id}`)
  } catch (err) {
    errorMsg.value = (err as Error).message || '创建失败'
  } finally {
    creating.value = false
  }
}

async function doJoin() {
  errorMsg.value = ''
  if (!joinNo.value.trim()) {
    errorMsg.value = '请输入房号'
    return
  }
  try {
    const room = await joinRoom(joinNo.value.trim())
    router.push(`/room/${room.id}`)
  } catch (err) {
    errorMsg.value = (err as Error).message || '加入失败'
  }
}

function openRoom(room: PokerRoomDTO) {
  router.push(`/room/${room.id}`)
}
</script>

<template>
  <div class="min-h-screen bg-canvas">
    <header class="flex flex-wrap items-center justify-between gap-y-2 border-b border-line bg-slate-900/70 px-4 py-3 sm:px-6">
      <div>
        <h1 class="text-xl font-bold text-gold-500">Poker 德州扑克</h1>
        <p class="text-xs text-muted">娱乐 · 虚拟筹码</p>
      </div>
      <div class="flex items-center gap-3">
        <button
          class="rounded-lg bg-gold-500 px-4 py-2 text-sm font-semibold text-slate-900 transition hover:bg-gold-400"
          @click="showCreate = true"
        >
          + 创建房间
        </button>
        <button
          class="rounded-lg border border-line px-3 py-2 text-sm text-muted transition hover:text-ink"
          @click="router.push('/stats')"
        >
          战绩
        </button>
        <button
          class="text-sm text-muted transition hover:text-ink"
          @click="userStore.logout(); router.push('/login')"
        >
          退出登录
        </button>
      </div>
    </header>

    <main class="mx-auto max-w-4xl px-6 py-6">
      <!-- 加入 -->
      <div class="mb-6 flex items-center gap-2 rounded-2xl border border-line bg-slate-900/60 p-4">
        <input
          v-model="joinNo"
          class="flex-1 rounded-lg border border-line bg-slate-800 px-3 py-2 font-mono text-lg tracking-widest text-ink outline-none focus:border-gold-500"
          maxlength="6"
          placeholder="输入 6 位房号加入"
        />
        <button
          class="rounded-lg bg-slate-700 px-6 py-2.5 text-sm font-semibold transition hover:bg-slate-600"
          @click="doJoin"
        >
          加入
        </button>
      </div>

      <p v-if="errorMsg" class="mb-4 text-sm text-red-400">{{ errorMsg }}</p>

      <!-- 房间列表 -->
      <div class="space-y-3">
        <p v-if="loading" class="py-8 text-center text-sm text-muted">加载中…</p>
        <p v-else-if="rooms.length === 0" class="py-8 text-center text-sm text-muted">
          暂无房间，创建一个开始游戏吧
        </p>

        <div
          v-for="room in rooms"
          :key="room.id"
          class="flex cursor-pointer items-center justify-between rounded-2xl border border-line bg-slate-900/60 p-4 transition hover:border-gold-500/50"
          @click="openRoom(room)"
        >
          <div>
            <div class="flex items-center gap-2">
              <span class="font-bold text-ink">{{ room.name }}</span>
              <span class="font-mono text-xs text-muted">房号 {{ room.roomNo }}</span>
              <span
                class="rounded px-1.5 py-0.5 text-xs"
                :class="room.status === 'PLAYING' ? 'bg-emerald-600/20 text-emerald-300' : 'bg-slate-700 text-muted'"
              >
                {{ STATUS_NAMES[room.status] }}
              </span>
            </div>
            <div class="mt-1 text-xs text-muted">
              盲注 {{ room.smallBlind }}/{{ room.bigBlind }} · 买入
              {{ room.buyInMin }}~{{ room.buyInMax }} · 第 {{ room.handNo }} 手
            </div>
          </div>
          <div class="text-sm text-muted">
            <span class="font-bold text-ink">{{ room.currentPlayers }}</span> / {{ room.tableSize }}
          </div>
        </div>
      </div>
    </main>

    <!-- 创建弹窗 -->
    <div
      v-if="showCreate"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
      @click.self="showCreate = false"
    >
      <div class="w-full max-w-md animate-fade-up rounded-2xl border border-line bg-slate-900 p-6 shadow-soft">
        <h3 class="mb-5 text-center text-lg font-bold text-ink">创建房间</h3>

        <div class="space-y-4">
          <div>
            <label class="mb-1 block text-xs text-muted">房间名</label>
            <input
              v-model="form.name"
              class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-ink outline-none focus:border-gold-500"
              placeholder="例如：欢乐局"
            />
          </div>

          <div>
            <label class="mb-1 block text-xs text-muted">座位数</label>
            <div class="flex gap-2">
              <button
                v-for="n in [2, 3, 4, 5, 6, 9]"
                :key="n"
                class="flex-1 rounded-lg py-1.5 text-sm transition"
                :class="form.tableSize === n ? 'bg-gold-500 font-semibold text-slate-900' : 'bg-slate-800 text-muted hover:text-ink'"
                @click="form.tableSize = n"
              >
                {{ n }}
              </button>
            </div>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="mb-1 block text-xs text-muted">小盲</label>
              <input
                v-model.number="form.smallBlind"
                type="number"
                min="0.5"
                class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-ink outline-none focus:border-gold-500"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs text-muted">大盲</label>
              <input
                v-model.number="form.bigBlind"
                type="number"
                min="1"
                class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-ink outline-none focus:border-gold-500"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs text-muted">最小买入</label>
              <input
                v-model.number="form.buyInMin"
                type="number"
                min="1"
                class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-ink outline-none focus:border-gold-500"
              />
            </div>
            <div>
              <label class="mb-1 block text-xs text-muted">最大买入</label>
              <input
                v-model.number="form.buyInMax"
                type="number"
                min="1"
                class="w-full rounded-lg border border-line bg-slate-800 px-3 py-2 text-ink outline-none focus:border-gold-500"
              />
            </div>
          </div>
        </div>

        <div class="mt-6 flex gap-2">
          <button
            class="flex-1 rounded-lg border border-line py-2 text-sm text-muted transition hover:text-ink"
            @click="showCreate = false"
          >
            取消
          </button>
          <button
            class="flex-1 rounded-lg bg-gold-500 py-2 text-sm font-semibold text-slate-900 transition hover:bg-gold-400 disabled:opacity-50"
            :disabled="creating || !form.name.trim()"
            @click="doCreate"
          >
            {{ creating ? '创建中…' : '创建' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
