<template>
  <ToolWorkspace eyebrow="开发工具" title="长连接调试" description="选择加密或非加密协议，测试连接并查看实时消息。">
    <template #actions>
      <div class="flex items-center gap-2">
        <span class="rounded-full px-3 py-1.5 text-xs font-semibold" :class="statusClass">
          {{ statusLabel }}
        </span>
        <button class="secondary-button" type="button" @click="clearMessages">
          <Trash2 class="h-4 w-4" />
          清空记录
        </button>
      </div>
    </template>

    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">连接配置</h2>
        <Wifi class="h-5 w-5 text-brand-600" />
      </div>

      <div class="grid gap-4 sm:grid-cols-[120px_1fr]">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">协议</span>
          <select v-model="protocol" class="field">
            <option value="wss">加密（wss）</option>
            <option value="ws">普通（ws）</option>
          </select>
        </label>
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">地址</span>
          <input v-model="endpoint" class="field font-mono" placeholder="example.com/socket" @keyup.enter="connect" />
        </label>
      </div>

      <p class="mt-3 break-all text-xs text-muted">完整地址：{{ connectionUrl }}</p>
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>

      <div class="mt-5 grid gap-3 sm:grid-cols-2">
        <button class="primary-button" type="button" :disabled="isConnecting || isConnected" @click="connect">
          <PlugZap class="h-4 w-4" />
          {{ isConnecting ? '连接中…' : '建立连接' }}
        </button>
        <button class="secondary-button" type="button" :disabled="!socket" @click="disconnect">
          <Power class="h-4 w-4" />
          断开连接
        </button>
      </div>
    </section>

    <section class="panel p-5">
      <div class="mb-5 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">消息收发</h2>
        <span class="text-sm text-muted">{{ messages.length }} 条</span>
      </div>

      <textarea v-model="messageInput" class="code-field min-h-[150px]" spellcheck="false" placeholder="输入要发送的消息" />
      <button class="primary-button mt-4 w-full" type="button" :disabled="!isConnected || !messageInput.trim()" @click="sendMessage">
        <Send class="h-4 w-4" />
        发送消息
      </button>

      <div class="mt-5 max-h-[360px] space-y-3 overflow-y-auto pr-1">
        <div v-if="!messages.length" class="empty-state py-8">
          等待连接或消息
        </div>
        <article
          v-for="message in messages"
          :key="message.id"
          class="rounded-2xl border p-4 transition-colors duration-300"
          :class="messageClass(message.kind)"
        >
          <div class="mb-2 flex items-center justify-between gap-3 text-xs font-medium">
            <span>{{ message.kind }}</span>
            <time class="text-muted">{{ message.time }}</time>
          </div>
          <pre class="whitespace-pre-wrap break-words font-mono text-xs leading-6">{{ message.content }}</pre>
        </article>
      </div>
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { PlugZap, Power, Send, Trash2, Wifi } from '@lucide/vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

type ConnectionState = 'idle' | 'connecting' | 'connected' | 'closed' | 'error'
type MessageKind = '连接' | '发送' | '接收' | '关闭' | '错误'

interface DebugMessage {
  id: number
  kind: MessageKind
  content: string
  time: string
}

const protocol = ref<'ws' | 'wss'>('wss')
const endpoint = ref('echo.websocket.events')
const state = ref<ConnectionState>('idle')
const error = ref('')
const socket = ref<WebSocket | null>(null)
const messageInput = ref('')
const messages = ref<DebugMessage[]>([])
let messageId = 0

const connectionUrl = computed(() => {
  const address = endpoint.value.trim().replace(/^wss?:\/\//i, '').replace(/^\/+/, '')
  return `${protocol.value}://${address}`
})

const isConnecting = computed(() => state.value === 'connecting')
const isConnected = computed(() => state.value === 'connected')
const statusLabel = computed(() => {
  const labels: Record<ConnectionState, string> = {
    idle: '未连接',
    connecting: '连接中',
    connected: '已连接',
    closed: '已断开',
    error: '连接失败'
  }
  return labels[state.value]
})
const statusClass = computed(() => {
  const classes: Record<ConnectionState, string> = {
    idle: 'bg-slate-100 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300',
    connecting: 'bg-amber-500/10 text-amber-700 dark:text-amber-400',
    connected: 'bg-emerald-500/10 text-emerald-700 dark:text-emerald-400',
    closed: 'bg-slate-100 text-slate-600 dark:bg-zinc-800 dark:text-zinc-300',
    error: 'bg-rose-500/10 text-rose-700 dark:text-rose-400'
  }
  return classes[state.value]
})

function addMessage(kind: MessageKind, content: string) {
  messages.value.unshift({
    id: ++messageId,
    kind,
    content,
    time: new Date().toLocaleTimeString('zh-CN', { hour12: false })
  })
}

function messageClass(kind: MessageKind) {
  const classes: Record<MessageKind, string> = {
    连接: 'border-emerald-500/20 bg-emerald-500/5 text-emerald-900 dark:text-emerald-100',
    发送: 'border-indigo-500/20 bg-indigo-500/5 text-indigo-900 dark:text-indigo-100',
    接收: 'border-sky-500/20 bg-sky-500/5 text-sky-900 dark:text-sky-100',
    关闭: 'border-slate-300/60 bg-slate-100/60 text-slate-700 dark:bg-zinc-800/60 dark:text-zinc-200',
    错误: 'border-rose-500/20 bg-rose-500/5 text-rose-900 dark:text-rose-100'
  }
  return classes[kind]
}

function connect() {
  if (isConnecting.value || isConnected.value) {
    return
  }
  if (!endpoint.value.trim()) {
    error.value = '请输入连接地址'
    return
  }

  error.value = ''
  state.value = 'connecting'
  try {
    const client = new WebSocket(connectionUrl.value)
    socket.value = client

    client.onopen = () => {
      state.value = 'connected'
      addMessage('连接', `已连接：${connectionUrl.value}`)
    }
    client.onmessage = async (event) => {
      addMessage('接收', await readMessage(event.data))
    }
    client.onerror = () => {
      error.value = '连接发生错误，请检查地址、协议和浏览器安全策略'
      state.value = 'error'
      addMessage('错误', error.value)
    }
    client.onclose = (event) => {
      if (socket.value === client) {
        socket.value = null
      }
      if (state.value !== 'error') {
        state.value = 'closed'
      }
      addMessage('关闭', `连接已关闭（状态码：${event.code}${event.reason ? `，原因：${event.reason}` : ''}）`)
    }
  } catch (connectionError) {
    const message = connectionError instanceof Error ? connectionError.message : '无法创建长连接'
    error.value = message
    state.value = 'error'
    addMessage('错误', message)
  }
}

function disconnect() {
  socket.value?.close(1000, '客户端主动断开')
}

function sendMessage() {
  const content = messageInput.value.trim()
  if (!socket.value || socket.value.readyState !== WebSocket.OPEN || !content) {
    return
  }
  socket.value.send(content)
  addMessage('发送', content)
  messageInput.value = ''
}

async function readMessage(data: unknown) {
  if (typeof data === 'string') {
    return data
  }
  if (data instanceof Blob) {
    return data.text()
  }
  if (data instanceof ArrayBuffer) {
    return new TextDecoder().decode(data)
  }
  return String(data)
}

function clearMessages() {
  messages.value = []
}

onBeforeUnmount(() => {
  socket.value?.close(1000, '页面离开')
})
</script>
