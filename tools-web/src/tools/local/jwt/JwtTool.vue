<template>
  <ToolWorkspace
    eyebrow="Developer Tool"
    title="JWT 解析生成"
    description="本地解析、校验并生成 JWT 令牌，Secret 不会上传到服务器。"
  >
    <template #actions>
      <div class="flex gap-2">
        <button
          class="rounded-lg px-4 py-2 text-sm font-medium transition-all duration-300 ease-in-out"
          :class="tab === 'parse' ? 'bg-indigo-50 text-brand-600 dark:bg-indigo-950/40' : 'text-muted hover:text-brand-600'"
          type="button"
          @click="tab = 'parse'"
        >
          JWT 解析
        </button>
        <button
          class="rounded-lg px-4 py-2 text-sm font-medium transition-all duration-300 ease-in-out"
          :class="tab === 'generate' ? 'bg-indigo-50 text-brand-600 dark:bg-indigo-950/40' : 'text-muted hover:text-brand-600'"
          type="button"
          @click="tab = 'generate'"
        >
          JWT 生成
        </button>
      </div>
    </template>

    <template v-if="tab === 'parse'">
      <section class="panel p-5">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-semibold text-slate-900 dark:text-zinc-100">Token</h2>
          <div class="flex gap-2">
            <button class="secondary-button" type="button" @click="clearParse">清除</button>
            <button class="primary-button" type="button" @click="parseToken">解析</button>
          </div>
        </div>
        <textarea
          v-model="tokenInput"
          class="code-field min-h-[220px]"
          spellcheck="false"
          placeholder="粘贴 JWT Token"
        />
        <p v-if="parseError" class="mt-3 text-sm font-medium text-rose-500">{{ parseError }}</p>

        <div class="mt-5">
          <h3 class="mb-3 font-semibold text-slate-900 dark:text-zinc-100">Secret 校验</h3>
          <div class="flex flex-col gap-3 sm:flex-row">
            <input
              v-model="verifySecret"
              class="field flex-1"
              type="password"
              placeholder="请输入用于校验的 Secret"
            />
            <button class="primary-button shrink-0" type="button" :disabled="!parsed" @click="verifyToken">
              校验签名
            </button>
          </div>
          <p v-if="verifyMessage" class="mt-3 text-sm font-medium" :class="verifyOk ? 'text-emerald-600' : 'text-rose-500'">
            {{ verifyMessage }}
          </p>
        </div>
      </section>

      <section class="panel p-5">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-semibold text-slate-900 dark:text-zinc-100">解析结果</h2>
          <CopyButton v-if="headerText" :value="headerText + '\n\n' + payloadText" label="复制全部" />
        </div>

        <div v-if="!parsed" class="rounded-2xl border border-dashed border-line/80 p-8 text-center text-sm text-muted">
          等待解析
        </div>

        <div v-else class="space-y-5">
          <div v-if="timeFields.length" class="grid gap-3 sm:grid-cols-3">
            <div
              v-for="item in timeFields"
              :key="item.key"
              class="rounded-2xl border border-line/70 bg-white/70 p-4 dark:bg-zinc-950/40"
            >
              <p class="text-xs font-medium uppercase tracking-wide text-muted">{{ item.key }}</p>
              <p class="mt-2 text-sm font-semibold text-slate-900 dark:text-zinc-100">{{ item.text }}</p>
              <p v-if="item.key === 'exp'" class="mt-1 text-xs" :class="item.expired ? 'text-rose-500' : 'text-emerald-600'">
                {{ item.expired ? `已过期 ${formatDuration(Math.abs(item.remainingSeconds || 0))}` : `剩余 ${formatDuration(item.remainingSeconds || 0)}` }}
              </p>
            </div>
          </div>

          <div>
            <div class="mb-2 flex items-center justify-between">
              <h3 class="text-sm font-semibold text-slate-900 dark:text-zinc-100">Header(头部)</h3>
              <CopyButton :value="headerText" />
            </div>
            <textarea v-model="headerText" class="code-field min-h-[140px]" spellcheck="false" readonly />
          </div>

          <div>
            <div class="mb-2 flex items-center justify-between">
              <h3 class="text-sm font-semibold text-slate-900 dark:text-zinc-100">Payload(载荷)</h3>
              <CopyButton :value="payloadText" />
            </div>
            <textarea v-model="payloadText" class="code-field min-h-[180px]" spellcheck="false" readonly />
          </div>

          <div>
            <div class="mb-2 flex items-center justify-between">
              <h3 class="text-sm font-semibold text-slate-900 dark:text-zinc-100">Signature</h3>
              <CopyButton :value="signatureText" />
            </div>
            <textarea v-model="signatureText" class="code-field min-h-[100px]" spellcheck="false" readonly />
          </div>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="panel p-5">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-semibold text-slate-900 dark:text-zinc-100">生成配置</h2>
          <button class="secondary-button" type="button" @click="resetGenerate">重置</button>
        </div>

        <div class="space-y-4">
          <label class="block">
            <span class="mb-2 block text-sm font-medium text-muted">Header</span>
            <textarea v-model="generateHeader" class="code-field min-h-[120px]" spellcheck="false" />
          </label>

          <label class="block">
            <span class="mb-2 block text-sm font-medium text-muted">Payload</span>
            <textarea v-model="generatePayload" class="code-field min-h-[160px]" spellcheck="false" />
          </label>

          <div class="grid gap-4 sm:grid-cols-2">
            <label class="block">
              <span class="mb-2 block text-sm font-medium text-muted">算法</span>
              <select v-model="generateAlgorithm" class="field">
                <option value="HS256">HS256</option>
                <option value="HS384">HS384</option>
                <option value="HS512">HS512</option>
              </select>
            </label>
            <label class="block">
              <span class="mb-2 block text-sm font-medium text-muted">过期时间</span>
              <select v-model.number="expireHours" class="field" @change="applyExpireHours">
                <option :value="1">1 小时</option>
                <option :value="24">1 天</option>
                <option :value="168">7 天</option>
                <option :value="360">15 天</option>
              </select>
            </label>
          </div>

          <label class="block">
            <span class="mb-2 block text-sm font-medium text-muted">Secret</span>
            <div class="flex flex-col gap-3 sm:flex-row">
              <input v-model="generateSecret" class="field flex-1 font-mono" :type="showSecret ? 'text' : 'password'" />
              <button class="secondary-button shrink-0" type="button" @click="showSecret = !showSecret">
                {{ showSecret ? '隐藏' : '显示' }}
              </button>
              <button class="secondary-button shrink-0" type="button" @click="generateSecret = randomSecret()">
                随机密钥
              </button>
            </div>
          </label>
        </div>

        <p v-if="generateError" class="mt-3 text-sm font-medium text-rose-500">{{ generateError }}</p>
        <button class="primary-button mt-5 w-full" type="button" @click="generateToken">生成 JWT</button>
      </section>

      <section class="panel p-5">
        <div class="mb-4 flex items-center justify-between">
          <h2 class="font-semibold text-slate-900 dark:text-zinc-100">生成结果</h2>
          <div class="flex gap-2">
            <CopyButton :value="generatedToken" label="复制 Token" />
            <CopyButton :value="generateSecret" label="复制 Secret" />
          </div>
        </div>

        <label class="block">
          <span class="mb-2 block text-sm font-medium text-muted">JWT Token</span>
          <textarea v-model="generatedToken" class="code-field min-h-[180px]" spellcheck="false" readonly />
        </label>

        <div class="mt-5 grid gap-4">
          <label class="block">
            <span class="mb-2 block text-sm font-medium text-muted">Header 预览</span>
            <textarea v-model="previewHeader" class="code-field min-h-[120px]" spellcheck="false" readonly />
          </label>
          <label class="block">
            <span class="mb-2 block text-sm font-medium text-muted">Payload 预览</span>
            <textarea v-model="previewPayload" class="code-field min-h-[140px]" spellcheck="false" readonly />
          </label>
        </div>
      </section>
    </template>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'
