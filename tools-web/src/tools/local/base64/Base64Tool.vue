<template>
  <ToolWorkspace eyebrow="开发工具" title="Base64 编解码" description="支持中文与常见字符的 Base64 编码和解码。">
    <template #actions>
      <CopyButton :value="output" />
    </template>

    <section class="panel p-5">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输入</h2>
        <button class="secondary-button" type="button" @click="clear">清空</button>
      </div>
      <textarea v-model="input" class="code-field" spellcheck="false" placeholder="输入文本或 Base64 字符串" />
      <p v-if="error" class="mt-3 text-sm font-medium text-rose-500">{{ error }}</p>
    </section>

    <section class="panel p-5">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 class="font-semibold tracking-tight text-slate-900 dark:text-zinc-100">输出</h2>
        <div class="flex gap-2">
          <button class="secondary-button" type="button" @click="decode">解码</button>
          <button class="primary-button" type="button" @click="encode">编码</button>
        </div>
      </div>
      <textarea v-model="output" class="code-field" spellcheck="false" readonly />
    </section>
  </ToolWorkspace>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import CopyButton from '@/shared/components/CopyButton.vue'
import ToolWorkspace from '@/shared/components/ToolWorkspace.vue'

const input = ref('GameGroup 工具箱')
const output = ref('')
const error = ref('')

function bytesToBase64(bytes: Uint8Array) {
  let binary = ''
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte)
  })
  return window.btoa(binary)
}

function base64ToBytes(value: string) {
  const binary = window.atob(value)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index)
  }
  return bytes
}

function encode() {
  error.value = ''
  output.value = bytesToBase64(new TextEncoder().encode(input.value))
}

function decode() {
  try {
    error.value = ''
    output.value = new TextDecoder().decode(base64ToBytes(input.value.trim()))
  } catch {
    error.value = 'Base64 字符串无效'
  }
}

function clear() {
  input.value = ''
  output.value = ''
  error.value = ''
}

encode()
</script>