import {
  createDefaultHeader,
  createDefaultPayload,
  extractTimeFields,
  formatDuration,
  formatJson,
  parseJwt,
  randomSecret,
  signHsJwt,
  verifyHsSignature,
  type HsAlgorithm,
  type JwtParts,
  type TimeFieldInfo
} from './jwt'

const tab = ref<'parse' | 'generate'>('parse')
const tokenInput = ref('')
const parseError = ref('')
const parsed = ref<JwtParts | null>(null)
const headerText = ref('')
const payloadText = ref('')
const signatureText = ref('')
const timeFields = ref<TimeFieldInfo[]>([])
const verifySecret = ref('')
const verifyMessage = ref('')
const verifyOk = ref(false)

const generateAlgorithm = ref<HsAlgorithm>('HS256')
const generateHeader = ref(formatJson(createDefaultHeader('HS256')))
const generatePayload = ref(formatJson(createDefaultPayload(1)))
const generateSecret = ref(randomSecret())
const showSecret = ref(false)
const expireHours = ref(1)
const generateError = ref('')
const generatedToken = ref('')
const previewHeader = ref('')
const previewPayload = ref('')

function clearParse() {
  tokenInput.value = ''
  parseError.value = ''
  parsed.value = null
  headerText.value = ''
  payloadText.value = ''
  signatureText.value = ''
  timeFields.value = []
  verifyMessage.value = ''
  verifyOk.value = false
}

function parseToken() {
  try {
    parseError.value = ''
    verifyMessage.value = ''
    const result = parseJwt(tokenInput.value)
    parsed.value = result
    headerText.value = formatJson(result.header)
    payloadText.value = formatJson(result.payload)
    signatureText.value = result.signature
    timeFields.value = extractTimeFields(result.payload)
  } catch (error) {
    parsed.value = null
    headerText.value = ''
    payloadText.value = ''
    signatureText.value = ''
    timeFields.value = []
    parseError.value = error instanceof Error ? error.message : '解析失败'
  }
}

async function verifyToken() {
  try {
    verifyMessage.value = ''
    const result = await verifyHsSignature(tokenInput.value, verifySecret.value)
    verifyOk.value = result.valid
    verifyMessage.value = `${result.message}（${result.algorithm}）`
  } catch (error) {
    verifyOk.value = false
    verifyMessage.value = error instanceof Error ? error.message : '校验失败'
  }
}

function applyExpireHours() {
  try {
    const payload = JSON.parse(generatePayload.value) as Record<string, unknown>
    const now = Math.floor(Date.now() / 1000)
    payload.iat = now
    payload.exp = now + expireHours.value * 3600
    generatePayload.value = formatJson(payload)
  } catch {
    generatePayload.value = formatJson(createDefaultPayload(expireHours.value))
  }
}

function resetGenerate() {
  generateAlgorithm.value = 'HS256'
  generateHeader.value = formatJson(createDefaultHeader('HS256'))
  generatePayload.value = formatJson(createDefaultPayload(expireHours.value))
  generateSecret.value = randomSecret()
  generateError.value = ''
  generatedToken.value = ''
  previewHeader.value = ''
  previewPayload.value = ''
}

async function generateToken() {
  try {
    generateError.value = ''
    const header = JSON.parse(generateHeader.value) as Record<string, unknown>
    const payload = JSON.parse(generatePayload.value) as Record<string, unknown>
    const token = await signHsJwt(header, payload, generateSecret.value, generateAlgorithm.value)
    generatedToken.value = token
    const parsedGenerated = parseJwt(token)
    previewHeader.value = formatJson(parsedGenerated.header)
    previewPayload.value = formatJson(parsedGenerated.payload)
  } catch (error) {
    generateError.value = error instanceof Error ? error.message : '生成失败'
    generatedToken.value = ''
    previewHeader.value = ''
    previewPayload.value = ''
  }
}

watch(generateAlgorithm, (algorithm) => {
  try {
    const header = JSON.parse(generateHeader.value) as Record<string, unknown>
    header.alg = algorithm
    generateHeader.value = formatJson(header)
  } catch {
    generateHeader.value = formatJson(createDefaultHeader(algorithm))
  }
})

onMounted(async () => {
  try {
    const demoSecret = 'tools-web-demo-secret'
    const token = await signHsJwt(createDefaultHeader('HS256'), createDefaultPayload(1), demoSecret, 'HS256')
    tokenInput.value = token
    verifySecret.value = demoSecret
    parseToken()
  } catch {
    // ignore bootstrap errors
  }
})
</script>
